"""Tests for idempotent Karnak apply orchestration.

Ported from MiCo-BID-pipeline tests/unit/test_karnak_apply.py when the apply
layer moved into this package.
"""

from __future__ import annotations

from pathlib import Path
from typing import Any

from karnak_api_client import apply_config


class FakeKarnakClient:
    def __init__(self) -> None:
        self.updated_destinations: list[dict[str, Any]] = []
        self.uploaded_profiles: list[Path] = []

    def list_profiles(self) -> list[dict[str, Any]]:
        return [{"id": 3, "name": "Profile A", "version": "1.0"}]

    def upload_profile(self, path: Path) -> dict[str, Any]:
        self.uploaded_profiles.append(path)
        return {"id": 99, "name": "Uploaded"}

    def list_projects(self) -> list[dict[str, Any]]:
        return [{"id": 4, "name": "Project A"}]

    def update_project(self, project_id: int, **_kwargs: Any) -> dict[str, Any]:
        return {"id": project_id}

    def get_project(self, project_id: int) -> dict[str, Any]:
        return {
            "id": project_id,
            "secretEntities": [{"id": 1, "active": True}],
        }

    def create_project(self, name: str, profile_id: int | None = None) -> dict[str, Any]:
        return {"id": 44, "name": name, "profileId": profile_id}

    def add_project_secret(self, project_id: int, hex_key: str | None = None) -> dict[str, Any]:
        return {"projectId": project_id, "hexKey": hex_key or "generated", "active": True}

    def list_auth_configs(self) -> list[dict[str, Any]]:
        return []

    def list_forward_nodes(self) -> list[dict[str, Any]]:
        return [{"id": 5, "fwdAeTitle": "KARNAK-GATEWAY"}]

    def update_forward_node(self, forward_node_id: int, ae_title: str, description: str = "") -> dict[str, Any]:
        return {"id": forward_node_id, "fwdAeTitle": ae_title, "fwdDescription": description}

    def create_forward_node(self, ae_title: str, description: str = "") -> dict[str, Any]:
        return {"id": 55, "fwdAeTitle": ae_title, "fwdDescription": description}

    def list_source_nodes(self, _forward_node_id: int) -> list[dict[str, Any]]:
        return []

    def list_destinations(self, _forward_node_id: int) -> list[dict[str, Any]]:
        return [{"id": 6, "destinationType": "dicom", "aeTitle": "LOCAL-STORAGE"}]

    def update_destination(
        self,
        _forward_node_id: int,
        destination_id: int,
        body: dict[str, Any],
    ) -> dict[str, Any]:
        self.updated_destinations.append({"id": destination_id, **body})
        return {"id": destination_id, **body}

    def add_destination(self, _forward_node_id: int, body: dict[str, Any]) -> dict[str, Any]:
        return {"id": 66, **body}


def test_apply_config_reuses_existing_resources_and_links_destination(tmp_path: Path) -> None:
    profile = tmp_path / "profile.yml"
    profile.write_text("name: Profile A\nversion: '1.0'\n", encoding="utf-8")
    client = FakeKarnakClient()

    summary = apply_config(
        client,  # type: ignore[arg-type]
        {
            "profile": {"path": str(profile), "name": "Profile A", "version": "1.0"},
            "project": {"name": "Project A", "ensureSecret": True},
            "forwardNode": {"aeTitle": "KARNAK-GATEWAY"},
            "sourceNodes": [],
            "destinations": [
                {
                    "destinationType": "dicom",
                    "aeTitle": "LOCAL-STORAGE",
                    "hostname": "dicom_receiver",
                    "port": 11104,
                    "activate": True,
                    "desidentification": True,
                }
            ],
        },
    )

    assert summary["profile"]["created"] is False
    assert summary["project"]["created"] is False
    assert summary["project"]["secret_created"] is False
    assert client.uploaded_profiles == []
    assert client.updated_destinations[0]["deIdentificationProject"] == {"id": 4}
