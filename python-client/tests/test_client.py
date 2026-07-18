"""Unit tests for the unified Karnak client (auth modes + transport)."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

import pytest
import requests

from karnak_api_client import (
    AuthenticationError,
    KarnakApiError,
    KarnakClient,
    KarnakClientConfig,
    KarnakError,
)

BASE = "http://karnak.test:8081"


def make_config(auth_mode: str = "auto") -> KarnakClientConfig:
    return KarnakClientConfig(
        base_url=BASE, username="admin", password="karnak", auth_mode=auth_mode
    )


class FakeResponse:
    def __init__(
        self,
        status_code: int,
        body: Any = None,
        headers: dict[str, str] | None = None,
    ) -> None:
        self.status_code = status_code
        self.headers = headers or {}
        if body is None:
            self.content = b""
            self.text = ""
        elif isinstance(body, str):
            self.content = body.encode()
            self.text = body
            self.headers.setdefault("Content-Type", "text/html")
        else:
            self.text = json.dumps(body)
            self.content = self.text.encode()
            self.headers.setdefault("Content-Type", "application/json")
        self._body = body

    def json(self) -> Any:
        return self._body


class FakeCookies:
    def __init__(self) -> None:
        self.cleared = 0

    def clear(self) -> None:
        self.cleared += 1


class FakeSession:
    """Queue-driven stand-in for requests.Session."""

    def __init__(self, responses: list[FakeResponse]) -> None:
        self.responses = list(responses)
        self.calls: list[dict[str, Any]] = []
        self.headers: dict[str, str] = {}
        self.cookies = FakeCookies()

    def request(self, method: str, url: str, **kwargs: Any) -> FakeResponse:
        self.calls.append({"method": method, "url": url, **kwargs})
        if not self.responses:
            raise AssertionError(f"Unexpected request: {method} {url}")
        return self.responses.pop(0)

    def post(self, url: str, **kwargs: Any) -> FakeResponse:
        return self.request("POST", url, **kwargs)


def make_client(responses: list[FakeResponse], auth_mode: str = "auto") -> tuple[KarnakClient, FakeSession]:
    session = FakeSession(responses)
    client = KarnakClient(make_config(auth_mode), session=session)  # type: ignore[arg-type]
    return client, session


def login_ok() -> FakeResponse:
    return FakeResponse(302, headers={"Location": f"{BASE}/"})


def login_redirect() -> FakeResponse:
    return FakeResponse(302, headers={"Location": f"{BASE}/login"})


# ── basic mode ──────────────────────────────────────────────────────────────


def test_basic_mode_sends_basic_auth_and_parses_json() -> None:
    client, session = make_client([FakeResponse(200, [{"id": 1}])], auth_mode="basic")
    assert client.list_profiles() == [{"id": 1}]
    call = session.calls[0]
    assert call["auth"] is not None
    assert call["allow_redirects"] is False


def test_basic_mode_does_not_fall_back_on_401() -> None:
    client, session = make_client([FakeResponse(401, "nope")], auth_mode="basic")
    with pytest.raises(KarnakApiError) as exc:
        client.list_profiles()
    assert exc.value.status_code == 401
    assert len(session.calls) == 1


# ── auto mode fallback ──────────────────────────────────────────────────────


@pytest.mark.parametrize("first", [login_redirect(), FakeResponse(401, "nope")])
def test_auto_mode_falls_back_to_form_login(first: FakeResponse) -> None:
    client, session = make_client([first, login_ok(), FakeResponse(200, [{"id": 7}])])
    assert client.list_profiles() == [{"id": 7}]
    methods = [(c["method"], c["url"]) for c in session.calls]
    assert methods == [
        ("GET", f"{BASE}/api/profiles"),
        ("POST", f"{BASE}/login"),
        ("GET", f"{BASE}/api/profiles"),
    ]
    # After fallback the client stays in form mode: no Basic auth on retry.
    assert session.calls[2]["auth"] is None
    # Subsequent calls skip Basic entirely without a new login.
    session.responses = [FakeResponse(200, [])]
    client.list_projects()
    assert session.calls[3]["auth"] is None


def test_auto_mode_raises_authentication_error_on_bad_credentials() -> None:
    client, _ = make_client(
        [login_redirect(), FakeResponse(302, headers={"Location": f"{BASE}/login?error"})]
    )
    with pytest.raises(AuthenticationError):
        client.list_profiles()


# ── form mode ───────────────────────────────────────────────────────────────


def test_form_mode_logs_in_before_first_request() -> None:
    client, session = make_client([login_ok(), FakeResponse(200, [])], auth_mode="form")
    client.list_profiles()
    methods = [(c["method"], c["url"]) for c in session.calls]
    assert methods[0] == ("POST", f"{BASE}/login")
    assert session.calls[1]["auth"] is None


def test_form_mode_relogins_when_session_expires() -> None:
    client, session = make_client(
        [login_ok(), login_redirect(), login_ok(), FakeResponse(200, [{"id": 2}])],
        auth_mode="form",
    )
    assert client.list_profiles() == [{"id": 2}]
    assert session.cookies.cleared == 1
    methods = [(c["method"], c["url"]) for c in session.calls]
    assert methods == [
        ("POST", f"{BASE}/login"),
        ("GET", f"{BASE}/api/profiles"),
        ("POST", f"{BASE}/login"),
        ("GET", f"{BASE}/api/profiles"),
    ]


def test_invalidate_session_clears_cookies_and_forces_relogin() -> None:
    client, session = make_client(
        [login_ok(), FakeResponse(200, []), login_ok(), FakeResponse(200, [])],
        auth_mode="form",
    )
    client.list_profiles()
    client.invalidate_session()
    client.list_profiles()
    posts = [c for c in session.calls if c["method"] == "POST"]
    assert len(posts) == 2


# ── transport behaviour ─────────────────────────────────────────────────────


def test_204_responses_map_to_empty_list() -> None:
    client, _ = make_client([FakeResponse(204)], auth_mode="basic")
    assert client.list_forward_nodes() == []


def test_unexpected_status_raises_api_error_with_details() -> None:
    client, _ = make_client([FakeResponse(500, "boom")], auth_mode="basic")
    with pytest.raises(KarnakApiError) as exc:
        client.get_project(3)
    assert exc.value.status_code == 500
    assert exc.value.method == "GET"
    assert "boom" in exc.value.body


def test_connection_error_wrapped_as_karnak_error() -> None:
    class ExplodingSession(FakeSession):
        def request(self, method: str, url: str, **kwargs: Any) -> FakeResponse:
            raise requests.ConnectionError("refused")

    session = ExplodingSession([])
    client = KarnakClient(make_config("basic"), session=session)  # type: ignore[arg-type]
    with pytest.raises(KarnakError):
        client.check_connectivity()


def test_upload_profile_accepts_str_and_survives_form_fallback(tmp_path: Path) -> None:
    profile = tmp_path / "p.yml"
    profile.write_bytes(b"name: X\n")
    client, session = make_client(
        [login_redirect(), login_ok(), FakeResponse(201, {"id": 12, "name": "X"})]
    )
    created = client.upload_profile(str(profile))
    assert created["id"] == 12
    # Both the first attempt and the retry carried the file payload.
    uploads = [c for c in session.calls if c["url"].endswith("/api/profiles")]
    for call in uploads:
        name, payload, content_type = call["files"]["file"]
        assert name == "p.yml"
        assert payload == b"name: X\n"
        assert content_type == "application/x-yaml"


# ── compatibility surface ───────────────────────────────────────────────────


def test_aliases_point_at_canonical_methods() -> None:
    assert KarnakClient.generate_secret is KarnakClient.add_project_secret
    assert KarnakClient.create_destination is KarnakClient.add_destination


def test_kwargs_constructor_and_base_property() -> None:
    session = FakeSession([])
    client = KarnakClient(
        base_url=f"{BASE}/",
        username="u",
        password="p",
        auth_mode="form",
        session=session,  # type: ignore[arg-type]
    )
    assert client.base == BASE
    assert client.config.username == "u"
    assert client.config.auth_mode == "form"


def test_invalid_auth_mode_rejected() -> None:
    with pytest.raises(ValueError):
        KarnakClientConfig(base_url=BASE, username="u", password="p", auth_mode="oauth")
