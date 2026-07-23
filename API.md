# Karnak REST API Reference

This document covers every REST API endpoint exposed by Karnak, with full request/response details and `curl` examples.

**Base URL:** `http://localhost:8081`
**Default credentials:** `admin` / `karnak`
**Authentication:** HTTP Basic or Spring form-login session cookie (all endpoints except `/api/echo/destinations`).
> With the default in-memory IdP this build accepts HTTP Basic on `/api/*`.
> Form-login also works: POST credentials to `/login` once and carry the
> resulting `JSESSIONID` cookie on every subsequent request.  The Python
> package in `python-client/` (`karnak-api-client`) handles both via
> `KarnakClient` (auth_mode `auto`/`basic`/`form`).
>
> ```
> POST /login
> Content-Type: application/x-www-form-urlencoded
>
> username=admin&password=karnak
> ```
>
> On success Karnak responds `302` to `/`; on failure `302` to `/login?error`.
> The `curl` examples below use `--cookie-jar` / `--cookie` to replicate this:
>
> ```bash
> # Obtain session cookie
> curl -c /tmp/karnak.jar -X POST http://localhost:8081/login \
>   -d "username=admin&password=karnak"
>
> # Use the cookie for API calls
> curl -b /tmp/karnak.jar http://localhost:8081/api/forward-nodes
> ```
>
> When Karnak is started with `IDP=oidc`, the form-login endpoint uses OIDC
> instead of in-memory credentials and these API calls can only be reached
> through an authenticated OAuth2 browser session.

---

## Table of Contents

1. [Echo](#1-echo)
2. [Forward Nodes](#2-forward-nodes)
3. [Source Nodes](#3-source-nodes)
4. [Destinations](#4-destinations)
5. [Profiles](#5-profiles)
6. [Projects](#6-projects)
7. [Project Secrets](#7-project-secrets)
8. [External IDs](#8-external-ids)
9. [Auth Configs](#9-auth-configs)
10. [Monitoring](#10-monitoring)
11. [End-to-end example](#11-end-to-end-example)

---

## 1. Echo

Check the connectivity status of configured destinations for a given source AE Title. **No authentication required.**

### `GET /api/echo/destinations`

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `srcAet` | query | yes | Source AE Title |

**Response codes:** `200 OK`, `204 No Content`

**Response body (200):**
```xml
<destinations>
    <destination>
        <url>https://dicomweb.example.com/studies</url>
        <status>0</status>
    </destination>
    <destination>
        <aet>ARCHIVE</aet>
        <status>0</status>
    </destination>
</destinations>
```

> `status: 0` = reachable. Non-zero = unreachable or error.

```bash
# XML response (default)
curl "http://localhost:8081/api/echo/destinations?srcAet=MY_GATEWAY"

# JSON response
curl -H "Accept: application/json" \
     "http://localhost:8081/api/echo/destinations?srcAet=MY_GATEWAY"
```

---

## 2. Forward Nodes

A **Forward Node** represents a gateway entry point identified by an AE Title. DICOM senders target this AE Title to push images through Karnak.

### `GET /api/forward-nodes`

List all forward nodes.

**Response codes:** `200 OK`, `204 No Content`

```bash
curl -u admin:karnak http://localhost:8081/api/forward-nodes
```

**Response body (200):**
```json
[
  {
    "id": 1,
    "fwdAeTitle": "MY_GATEWAY",
    "fwdDescription": "Main gateway node",
    "sourceNodes": [],
    "destinationEntities": []
  }
]
```

---

### `POST /api/forward-nodes`

Create a new forward node.

**Request body:**
```json
{
  "fwdAeTitle": "MY_GATEWAY",
  "fwdDescription": "Main gateway node"
}
```

**Response codes:** `201 Created`, `400 Bad Request` (missing/blank `fwdAeTitle`), `409 Conflict` (AE Title already exists)

```bash
curl -u admin:karnak -X POST http://localhost:8081/api/forward-nodes \
  -H "Content-Type: application/json" \
  -d '{"fwdAeTitle": "MY_GATEWAY", "fwdDescription": "Main gateway node"}'
```

---

### `GET /api/forward-nodes/{id}`

Get a forward node by ID.

**Response codes:** `200 OK`, `404 Not Found`

```bash
curl -u admin:karnak http://localhost:8081/api/forward-nodes/1
```

---

### `PUT /api/forward-nodes/{id}`

Update a forward node. **Only `fwdAeTitle` and `fwdDescription` are updatable** —
the server preserves existing source nodes and destinations even if they are
absent (or empty) in the request body. To add/remove children, use the
`/source-nodes` and `/destinations` sub-resources.

**Request body:**
```json
{
  "fwdAeTitle": "MY_GATEWAY",
  "fwdDescription": "Updated description"
}
```

**Response codes:** `200 OK`, `404 Not Found`

```bash
curl -u admin:karnak -X PUT http://localhost:8081/api/forward-nodes/1 \
  -H "Content-Type: application/json" \
  -d '{"fwdAeTitle": "MY_GATEWAY", "fwdDescription": "Updated description"}'
```

---

### `DELETE /api/forward-nodes/{id}`

Delete a forward node and all its source nodes and destinations.

**Response codes:** `204 No Content`, `404 Not Found`

```bash
curl -u admin:karnak -X DELETE http://localhost:8081/api/forward-nodes/1
```

---

## 3. Source Nodes

Source nodes restrict which DICOM senders are accepted by a forward node. If none are configured, all sources are accepted.

### `GET /api/forward-nodes/{id}/source-nodes`

List source nodes for a forward node.

**Response codes:** `200 OK`, `404 Not Found`

```bash
curl -u admin:karnak http://localhost:8081/api/forward-nodes/1/source-nodes
```

**Response body (200):**
```json
[
  {
    "id": 5,
    "description": "Main PACS",
    "aeTitle": "PACS_SRC",
    "hostname": "192.168.1.10",
    "checkHostname": true
  }
]
```

---

### `POST /api/forward-nodes/{id}/source-nodes`

Add a source node.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `aeTitle` | string | yes | AE Title (max 16 chars) |
| `hostname` | string | no | IP or hostname |
| `checkHostname` | boolean | no | Enforce hostname check (default: false) |
| `description` | string | no | Free text description |

**Response codes:** `201 Created`, `404 Not Found`

```bash
curl -u admin:karnak -X POST http://localhost:8081/api/forward-nodes/1/source-nodes \
  -H "Content-Type: application/json" \
  -d '{
    "aeTitle": "PACS_SRC",
    "hostname": "192.168.1.10",
    "checkHostname": true,
    "description": "Main PACS"
  }'
```

---

### `DELETE /api/forward-nodes/{id}/source-nodes/{sourceNodeId}`

Remove a source node.

**Response codes:** `204 No Content`, `404 Not Found`

```bash
curl -u admin:karnak -X DELETE http://localhost:8081/api/forward-nodes/1/source-nodes/5
```

---

## 4. Destinations

A destination is a forwarding target — either a **DICOM node** (`type: dicom`) or a **DICOMWeb STOW-RS endpoint** (`type: stow`).

### `GET /api/forward-nodes/{id}/destinations`

List all destinations of a forward node.

**Response codes:** `200 OK`, `404 Not Found`

```bash
curl -u admin:karnak http://localhost:8081/api/forward-nodes/1/destinations
```

---

### `POST /api/forward-nodes/{id}/destinations`

Add a destination.

#### DICOM destination

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `destinationType` | string | yes | Must be `"dicom"` |
| `aeTitle` | string | yes | Destination AE Title (max 16 chars) |
| `hostname` | string | yes | Destination host/IP |
| `port` | integer | yes | Port (1–65535) |
| `description` | string | no | Free text description |
| `activate` | boolean | no | Enable/disable (default: true) |
| `condition` | string | no | DICOM expression filter |
| `useaetdest` | boolean | no | Use destination AET as calling AET |
| `desidentification` | boolean | no | Enable de-identification |
| `deIdentificationProject` | object | no | `{"id": N}` — project to use for de-identification |
| `pseudonymType` | string | no | How the external pseudonym is resolved when de-identification is enabled (default: `CACHE_EXTID`). See [Pseudonym type](#pseudonym-type-de-identification) below. |
| `issuerByDefault` | string | no | Default Issuer of Patient ID used with Patient ID for unique identification |
| `tag` | string | no | DICOM tag (8 hex digits, e.g. `00100010`) — required for `EXTID_IN_TAG` |
| `delimiter` | string | no | Delimiter to split tag value — required when `position` > 0 for `EXTID_IN_TAG` |
| `position` | integer | no | Zero-based index after split — required when `delimiter` is set for `EXTID_IN_TAG` |
| `savePseudonym` | boolean | no | Store pseudonym read from DICOM tag in cache (`EXTID_IN_TAG` only) |
| `pseudonymUrl` | string | no | External API URL (DICOM expressions allowed) — required for `EXTID_API` |
| `method` | string | no | `GET` or `POST` — required for `EXTID_API` |
| `responsePath` | string | no | JSON path to pseudonym in API response — required for `EXTID_API` |
| `body` | string | no | JSON request body — required for `EXTID_API` when `method` is `POST` |
| `authConfig` | string | no | Auth config code (from `/api/auth-configs`) for `EXTID_API` |
| `activateTagMorphing` | boolean | no | Enable tag morphing |
| `tagMorphingProject` | object | no | `{"id": N}` — project for tag morphing |
| `activateNotification` | boolean | no | Enable email notifications |
| `notify` | string | no | Comma-separated email list |
| `notifyObjectPattern` | string | no | Email subject pattern |
| `notifyObjectValues` | string | no | DICOM tag values to embed in subject |
| `notifyInterval` | integer | no | Notification delay in seconds |
| `filterBySOPClasses` | boolean | no | Enable SOP class filtering |
| `transferSyntax` | string | no | Transfer syntax UID |
| `transcodeOnlyUncompressed` | boolean | no | Only transcode uncompressed images |

```bash
curl -u admin:karnak -X POST http://localhost:8081/api/forward-nodes/1/destinations \
  -H "Content-Type: application/json" \
  -d '{
    "destinationType": "dicom",
    "description": "Long-term archive",
    "aeTitle": "ARCHIVE",
    "hostname": "192.168.1.20",
    "port": 11112,
    "activate": true
  }'
```

#### DICOM destination with de-identification

```bash
curl -u admin:karnak -X POST http://localhost:8081/api/forward-nodes/1/destinations \
  -H "Content-Type: application/json" \
  -d '{
    "destinationType": "dicom",
    "aeTitle": "ARCHIVE_ANON",
    "hostname": "192.168.1.20",
    "port": 11112,
    "activate": true,
    "desidentification": true,
    "deIdentificationProject": {"id": 1},
    "activateNotification": true,
    "notify": "admin@hospital.org",
    "notifyObjectPattern": "[Karnak] %s %.30s",
    "notifyObjectValues": "PatientID,StudyDescription",
    "notifyInterval": 45
  }'
```

#### Pseudonym type (de-identification)

When `desidentification` is `true`, `pseudonymType` selects how Karnak obtains the **external pseudonym** before applying the project profile. Use the **enum name** in JSON (not the UI label).

| Value | Description | Additional fields |
|-------|-------------|-------------------|
| `CACHE_EXTID` | Lookup in the project's external-ID cache (default) | Pre-load mappings via `POST /api/projects/{id}/external-ids` |
| `EXTID_IN_TAG` | Read from a DICOM tag (optional split by delimiter) | `tag`, optional `delimiter` + `position`, optional `savePseudonym` |
| `EXTID_API` | HTTP call to an external service | `pseudonymUrl`, `method`, `responsePath`, optional `body` (POST), optional `authConfig` |

**DICOM destination — pseudonym from cache (default):**

```bash
curl -u admin:karnak -X POST http://localhost:8081/api/forward-nodes/1/destinations \
  -H "Content-Type: application/json" \
  -d '{
    "destinationType": "dicom",
    "aeTitle": "ARCHIVE_ANON",
    "hostname": "192.168.1.20",
    "port": 11112,
    "activate": true,
    "desidentification": true,
    "deIdentificationProject": {"id": 1},
    "pseudonymType": "CACHE_EXTID"
  }'
```

**DICOM destination — pseudonym in a DICOM tag:**

```bash
curl -u admin:karnak -X POST http://localhost:8081/api/forward-nodes/1/destinations \
  -H "Content-Type: application/json" \
  -d '{
    "destinationType": "dicom",
    "aeTitle": "ARCHIVE_ANON",
    "hostname": "192.168.1.20",
    "port": 11112,
    "activate": true,
    "desidentification": true,
    "deIdentificationProject": {"id": 1},
    "pseudonymType": "EXTID_IN_TAG",
    "tag": "00100010",
    "delimiter": "^",
    "position": 0
  }'
```

**DICOM destination — pseudonym from external API:**

```bash
curl -u admin:karnak -X POST http://localhost:8081/api/forward-nodes/1/destinations \
  -H "Content-Type: application/json" \
  -d '{
    "destinationType": "dicom",
    "aeTitle": "ARCHIVE_ANON",
    "hostname": "192.168.1.20",
    "port": 11112,
    "activate": true,
    "desidentification": true,
    "deIdentificationProject": {"id": 1},
    "pseudonymType": "EXTID_API",
    "pseudonymUrl": "https://pseudonym.example.com/lookup?patientId={PatientID}",
    "method": "GET",
    "responsePath": "$.pseudonym"
  }'
```

To change `pseudonymType` on an existing destination, `GET` the destination, update the field (and type-specific fields), then `PUT` the full object to `/api/forward-nodes/{id}/destinations/{destinationId}`.

#### STOW-RS destination

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `destinationType` | string | yes | Must be `"stow"` |
| `url` | string | yes | STOW-RS endpoint URL |
| `description` | string | no | Free text description |
| `headers` | string | no | HTTP headers (max 4096 chars) |
| `activate` | boolean | no | Enable/disable (default: true) |
| `condition` | string | no | DICOM expression filter |
| `authConfig` | string | no | Auth config code (from `/api/auth-configs`) |
| `desidentification` | boolean | no | Enable de-identification |
| `deIdentificationProject` | object | no | `{"id": N}` |
| `pseudonymType` | string | no | Same as DICOM — see [Pseudonym type](#pseudonym-type-de-identification) |
| `issuerByDefault` | string | no | Default Issuer of Patient ID |
| `tag` | string | no | For `EXTID_IN_TAG` |
| `delimiter` | string | no | For `EXTID_IN_TAG` |
| `position` | integer | no | For `EXTID_IN_TAG` |
| `savePseudonym` | boolean | no | For `EXTID_IN_TAG` |
| `pseudonymUrl` | string | no | For `EXTID_API` |
| `method` | string | no | For `EXTID_API` |
| `responsePath` | string | no | For `EXTID_API` |
| `body` | string | no | For `EXTID_API` (POST) |
| `authConfig` | string | no | For `EXTID_API` or STOW OAuth |
| `activateTagMorphing` | boolean | no | Enable tag morphing |
| `tagMorphingProject` | object | no | `{"id": N}` |
| `activateNotification` | boolean | no | Enable email notifications |
| `notify` | string | no | Comma-separated email list |
| `filterBySOPClasses` | boolean | no | Enable SOP class filtering |
| `transferSyntax` | string | no | Transfer syntax UID |

```bash
curl -u admin:karnak -X POST http://localhost:8081/api/forward-nodes/1/destinations \
  -H "Content-Type: application/json" \
  -d '{
    "destinationType": "stow",
    "description": "Cloud DICOMWeb endpoint",
    "url": "https://dicomweb.example.com/wado/rs/studies",
    "headers": "Authorization: Bearer eyJhbGc...",
    "activate": true
  }'
```

#### STOW-RS with OAuth2 auth config

```bash
curl -u admin:karnak -X POST http://localhost:8081/api/forward-nodes/1/destinations \
  -H "Content-Type: application/json" \
  -d '{
    "destinationType": "stow",
    "url": "https://dicomweb.example.com/wado/rs/studies",
    "activate": true,
    "authConfig": "keycloak-prod"
  }'
```

**Response codes:** `201 Created`, `404 Not Found`

---

### `PUT /api/forward-nodes/{id}/destinations/{destinationId}`

Update a destination. Send the full destination object.

**Response codes:** `200 OK`, `404 Not Found`

```bash
curl -u admin:karnak -X PUT http://localhost:8081/api/forward-nodes/1/destinations/3 \
  -H "Content-Type: application/json" \
  -d '{
    "destinationType": "dicom",
    "aeTitle": "ARCHIVE",
    "hostname": "192.168.1.20",
    "port": 11113,
    "activate": false
  }'
```

---

### `DELETE /api/forward-nodes/{id}/destinations/{destinationId}`

Delete a destination.

**Response codes:** `204 No Content`, `404 Not Found`

```bash
curl -u admin:karnak -X DELETE http://localhost:8081/api/forward-nodes/1/destinations/3
```

---

## 5. Profiles

De-identification profiles define which DICOM tags to anonymize and how. They are uploaded as YAML files and then assigned to projects.

### `GET /api/profiles`

List all profiles (summary: id, name, version, minimumKarnakVersion, byDefault).

**Response codes:** `200 OK`, `204 No Content`

```bash
curl -u admin:karnak http://localhost:8081/api/profiles
```

**Response body (200):**
```json
[
  {
    "id": 1,
    "name": "Profile by Default",
    "version": "1.0",
    "minimumKarnakVersion": "0.9.7",
    "byDefault": true
  },
  {
    "id": 3,
    "name": "My Custom Profile",
    "version": "2.1",
    "minimumKarnakVersion": "1.0.0",
    "byDefault": false
  }
]
```

---

### `POST /api/profiles`

Upload a YAML profile file. The file is validated before saving.

**Content-Type:** `multipart/form-data`
**Form field:** `file` — the YAML file

**Response codes:**
- `201 Created` — profile saved
- `400 Bad Request` — unreadable file or invalid YAML syntax
- `422 Unprocessable Entity` — YAML parsed but profile element validation failed (body contains error list)

```bash
curl -u admin:karnak -X POST http://localhost:8081/api/profiles \
  -F "file=@my-deidentification-profile.yml"
```

**Response body (201):**
```json
{"id": 3, "name": "My Custom Profile"}
```

**Response body (422):**
```json
{
  "errors": [
    "Tag replacement: Cannot find the profile codename: action.on.specific.tags"
  ]
}
```

**Example YAML profile structure:**
```yaml
name: My Custom Profile
version: "1.0"
minimumKarnakVersion: "1.0.0"
profileElements:
  - name: "Remove patient name"
    codename: "action.on.specific.tags"
    action: "X"
    tags:
      - "(0010,0010)"
  - name: "Keep study date"
    codename: "action.on.specific.tags"
    action: "K"
    tags:
      - "(0008,0020)"
```

---

### `GET /api/profiles/{id}`

Get full profile details as JSON.

**Response codes:** `200 OK`, `404 Not Found`

```bash
curl -u admin:karnak http://localhost:8081/api/profiles/3
```

---

### `GET /api/profiles/{id}/download`

Download the profile as a YAML file (suitable for re-upload).

**Response codes:** `200 OK`, `404 Not Found`

```bash
# Save to file
curl -u admin:karnak http://localhost:8081/api/profiles/3/download -o my-profile.yml

# Print to stdout
curl -u admin:karnak http://localhost:8081/api/profiles/3/download
```

---

### `PUT /api/profiles/{id}`

Update profile metadata only (does not affect profile elements/masks).

**Request body (all fields optional):**
```json
{
  "name": "Renamed Profile",
  "version": "2.0",
  "minimumKarnakVersion": "1.1.0"
}
```

**Response codes:** `200 OK`, `404 Not Found`

```bash
curl -u admin:karnak -X PUT http://localhost:8081/api/profiles/3 \
  -H "Content-Type: application/json" \
  -d '{"name": "Renamed Profile", "version": "2.0"}'
```

---

### `DELETE /api/profiles/{id}`

Delete a profile. Will fail at DB level if the profile is still referenced by a project.

**Response codes:** `204 No Content`, `404 Not Found`

```bash
curl -u admin:karnak -X DELETE http://localhost:8081/api/profiles/3
```

---

## 6. Projects

Projects group a de-identification profile with an HMAC secret key. They are assigned to destinations to enable de-identification or tag morphing.

### `GET /api/projects`

List all projects.

**Response codes:** `200 OK`, `204 No Content`

```bash
curl -u admin:karnak http://localhost:8081/api/projects
```

**Response body (200):**
```json
[
  {
    "id": 1,
    "name": "Study Group A",
    "profileEntity": {
      "name": "My Custom Profile",
      "version": "1.0"
    },
    "secretEntities": [
      {
        "id": 2,
        "creationDate": "2024-06-01T10:30:00",
        "active": true
      }
    ]
  }
]
```

> The raw HMAC key bytes are **never** included in this response. Capture the
> key once at creation time from `POST /api/projects/{id}/secrets`.

---

### `POST /api/projects`

Create a new project.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | yes | Project name |
| `profileId` | long | no | ID of a profile to associate |

**Response codes:** `201 Created`, `400 Bad Request` (missing name), `404 Not Found` (profile not found)

```bash
# Project without a profile
curl -u admin:karnak -X POST http://localhost:8081/api/projects \
  -H "Content-Type: application/json" \
  -d '{"name": "Study Group A"}'

# Project with a profile
curl -u admin:karnak -X POST http://localhost:8081/api/projects \
  -H "Content-Type: application/json" \
  -d '{"name": "Study Group A", "profileId": 3}'
```

---

### `GET /api/projects/{id}`

Get a project by ID.

**Response codes:** `200 OK`, `404 Not Found`

```bash
curl -u admin:karnak http://localhost:8081/api/projects/1
```

---

### `PUT /api/projects/{id}`

Update a project. All fields optional; only provided fields are changed.

| Field | Type | Description |
|-------|------|-------------|
| `name` | string | New name |
| `profileId` | long or null | New profile ID; send `null` to detach the profile |

**Response codes:** `200 OK`, `404 Not Found`

```bash
# Rename and change profile
curl -u admin:karnak -X PUT http://localhost:8081/api/projects/1 \
  -H "Content-Type: application/json" \
  -d '{"name": "Study Group A v2", "profileId": 5}'

# Detach profile
curl -u admin:karnak -X PUT http://localhost:8081/api/projects/1 \
  -H "Content-Type: application/json" \
  -d '{"profileId": null}'
```

---

### `DELETE /api/projects/{id}`

Delete a project.

**Response codes:** `204 No Content`, `404 Not Found`

```bash
curl -u admin:karnak -X DELETE http://localhost:8081/api/projects/1
```

---

## 7. Project Secrets

Each project has an HMAC-SHA256 secret key used to pseudonymize patient
identifiers. Only one secret is active at a time; adding a new one deactivates
the previous.

> **The full hex key is only returned in the `POST /secrets` response.** It is
> never echoed back from `GET /api/projects/...`. Store the value safely on
> create — there is no read-back endpoint.

### `POST /api/projects/{id}/secrets`

Add or generate an HMAC secret for the project.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `hexKey` | string | no | Exactly 32 hex characters (16 bytes). Omit for auto-generation. Dashes are stripped automatically; any other separators or non-hex characters are rejected with `400`. |

**Response codes:** `201 Created`, `400 Bad Request` (invalid hex / wrong length), `404 Not Found`

```bash
# Auto-generate a random key
curl -u admin:karnak -X POST http://localhost:8081/api/projects/1/secrets \
  -H "Content-Type: application/json" -d '{}'

# Import a known key
curl -u admin:karnak -X POST http://localhost:8081/api/projects/1/secrets \
  -H "Content-Type: application/json" \
  -d '{"hexKey": "deadbeefdeadbeefdeadbeefdeadbeef"}'

# Import a key in display format (dashes stripped automatically)
curl -u admin:karnak -X POST http://localhost:8081/api/projects/1/secrets \
  -H "Content-Type: application/json" \
  -d '{"hexKey": "deadbeef-dead-beef-dead-beefdeadbeef"}'
```

**Response body (201):**
```json
{
  "projectId": 1,
  "hexKey": "deadbeefdeadbeefdeadbeefdeadbeef",
  "displayKey": "deadbeef-dead-beef-dead-beefdeadbeef",
  "active": true
}
```

---

## 8. External IDs

The External ID cache maps real patient identifiers to pseudonyms, enabling lookup-based de-identification. **Data is in-memory — it is lost on application restart.** It is scoped per project.

### `GET /api/projects/{id}/external-ids`

List all patient mappings for a project.

**Response codes:** `200 OK`, `404 Not Found`

```bash
curl -u admin:karnak http://localhost:8081/api/projects/1/external-ids
```

**Response body (200):**
```json
[
  {
    "pseudonym": "PSEUDO-001",
    "patientId": "PAT-12345",
    "patientFirstName": "John",
    "patientLastName": "Doe",
    "patientName": "Doe^John",
    "patientBirthDate": "1980-05-15",
    "patientSex": "M",
    "issuerOfPatientId": "HospitalA",
    "projectID": 1
  }
]
```

---

### `POST /api/projects/{id}/external-ids`

Add a single patient mapping.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `pseudonym` | string | yes | Anonymized identifier |
| `patientId` | string | yes | Real patient ID |
| `patientFirstName` | string | no | First name |
| `patientLastName` | string | no | Last name |
| `issuerOfPatientId` | string | no | Issuer of patient ID (used as part of cache key) |
| `patientBirthDate` | string | no | Format: `YYYY-MM-DD` |
| `patientSex` | string | no | `M`, `F`, or `O` |

**Response codes:** `201 Created`, `400 Bad Request` (missing pseudonym/patientId or malformed `patientBirthDate`), `404 Not Found`, `409 Conflict` (patient already exists)

```bash
curl -u admin:karnak -X POST http://localhost:8081/api/projects/1/external-ids \
  -H "Content-Type: application/json" \
  -d '{
    "pseudonym": "PSEUDO-001",
    "patientId": "PAT-12345",
    "patientFirstName": "John",
    "patientLastName": "Doe",
    "issuerOfPatientId": "HospitalA",
    "patientBirthDate": "1980-05-15",
    "patientSex": "M"
  }'
```

---

### `POST /api/projects/{id}/external-ids/import`

Bulk import patient mappings from a JSON array. Duplicate entries (same `patientId` + `issuerOfPatientId`) are skipped.

**Response codes:** `200 OK`, `404 Not Found`

```bash
curl -u admin:karnak -X POST http://localhost:8081/api/projects/1/external-ids/import \
  -H "Content-Type: application/json" \
  -d '[
    {
      "pseudonym": "P001",
      "patientId": "ID001",
      "patientFirstName": "Alice",
      "patientLastName": "Smith",
      "issuerOfPatientId": "HospitalA"
    },
    {
      "pseudonym": "P002",
      "patientId": "ID002",
      "patientFirstName": "Bob",
      "patientLastName": "Jones",
      "issuerOfPatientId": "HospitalA"
    }
  ]'
```

**Response body (200):**
```json
{"added": 2, "skipped": 0}
```

---

### `PUT /api/projects/{id}/external-ids/{patientId}`

Update an existing patient mapping. The original record is identified by `{patientId}` (path) and `issuerId` (query param). Only fields present in the body are updated.

| Query param | Default | Description |
|-------------|---------|-------------|
| `issuerId` | `""` | Issuer of patient ID (part of the cache lookup key) |

**Response codes:** `200 OK`, `400 Bad Request` (malformed `patientBirthDate`), `404 Not Found`

```bash
curl -u admin:karnak \
  -X PUT "http://localhost:8081/api/projects/1/external-ids/PAT-12345?issuerId=HospitalA" \
  -H "Content-Type: application/json" \
  -d '{"patientFirstName": "Jonathan", "patientSex": "M"}'
```

---

### `DELETE /api/projects/{id}/external-ids/{patientId}`

Delete a single patient mapping.

| Query param | Default | Description |
|-------------|---------|-------------|
| `issuerId` | `""` | Issuer of patient ID |

**Response codes:** `204 No Content`, `404 Not Found`

```bash
curl -u admin:karnak \
  -X DELETE "http://localhost:8081/api/projects/1/external-ids/PAT-12345?issuerId=HospitalA"
```

---

### `DELETE /api/projects/{id}/external-ids`

Delete **all** patient mappings for a project.

**Response codes:** `204 No Content`, `404 Not Found`

```bash
curl -u admin:karnak -X DELETE http://localhost:8081/api/projects/1/external-ids
```

---

## 9. Auth Configs

OAuth2 authentication configurations are used by STOW-RS destinations to obtain bearer tokens automatically. Sensitive fields (`clientId`, `clientSecret`, `accessTokenUrl`, `scope`) are encrypted at rest in the database.

### `GET /api/auth-configs`

List all auth configurations.

**Response codes:** `200 OK`, `204 No Content`

```bash
curl -u admin:karnak http://localhost:8081/api/auth-configs
```

**Response body (200):**
```json
[
  {
    "code": "keycloak-prod",
    "clientId": "karnak-client",
    "accessTokenUrl": "https://auth.example.com/realms/hospital/protocol/openid-connect/token",
    "scope": "openid",
    "authConfigType": "OAUTH2"
  }
]
```

> `clientSecret` is **never** returned in responses (write-only). It can only be
> set via `POST` or `PUT` and is encrypted at rest. There is no read-back of an
> existing secret.

---

### `POST /api/auth-configs`

Create a new auth configuration.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `code` | string | yes | Unique identifier (referenced in destinations) |
| `clientId` | string | no | OAuth2 client ID |
| `clientSecret` | string | no | OAuth2 client secret (write-only; never returned by GET) |
| `accessTokenUrl` | string | no | Token endpoint URL |
| `scope` | string | no | OAuth2 scope |
| `authConfigType` | string | no | Auth type — only `"OAUTH2"` supported (default) |

**Response codes:** `201 Created`, `400 Bad Request` (missing `code`), `409 Conflict` (code already exists)

```bash
curl -u admin:karnak -X POST http://localhost:8081/api/auth-configs \
  -H "Content-Type: application/json" \
  -d '{
    "code": "keycloak-prod",
    "clientId": "karnak-client",
    "clientSecret": "s3cr3t-client-p@ssword",
    "accessTokenUrl": "https://auth.example.com/realms/hospital/protocol/openid-connect/token",
    "scope": "openid"
  }'
```

---

### `GET /api/auth-configs/{code}`

Get an auth config by its code identifier.

**Response codes:** `200 OK`, `404 Not Found`

```bash
curl -u admin:karnak http://localhost:8081/api/auth-configs/keycloak-prod
```

---

### `PUT /api/auth-configs/{code}`

Update an existing auth config. Only provided fields are changed.

**Response codes:** `200 OK`, `404 Not Found`

```bash
curl -u admin:karnak -X PUT http://localhost:8081/api/auth-configs/keycloak-prod \
  -H "Content-Type: application/json" \
  -d '{
    "clientSecret": "new-s3cr3t",
    "scope": "openid profile"
  }'
```

---

### `DELETE /api/auth-configs/{code}`

Delete an auth config.

**Response codes:** `204 No Content`, `404 Not Found`

```bash
curl -u admin:karnak -X DELETE http://localhost:8081/api/auth-configs/keycloak-prod
```

---

## 10. Monitoring

Query, count, and export aggregated DICOM transfer status records logged by Karnak. Each
record is one row per (forward node, destination, series) — counters (`instances`,
`retries`, `sent`, `errors`, `excluded`) accumulate as instances are transferred, rather
than one row per individual transfer event.

### `GET /api/monitoring/transfers`

Query transfer records with optional filters and pagination.

| Query param | Type | Default | Description |
|-------------|------|---------|-------------|
| `studyUid` | string | — | Filter by Study Instance UID (partial match) |
| `serieUid` | string | — | Filter by Series Instance UID (partial match) |
| `status` | string | `ALL` | One of: `ALL`, `SENT`, `NOT_SENT`, `EXCLUDED`, `ERROR` |
| `start` | ISO datetime | — | From date-time (e.g. `2024-01-01T00:00:00`), applies to the series' last activity |
| `end` | ISO datetime | — | To date-time (e.g. `2024-12-31T23:59:59`), applies to the series' last activity |
| `page` | integer | `0` | Page number (0-based). Negative values are clamped to `0`. |
| `size` | integer | `50` | Records per page (1–1000). Out-of-range values are clamped. |

**Response codes:** `200 OK`

```bash
# All records, page 0
curl -u admin:karnak http://localhost:8081/api/monitoring/transfers

# Filter by status
curl -u admin:karnak \
  "http://localhost:8081/api/monitoring/transfers?status=ERROR"

# Filter by date range
curl -u admin:karnak \
  "http://localhost:8081/api/monitoring/transfers?start=2024-06-01T00:00:00&end=2024-06-30T23:59:59&size=100"

# Filter by Study UID
curl -u admin:karnak \
  "http://localhost:8081/api/monitoring/transfers?studyUid=1.2.840.10008"
```

**Response body (200):**
```json
{
  "content": [
    {
      "forwardNodeId": 1,
      "destinationId": 3,
      "studyUidOriginal": "1.2.840.10008.5.1.4.1.1.4",
      "serieUidOriginal": "...",
      "instances": 42,
      "retries": 0,
      "sent": 42,
      "errors": 0,
      "excluded": 0,
      "firstSeen": "2024-06-15T14:30:00",
      "lastSeen": "2024-06-15T14:32:00"
    }
  ],
  "totalElements": 1543,
  "totalPages": 31,
  "page": 0,
  "size": 50
}
```

---

### `GET /api/monitoring/transfers/count`

Count transfer records matching the filter. Accepts the same filter params as the query endpoint (except `page` and `size`).

**Response codes:** `200 OK`

```bash
# Count all errors
curl -u admin:karnak \
  "http://localhost:8081/api/monitoring/transfers/count?status=ERROR"
```

**Response body (200):**
```json
{"count": 42}
```

---

### `GET /api/monitoring/transfers/export`

Download transfer records as a CSV file. Accepts the same filter params as the query endpoint, plus:

| Query param | Default | Description |
|-------------|---------|-------------|
| `delimiter` | `,` | CSV column separator character |

**Response codes:** `200 OK`, `500 Internal Server Error`

```bash
# Export all records as CSV
curl -u admin:karnak \
  "http://localhost:8081/api/monitoring/transfers/export" \
  -o monitoring.csv

# Export errors only, semicolon delimiter
curl -u admin:karnak \
  "http://localhost:8081/api/monitoring/transfers/export?status=ERROR&delimiter=;" \
  -o errors.csv

# Export a specific date range
curl -u admin:karnak \
  "http://localhost:8081/api/monitoring/transfers/export?start=2024-06-01T00:00:00&end=2024-06-30T23:59:59" \
  -o june-2024.csv
```

---

## 11. End-to-end example

This script sets up a complete Karnak configuration programmatically — no UI needed.

```bash
#!/bin/bash
BASE="http://localhost:8081"
CREDS="admin:karnak"

echo "=== 1. Upload a de-identification profile ==="
PROFILE_ID=$(curl -s -u $CREDS -X POST $BASE/api/profiles \
  -F "file=@deidentification.yml" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
echo "Profile ID: $PROFILE_ID"

echo "=== 2. Create a project and link the profile ==="
PROJECT=$(curl -s -u $CREDS -X POST $BASE/api/projects \
  -H "Content-Type: application/json" \
  -d "{\"name\": \"Clinical Trial 2024\", \"profileId\": $PROFILE_ID}")
PROJECT_ID=$(echo $PROJECT | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
echo "Project ID: $PROJECT_ID"

echo "=== 3. Generate an HMAC secret for the project ==="
SECRET=$(curl -s -u $CREDS -X POST $BASE/api/projects/$PROJECT_ID/secrets \
  -H "Content-Type: application/json" -d '{}')
echo "Secret: $(echo $SECRET | python3 -c "import sys,json; print(json.load(sys.stdin)['displayKey'])")"

echo "=== 4. Create an OAuth2 auth config for the STOW destination ==="
curl -s -u $CREDS -X POST $BASE/api/auth-configs \
  -H "Content-Type: application/json" \
  -d '{
    "code": "keycloak-prod",
    "clientId": "karnak-client",
    "clientSecret": "s3cr3t",
    "accessTokenUrl": "https://auth.example.com/token",
    "scope": "openid"
  }' > /dev/null

echo "=== 5. Create a forward node (gateway entry point) ==="
NODE=$(curl -s -u $CREDS -X POST $BASE/api/forward-nodes \
  -H "Content-Type: application/json" \
  -d '{"fwdAeTitle": "GATEWAY1", "fwdDescription": "Clinical trial gateway"}')
NODE_ID=$(echo $NODE | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
echo "Forward node ID: $NODE_ID"

echo "=== 6. Restrict accepted sources (optional) ==="
curl -s -u $CREDS -X POST $BASE/api/forward-nodes/$NODE_ID/source-nodes \
  -H "Content-Type: application/json" \
  -d '{
    "aeTitle": "MODALITY1",
    "hostname": "10.0.0.5",
    "checkHostname": true,
    "description": "MRI scanner"
  }' > /dev/null

echo "=== 7. Add a DICOM destination with de-identification ==="
curl -s -u $CREDS -X POST $BASE/api/forward-nodes/$NODE_ID/destinations \
  -H "Content-Type: application/json" \
  -d "{
    \"destinationType\": \"dicom\",
    \"description\": \"Anonymized archive\",
    \"aeTitle\": \"PACS_ANON\",
    \"hostname\": \"192.168.1.50\",
    \"port\": 11112,
    \"activate\": true,
    \"desidentification\": true,
    \"deIdentificationProject\": {\"id\": $PROJECT_ID},
    \"pseudonymType\": \"CACHE_EXTID\"
  }" > /dev/null

echo "=== 8. Add a STOW-RS destination with OAuth2 ==="
curl -s -u $CREDS -X POST $BASE/api/forward-nodes/$NODE_ID/destinations \
  -H "Content-Type: application/json" \
  -d '{
    "destinationType": "stow",
    "description": "Cloud DICOMWeb archive",
    "url": "https://dicomweb.example.com/wado/rs/studies",
    "activate": true,
    "authConfig": "keycloak-prod"
  }' > /dev/null

echo "=== 9. Pre-load external ID mappings ==="
curl -s -u $CREDS -X POST $BASE/api/projects/$PROJECT_ID/external-ids/import \
  -H "Content-Type: application/json" \
  -d '[
    {"pseudonym":"P001","patientId":"ID001","patientFirstName":"Alice","patientLastName":"Smith","issuerOfPatientId":"HospitalA"},
    {"pseudonym":"P002","patientId":"ID002","patientFirstName":"Bob","patientLastName":"Jones","issuerOfPatientId":"HospitalA"}
  ]' | python3 -c "import sys,json; d=json.load(sys.stdin); print(f'Imported: {d[\"added\"]} added, {d[\"skipped\"]} skipped')"

echo "=== 10. Check echo status ==="
curl -s "http://localhost:8081/api/echo/destinations?srcAet=GATEWAY1"

echo ""
echo "=== Setup complete ==="
echo "Forward node ID : $NODE_ID  (AET: GATEWAY1)"
echo "Project ID      : $PROJECT_ID"
echo "Profile ID      : $PROFILE_ID"
```
