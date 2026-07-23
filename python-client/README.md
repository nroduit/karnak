# karnak-api-client

Python client for the REST API exposed by this Karnak fork
([API.md](../API.md) documents every endpoint).

Two layers:

- `karnak_api_client.client` — `KarnakClient`: thin authenticated transport
  mirroring the REST resources. Supports three auth modes:
  - `basic` — HTTP Basic on every request (default in-memory IdP enables it);
  - `form` — Spring form-login: POST `/login`, reuse the `JSESSIONID` cookie;
  - `auto` (default) — try Basic, fall back to form-login transparently when
    the server redirects to `/login` or returns 401.
- `karnak_api_client.apply` — `apply_config(client, config)`: idempotent
  desired-state application (profile, project + HMAC secret, auth configs,
  forward node, source nodes, destinations, external-ID import) from a JSON
  config document.

## Install

```sh
pip install "karnak-api-client @ git+https://github.com/jbardet/karnak.git@client-v0.1.0#subdirectory=python-client"
```

## Usage

```python
from karnak_api_client import KarnakClient, KarnakClientConfig

client = KarnakClient(KarnakClientConfig(
    base_url="http://localhost:8081", username="admin", password="karnak",
))
profiles = client.list_profiles()
```

Or configure from the environment (`KARNAK_API_BASE`, `KARNAK_USERNAME`,
`KARNAK_PASSWORD`, `KARNAK_AUTH_MODE`, `KARNAK_API_TIMEOUT_SEC`):

```python
client = KarnakClient()  # KarnakClientConfig.from_env()
```

## Versioning

Tag scheme `client-v<semver>` on this repo. The Docker image of the fork uses
`<upstream-version>-api.<n>` tags; the two are released independently but the
client tracks the controllers in `src/main/java/org/karnak/backend/controller/`.

## Consumers

- MiCo-BID-pipeline (DAG step `00_apply_karnak_config.py`, `micobid` CLI)
- TMLCTP equivalence harness (`integration_tests/karnak_api/`)
- `setup_deid_gateway.py` in this repo
