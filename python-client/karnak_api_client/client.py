"""Authenticated transport client for the Karnak REST API.

The client intentionally mirrors the REST resources from API.md and keeps
policy decisions out of the transport layer. Higher-level code decides what
"apply" means (see ``karnak_api_client.apply``); this module only performs
authenticated HTTP calls and returns JSON-compatible dictionaries/lists.

Authentication
--------------
Depending on how the Karnak image is configured, the REST API is reachable
either with HTTP Basic (default in-memory IdP) or only through Spring
form-login (POST ``/login`` once, then carry the ``JSESSIONID`` cookie).
``auth_mode`` selects the behaviour:

- ``"basic"`` — HTTP Basic on every request, no fallback.
- ``"form"``  — form-login before the first request, cookie afterwards.
- ``"auto"``  (default) — try Basic; when the server answers 401 or redirects
  to ``/login``, switch to form-login transparently and retry once.
"""

from __future__ import annotations

import os
from dataclasses import dataclass, replace
from pathlib import Path
from typing import Any

import requests
from requests.auth import HTTPBasicAuth

_AUTH_MODES = ("auto", "basic", "form")


class KarnakError(RuntimeError):
    """Base class for all Karnak client errors (incl. network failures)."""


class AuthenticationError(KarnakError):
    """Raised when login fails or the session is rejected."""


class KarnakApiError(KarnakError):
    """Raised when Karnak returns a non-success HTTP response."""

    def __init__(self, method: str, url: str, status_code: int, body: str) -> None:
        self.method = method
        self.url = url
        self.status_code = status_code
        self.body = body
        super().__init__(f"Karnak {method} {url} failed with HTTP {status_code}: {body}")


# Backwards-compatible alias kept for callers written against the original
# form-login client (TMLCTP harness, setup_deid_gateway.py).
ApiError = KarnakApiError


@dataclass(frozen=True)
class KarnakClientConfig:
    base_url: str
    username: str
    password: str
    timeout_sec: float = 30.0
    auth_mode: str = "auto"

    def __post_init__(self) -> None:
        if self.auth_mode not in _AUTH_MODES:
            raise ValueError(f"auth_mode must be one of {_AUTH_MODES}, got {self.auth_mode!r}")

    @classmethod
    def from_env(cls) -> "KarnakClientConfig":
        return cls(
            base_url=os.getenv("KARNAK_API_BASE", "http://localhost:8081").rstrip("/"),
            username=os.getenv("KARNAK_USERNAME", os.getenv("KARNAK_LOGIN_ADMIN", "admin")),
            password=os.getenv("KARNAK_PASSWORD", "karnak"),
            timeout_sec=float(os.getenv("KARNAK_API_TIMEOUT_SEC", "30")),
            auth_mode=os.getenv("KARNAK_AUTH_MODE", "auto").strip().lower(),
        )


def _redirects_to_login(resp: requests.Response) -> bool:
    if resp.status_code in (302, 303):
        return "login" in resp.headers.get("Location", "")
    return False


class KarnakClient:
    """Thin wrapper around the Karnak REST API.

    Construction styles (equivalent)::

        KarnakClient()                                  # from environment
        KarnakClient(KarnakClientConfig(...))           # explicit config
        KarnakClient(base_url=..., username=..., password=...)  # kwargs
    """

    def __init__(
        self,
        config: KarnakClientConfig | None = None,
        *,
        base_url: str | None = None,
        username: str | None = None,
        password: str | None = None,
        timeout_sec: float | None = None,
        auth_mode: str | None = None,
        session: requests.Session | None = None,
    ) -> None:
        cfg = config or KarnakClientConfig.from_env()
        overrides = {
            key: value
            for key, value in {
                "base_url": base_url.rstrip("/") if base_url else None,
                "username": username,
                "password": password,
                "timeout_sec": timeout_sec,
                "auth_mode": auth_mode,
            }.items()
            if value is not None
        }
        if overrides:
            cfg = replace(cfg, **overrides)
        self.config = cfg
        self.session = session if session is not None else requests.Session()
        self.session.headers.update({"Accept": "application/json"})
        self._basic = HTTPBasicAuth(cfg.username, cfg.password)
        self._use_form = cfg.auth_mode == "form"
        self._form_logged_in = False

    # ── authentication ──────────────────────────────────────────────────

    @property
    def base(self) -> str:
        """Base URL (kept as an attribute-style accessor for older callers)."""
        return self.config.base_url

    def login(self) -> None:
        """POST form credentials to /login and establish a session cookie."""
        try:
            resp = self.session.post(
                f"{self.config.base_url}/login",
                data={"username": self.config.username, "password": self.config.password},
                allow_redirects=False,
                timeout=self.config.timeout_sec,
            )
        except requests.RequestException as exc:
            raise KarnakError(f"Cannot reach {self.config.base_url}: {exc}") from exc
        if resp.status_code not in (302, 303):
            raise AuthenticationError(f"Unexpected login response: HTTP {resp.status_code}")
        location = resp.headers.get("Location", "")
        if "error" in location or location.rstrip("/").endswith("/login"):
            raise AuthenticationError(
                f"Login failed — invalid credentials for {self.config.username!r}"
            )
        self._form_logged_in = True

    def invalidate_session(self) -> None:
        """Drop the session cookie (e.g. after a Karnak container restart)."""
        self.session.cookies.clear()
        self._form_logged_in = False

    def check_connectivity(self, timeout: float = 5.0) -> None:
        """Verify Karnak is reachable and credentials are accepted."""
        self._request("GET", "/api/forward-nodes", expected={200, 204}, timeout_sec=timeout)

    # ── transport ───────────────────────────────────────────────────────

    def _url(self, path: str) -> str:
        if not path.startswith("/"):
            path = "/" + path
        return f"{self.config.base_url}{path}"

    def _request(
        self,
        method: str,
        path: str,
        *,
        json_body: Any | None = None,
        files: dict[str, Any] | None = None,
        params: dict[str, Any] | None = None,
        expected: set[int] | None = None,
        timeout_sec: float | None = None,
        _form_retry: bool = False,
    ) -> Any:
        expected = expected or {200}
        url = self._url(path)
        if self._use_form and not self._form_logged_in:
            self.login()
        try:
            resp = self.session.request(
                method,
                url,
                json=json_body,
                files=files,
                params=params,
                auth=None if self._use_form else self._basic,
                allow_redirects=False,
                timeout=timeout_sec or self.config.timeout_sec,
            )
        except requests.RequestException as exc:
            raise KarnakError(f"Karnak {method} {url} failed: {exc}") from exc

        unauthenticated = resp.status_code == 401 or _redirects_to_login(resp)
        if unauthenticated and not self._use_form and not _form_retry:
            if self.config.auth_mode == "basic":
                raise KarnakApiError(method, url, resp.status_code, resp.text)
            # auto mode: this Karnak build does not accept Basic on the API —
            # switch to form-login for the rest of the client's lifetime.
            self._use_form = True
            self.login()
            return self._request(
                method,
                path,
                json_body=json_body,
                files=files,
                params=params,
                expected=expected,
                timeout_sec=timeout_sec,
                _form_retry=True,
            )
        if unauthenticated and self._use_form and not _form_retry:
            # Session cookie expired (e.g. Karnak restarted): log in again once.
            self.invalidate_session()
            self.login()
            return self._request(
                method,
                path,
                json_body=json_body,
                files=files,
                params=params,
                expected=expected,
                timeout_sec=timeout_sec,
                _form_retry=True,
            )

        if resp.status_code not in expected:
            raise KarnakApiError(method, url, resp.status_code, resp.text)
        if resp.status_code == 204 or not resp.content:
            return None
        content_type = resp.headers.get("Content-Type", "")
        if "json" not in content_type.lower():
            return resp.text
        return resp.json()

    # ── health / echo ───────────────────────────────────────────────────

    def echo_destinations(self, src_aet: str) -> Any:
        # This endpoint is intentionally unauthenticated server-side, but using
        # session auth is harmless and keeps the client simple.
        return self._request(
            "GET",
            "/api/echo/destinations",
            params={"srcAet": src_aet},
            expected={200, 204},
        )

    # ── profiles ────────────────────────────────────────────────────────

    def list_profiles(self) -> list[dict[str, Any]]:
        return self._request("GET", "/api/profiles", expected={200, 204}) or []

    def upload_profile(self, path: Path | str) -> dict[str, Any]:
        path = Path(path)
        # Read up-front so the auto-mode form-login retry can resend the body.
        payload = path.read_bytes()
        return self._request(
            "POST",
            "/api/profiles",
            files={"file": (path.name, payload, "application/x-yaml")},
            expected={201},
            timeout_sec=max(self.config.timeout_sec, 60.0),
        )

    def get_profile(self, profile_id: int) -> dict[str, Any]:
        return self._request("GET", f"/api/profiles/{profile_id}", expected={200})

    def update_profile(self, profile_id: int, body: dict[str, Any]) -> dict[str, Any]:
        return self._request("PUT", f"/api/profiles/{profile_id}", json_body=body, expected={200})

    def delete_profile(self, profile_id: int) -> None:
        self._request("DELETE", f"/api/profiles/{profile_id}", expected={204})

    # ── projects / secrets ──────────────────────────────────────────────

    def list_projects(self) -> list[dict[str, Any]]:
        return self._request("GET", "/api/projects", expected={200, 204}) or []

    def create_project(self, name: str, profile_id: int | None = None) -> dict[str, Any]:
        body: dict[str, Any] = {"name": name}
        if profile_id is not None:
            body["profileId"] = profile_id
        return self._request("POST", "/api/projects", json_body=body, expected={201})

    def get_project(self, project_id: int) -> dict[str, Any]:
        return self._request("GET", f"/api/projects/{project_id}", expected={200})

    def update_project(
        self,
        project_id: int,
        *,
        name: str | None = None,
        profile_id: int | None = None,
        detach_profile: bool = False,
    ) -> dict[str, Any]:
        body: dict[str, Any] = {}
        if name is not None:
            body["name"] = name
        if detach_profile:
            body["profileId"] = None
        elif profile_id is not None:
            body["profileId"] = profile_id
        return self._request("PUT", f"/api/projects/{project_id}", json_body=body, expected={200})

    def delete_project(self, project_id: int) -> None:
        self._request("DELETE", f"/api/projects/{project_id}", expected={204})

    def add_project_secret(self, project_id: int, hex_key: str | None = None) -> dict[str, Any]:
        body: dict[str, Any] = {}
        if hex_key:
            body["hexKey"] = hex_key
        return self._request(
            "POST",
            f"/api/projects/{project_id}/secrets",
            json_body=body,
            expected={201},
        )

    # Alias kept for callers written against the original form-login client.
    generate_secret = add_project_secret

    def list_external_ids(self, project_id: int) -> list[dict[str, Any]]:
        return self._request("GET", f"/api/projects/{project_id}/external-ids", expected={200}) or []

    def import_external_ids(self, project_id: int, rows: list[dict[str, Any]]) -> dict[str, Any]:
        return self._request(
            "POST",
            f"/api/projects/{project_id}/external-ids/import",
            json_body=rows,
            expected={200},
        )

    # ── forward nodes / sources / destinations ──────────────────────────

    def list_forward_nodes(self) -> list[dict[str, Any]]:
        return self._request("GET", "/api/forward-nodes", expected={200, 204}) or []

    def get_forward_node(self, forward_node_id: int) -> dict[str, Any]:
        return self._request("GET", f"/api/forward-nodes/{forward_node_id}", expected={200})

    def create_forward_node(self, ae_title: str, description: str = "") -> dict[str, Any]:
        return self._request(
            "POST",
            "/api/forward-nodes",
            json_body={"fwdAeTitle": ae_title, "fwdDescription": description},
            expected={201},
        )

    def update_forward_node(
        self,
        forward_node_id: int,
        ae_title: str,
        description: str = "",
    ) -> dict[str, Any]:
        return self._request(
            "PUT",
            f"/api/forward-nodes/{forward_node_id}",
            json_body={"fwdAeTitle": ae_title, "fwdDescription": description},
            expected={200},
        )

    def delete_forward_node(self, forward_node_id: int) -> None:
        self._request("DELETE", f"/api/forward-nodes/{forward_node_id}", expected={204})

    def list_source_nodes(self, forward_node_id: int) -> list[dict[str, Any]]:
        return self._request(
            "GET",
            f"/api/forward-nodes/{forward_node_id}/source-nodes",
            expected={200, 204},
        ) or []

    def add_source_node(self, forward_node_id: int, body: dict[str, Any]) -> dict[str, Any]:
        return self._request(
            "POST",
            f"/api/forward-nodes/{forward_node_id}/source-nodes",
            json_body=body,
            expected={201},
        )

    def delete_source_node(self, forward_node_id: int, source_node_id: int) -> None:
        self._request(
            "DELETE",
            f"/api/forward-nodes/{forward_node_id}/source-nodes/{source_node_id}",
            expected={204},
        )

    def list_destinations(self, forward_node_id: int) -> list[dict[str, Any]]:
        return self._request(
            "GET",
            f"/api/forward-nodes/{forward_node_id}/destinations",
            expected={200, 204},
        ) or []

    def add_destination(self, forward_node_id: int, body: dict[str, Any]) -> dict[str, Any]:
        return self._request(
            "POST",
            f"/api/forward-nodes/{forward_node_id}/destinations",
            json_body=body,
            expected={201},
        )

    # Alias kept for callers written against the original form-login client.
    create_destination = add_destination

    def update_destination(
        self,
        forward_node_id: int,
        destination_id: int,
        body: dict[str, Any],
    ) -> dict[str, Any]:
        return self._request(
            "PUT",
            f"/api/forward-nodes/{forward_node_id}/destinations/{destination_id}",
            json_body=body,
            expected={200},
        )

    def delete_destination(self, forward_node_id: int, destination_id: int) -> None:
        self._request(
            "DELETE",
            f"/api/forward-nodes/{forward_node_id}/destinations/{destination_id}",
            expected={204},
        )

    # ── auth configs ────────────────────────────────────────────────────

    def list_auth_configs(self) -> list[dict[str, Any]]:
        return self._request("GET", "/api/auth-configs", expected={200, 204}) or []

    def create_auth_config(self, body: dict[str, Any]) -> dict[str, Any]:
        return self._request("POST", "/api/auth-configs", json_body=body, expected={201})

    def update_auth_config(self, code: str, body: dict[str, Any]) -> dict[str, Any]:
        return self._request("PUT", f"/api/auth-configs/{code}", json_body=body, expected={200})

    # ── monitoring ──────────────────────────────────────────────────────

    def query_transfers(self, params: dict[str, Any] | None = None) -> dict[str, Any]:
        return self._request("GET", "/api/monitoring/transfers", params=params or {}, expected={200})

    def count_transfers(self, params: dict[str, Any] | None = None) -> dict[str, Any]:
        return self._request("GET", "/api/monitoring/transfers/count", params=params or {}, expected={200})
