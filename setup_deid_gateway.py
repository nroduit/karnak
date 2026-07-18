#!/usr/bin/env python3
"""
setup_deid_gateway.py
---------------------
Automates three steps against the Karnak REST API:
  1. Upload a de-identification profile YAML file
  2. Create a project linked to that profile (+ generate an HMAC secret)
  3. Add a de-identified DICOM destination to a forward node using that project

Uses form-login session authentication via _karnak_api.KarnakClient.

Usage
-----
  python setup_deid_gateway.py --profile my-profile.yml \
      --project-name "Clinical Trial 2024" \
      --fwd-aet GATEWAY1 \
      --dest-aet PACS_ANON \
      --dest-host 192.168.1.50 \
      --dest-port 11112

  # Pseudonym from a DICOM tag:
  python setup_deid_gateway.py ... --pseudonym-type EXTID_IN_TAG \
      --pseudonym-tag 00100010 --pseudonym-delimiter "^" --pseudonym-position 0

  # Use an existing forward node by ID instead of creating one:
  python setup_deid_gateway.py --profile my-profile.yml \
      --project-name "Clinical Trial 2024" \
      --fwd-id 3 \
      --dest-aet PACS_ANON \
      --dest-host 192.168.1.50 \
      --dest-port 11112
"""

import argparse
import sys
from pathlib import Path

# The client lives in python-client/ (installable as karnak-api-client); the
# path insert lets this script run from a bare checkout without pip install.
sys.path.insert(0, str(Path(__file__).resolve().parent / "python-client"))

from karnak_api_client import ApiError, AuthenticationError, KarnakClient, KarnakError  # noqa: E402

PSEUDONYM_TYPES = ("CACHE_EXTID", "EXTID_IN_TAG", "EXTID_API")


def die(msg: str) -> None:
    print(f"ERROR: {msg}", file=sys.stderr)
    sys.exit(1)


# ---------------------------------------------------------------------------
# Step 1: Upload profile YAML
# ---------------------------------------------------------------------------

def upload_profile(client: KarnakClient, profile_path: str) -> int:
    print(f"[1/3] Uploading profile: {profile_path}")
    data = client.upload_profile(profile_path)
    profile_id = data["id"]
    print(f"      Profile uploaded  id={profile_id}  name={data.get('name', '?')}")
    return profile_id


# ---------------------------------------------------------------------------
# Step 2: Create project + HMAC secret
# ---------------------------------------------------------------------------

def create_project(client: KarnakClient, name: str, profile_id: int) -> int:
    print(f"[2/3] Creating project: {name!r}  (profileId={profile_id})")
    data = client.create_project(name, profile_id)
    project_id = data["id"]
    print(f"      Project created   id={project_id}")

    secret = client.generate_secret(project_id)
    print(f"      HMAC secret       {secret.get('displayKey', '(generated)')}")

    return project_id


# ---------------------------------------------------------------------------
# Destination payload (de-id + pseudonym type)
# ---------------------------------------------------------------------------

def build_destination_payload(
    *,
    project_id: int,
    dest_description: str,
    dest_aet: str,
    dest_host: str,
    dest_port: int,
    pseudonym_type: str,
    issuer_by_default: str | None,
    pseudonym_tag: str | None,
    pseudonym_delimiter: str | None,
    pseudonym_position: int | None,
    save_pseudonym: bool | None,
    pseudonym_url: str | None,
    pseudonym_method: str | None,
    pseudonym_response_path: str | None,
    pseudonym_body: str | None,
    pseudonym_auth_config: str | None,
) -> dict:
    payload: dict = {
        "destinationType": "dicom",
        "description": dest_description,
        "aeTitle": dest_aet,
        "hostname": dest_host,
        "port": dest_port,
        "activate": True,
        "desidentification": True,
        "deIdentificationProject": {"id": project_id},
        "pseudonymType": pseudonym_type,
    }
    if issuer_by_default:
        payload["issuerByDefault"] = issuer_by_default

    if pseudonym_type == "EXTID_IN_TAG":
        payload["tag"] = pseudonym_tag
        if pseudonym_delimiter is not None:
            payload["delimiter"] = pseudonym_delimiter
        if pseudonym_position is not None:
            payload["position"] = pseudonym_position
        if save_pseudonym is not None:
            payload["savePseudonym"] = save_pseudonym
    elif pseudonym_type == "EXTID_API":
        payload["pseudonymUrl"] = pseudonym_url
        payload["method"] = pseudonym_method
        payload["responsePath"] = pseudonym_response_path
        if pseudonym_body:
            payload["body"] = pseudonym_body
        if pseudonym_auth_config:
            payload["authConfig"] = pseudonym_auth_config

    return payload


def validate_pseudonym_args(args: argparse.Namespace) -> None:
    if args.pseudonym_type == "EXTID_IN_TAG":
        if not args.pseudonym_tag:
            die("--pseudonym-tag is required when --pseudonym-type is EXTID_IN_TAG")
        if args.pseudonym_position is not None and args.pseudonym_position > 0 and not args.pseudonym_delimiter:
            die("--pseudonym-delimiter is required when --pseudonym-position > 0")
        if args.pseudonym_delimiter and args.pseudonym_position is None:
            die("--pseudonym-position is required when --pseudonym-delimiter is set")
    elif args.pseudonym_type == "EXTID_API":
        missing = [
            name
            for name, value in (
                ("--pseudonym-url", args.pseudonym_url),
                ("--pseudonym-method", args.pseudonym_method),
                ("--pseudonym-response-path", args.pseudonym_response_path),
            )
            if not value
        ]
        if missing:
            die(f"{', '.join(missing)} required when --pseudonym-type is EXTID_API")
        if args.pseudonym_method == "POST" and not args.pseudonym_body:
            die("--pseudonym-body is required when --pseudonym-method is POST")


# ---------------------------------------------------------------------------
# Step 3: Resolve / create forward node, then add de-identified destination
# ---------------------------------------------------------------------------

def setup_gateway(
    client: KarnakClient,
    project_id: int,
    fwd_id: int | None,
    fwd_aet: str | None,
    fwd_description: str,
    dest_aet: str,
    dest_host: str,
    dest_port: int,
    dest_description: str,
    pseudonym_type: str,
    issuer_by_default: str | None,
    pseudonym_tag: str | None,
    pseudonym_delimiter: str | None,
    pseudonym_position: int | None,
    save_pseudonym: bool | None,
    pseudonym_url: str | None,
    pseudonym_method: str | None,
    pseudonym_response_path: str | None,
    pseudonym_body: str | None,
    pseudonym_auth_config: str | None,
) -> None:
    print("[3/3] Configuring gateway destination with de-identification")

    if fwd_id is not None:
        node = client.get_forward_node(fwd_id)
        print(f"      Using existing forward node  id={fwd_id}  aet={node.get('fwdAeTitle', '?')}")
    else:
        if not fwd_aet:
            die("Provide --fwd-id or --fwd-aet to identify the forward node.")
        try:
            node = client.create_forward_node(fwd_aet, fwd_description)
            fwd_id = node["id"]
            print(f"      Forward node created  id={fwd_id}  aet={fwd_aet}")
        except ApiError as exc:
            if exc.status_code != 409:
                raise
            # Already exists — find it by listing
            all_nodes = client.list_forward_nodes()
            matches = [n for n in all_nodes if n.get("fwdAeTitle") == fwd_aet]
            if not matches:
                die(f"Forward node AET {fwd_aet!r} conflict but not found in list.")
            node = matches[0]
            fwd_id = node["id"]
            print(f"      Forward node already exists  id={fwd_id}  aet={fwd_aet}")

    dest_payload = build_destination_payload(
        project_id=project_id,
        dest_description=dest_description,
        dest_aet=dest_aet,
        dest_host=dest_host,
        dest_port=dest_port,
        pseudonym_type=pseudonym_type,
        issuer_by_default=issuer_by_default,
        pseudonym_tag=pseudonym_tag,
        pseudonym_delimiter=pseudonym_delimiter,
        pseudonym_position=pseudonym_position,
        save_pseudonym=save_pseudonym,
        pseudonym_url=pseudonym_url,
        pseudonym_method=pseudonym_method,
        pseudonym_response_path=pseudonym_response_path,
        pseudonym_body=pseudonym_body,
        pseudonym_auth_config=pseudonym_auth_config,
    )
    dest = client.create_destination(fwd_id, dest_payload)
    print(
        f"      Destination created  id={dest.get('id', '?')}  "
        f"aet={dest_aet}  host={dest_host}:{dest_port}"
    )
    print("      De-identification: ENABLED")
    print(f"      Pseudonym type:      {pseudonym_type}")


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description="Upload a profile, create a project, and wire it to a Karnak de-id gateway."
    )
    p.add_argument("--base-url", default="http://localhost:8081", help="Karnak base URL")
    p.add_argument("--user", default="admin", help="Login username")
    p.add_argument("--password", default="karnak", help="Login password")

    p.add_argument("--profile", required=True, metavar="FILE", help="Path to profile YAML file")
    p.add_argument("--project-name", required=True, metavar="NAME", help="Project name to create")

    fwd = p.add_mutually_exclusive_group(required=True)
    fwd.add_argument("--fwd-id", type=int, metavar="ID", help="Existing forward node ID")
    fwd.add_argument("--fwd-aet", metavar="AET", help="Forward node AE Title (create if absent)")

    p.add_argument("--fwd-description", default="De-identification gateway", metavar="TEXT")

    p.add_argument("--dest-aet", required=True, metavar="AET", help="Destination AE Title")
    p.add_argument("--dest-host", required=True, metavar="HOST", help="Destination hostname/IP")
    p.add_argument("--dest-port", required=True, type=int, metavar="PORT", help="Destination port")
    p.add_argument(
        "--dest-description", default="De-identified DICOM archive", metavar="TEXT"
    )

    p.add_argument(
        "--pseudonym-type",
        default="CACHE_EXTID",
        choices=PSEUDONYM_TYPES,
        help="How to resolve the external pseudonym (default: CACHE_EXTID)",
    )
    p.add_argument(
        "--issuer-by-default",
        default=None,
        metavar="TEXT",
        help="Default Issuer of Patient ID for de-identification",
    )

    tag = p.add_argument_group("EXTID_IN_TAG options")
    tag.add_argument("--pseudonym-tag", metavar="TAG", help="DICOM tag, 8 hex digits (e.g. 00100010)")
    tag.add_argument("--pseudonym-delimiter", metavar="CHAR", help="Delimiter to split tag value")
    tag.add_argument("--pseudonym-position", type=int, metavar="N", help="Index after split (0-based)")
    tag.add_argument(
        "--save-pseudonym",
        action="store_true",
        help="Store pseudonym read from tag in project cache",
    )

    api = p.add_argument_group("EXTID_API options")
    api.add_argument("--pseudonym-url", metavar="URL", help="External API URL")
    api.add_argument(
        "--pseudonym-method",
        choices=("GET", "POST"),
        help="HTTP method (required for EXTID_API)",
    )
    api.add_argument(
        "--pseudonym-response-path", metavar="PATH", help="JSON path to pseudonym in response"
    )
    api.add_argument("--pseudonym-body", metavar="JSON", help="Request body (required for POST)")
    api.add_argument(
        "--pseudonym-auth-config",
        metavar="CODE",
        help="Auth config code from /api/auth-configs",
    )

    return p.parse_args()


def main() -> None:
    args = parse_args()
    validate_pseudonym_args(args)

    client = KarnakClient(
        base_url=args.base_url,
        username=args.user,
        password=args.password,
    )

    try:
        client.check_connectivity()
    except AuthenticationError as exc:
        die(str(exc))
    except KarnakError as exc:
        die(str(exc))

    try:
        profile_id = upload_profile(client, args.profile)
        project_id = create_project(client, args.project_name, profile_id)
        setup_gateway(
            client=client,
            project_id=project_id,
            fwd_id=args.fwd_id,
            fwd_aet=args.fwd_aet,
            fwd_description=args.fwd_description,
            dest_aet=args.dest_aet,
            dest_host=args.dest_host,
            dest_port=args.dest_port,
            dest_description=args.dest_description,
            pseudonym_type=args.pseudonym_type,
            issuer_by_default=args.issuer_by_default,
            pseudonym_tag=args.pseudonym_tag,
            pseudonym_delimiter=args.pseudonym_delimiter,
            pseudonym_position=args.pseudonym_position,
            save_pseudonym=args.save_pseudonym if args.save_pseudonym else None,
            pseudonym_url=args.pseudonym_url,
            pseudonym_method=args.pseudonym_method,
            pseudonym_response_path=args.pseudonym_response_path,
            pseudonym_body=args.pseudonym_body,
            pseudonym_auth_config=args.pseudonym_auth_config,
        )
    except (ApiError, KarnakError) as exc:
        die(str(exc))

    print("\nDone.")
    print(f"  Profile ID : {profile_id}")
    print(f"  Project ID : {project_id}")


if __name__ == "__main__":
    main()
