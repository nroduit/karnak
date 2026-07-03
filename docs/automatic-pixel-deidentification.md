# Automatic Pixel Data De-identification

Karnak can automatically detect and mask sensitive patient data (PHI) that is
*burned into* the DICOM pixel data (text overlays such as patient name,
accession number, dates, etc.). Instead of defining mask rectangles by hand per
station name, Karnak sends each image to an external **de-identification image
API** that runs OCR, detects sensitive text, and returns the mask areas to apply.

This feature is an option of the **Clean Pixel Data** profile element.

---

## How it works

1. A destination uses a de-identification profile that contains a **Clean Pixel
   Data** element with automatic mask generation enabled.
2. For every eligible instance (US, Secondary Capture, XC SOP classes, or any
   image flagged `BurnedInAnnotation`), Karnak extracts the pixel data and a set
   of sensitive tag values and sends them to the external API.
3. The API returns one or more mask areas (rectangles, each with a color).
   Karnak draws them on the image before forwarding it to the destination.
4. If the API detects no sensitive data, no mask is applied and the image is
   forwarded unchanged.

### Sensitive tags sent to the API

The values of the following tags are sent so the API can detect them in the
image. No pixel data leaves Karnak beyond the single frame being analysed.

`AccessionNumber`, `InstitutionName`, `OperatorsName`, `PatientAge`,
`PatientBirthDate`, `PatientID`, `PatientName`, `PatientSex`,
`PerformingPhysicianName`, `PerformedProcedureStepID`,
`ReferringPhysicianName`, `StudyDate`.

### Fail-closed behavior

When automatic mask generation is enabled and the API is **unreachable or
returns an error**, Karnak produces **no mask**, and the instance is **not
forwarded**. This prevents un-masked PHI from leaking to the destination if the
service is down. Check the Karnak logs for the cause (client error, server
error, or unreachable service).

> Note: when automatic mask generation is enabled, the statically configured
> station-name masks are **not** used. Manual masks only apply when automatic
> generation is disabled.

---

## Enabling the option in a profile

### Via the UI

1. Open the profile editor and add (or edit) a **Clean Pixel Data** element.
2. Enable **Automatic masks generation**.
3. Make sure the profile also contains the **DICOM basic profile** element.
4. Save and assign the profile to the destination's de-identification project.

### Via a YAML profile

Set the `automaticMasksGeneration` argument to `"true"` on the
`clean.pixel.data` element:

```yaml
name: "Automatic Deidentification Karnak Profile"
version: "1.0"
minimumKarnakVersion: "0.9.2"
profileElements:
- name: "Clean pixel data"
  codename: "clean.pixel.data"
  arguments:
    automaticMasksGeneration: "true"
- name: "DICOM basic profile"
  codename: "basic.dicom.profile"
```

Reference profiles are provided under
[`profils/`](../profils): `Automatic-Deidentification-Karnak-Profile.yml` and
`Automatic-Deidentification-No-Mask-Karnak-Profile.yml`.

---

## Configuring the external API endpoint

Karnak calls the de-identification image API at the URL configured by the
`DEIDENTIFY_IMAGE_URL` environment variable (default
`http://localhost:8000`). It maps to the `karnak.deidentify-image.url` property
in `application.yml`:

```yaml
karnak:
  deidentify-image:
    url: ${DEIDENTIFY_IMAGE_URL:http://localhost:8000}
```

| Variable | Default | Description |
|----------|---------|-------------|
| `DEIDENTIFY_IMAGE_URL` | `http://localhost:8000` | Base URL of the de-identification image API. |

The API contract is documented in
[`src/main/resources/deidentification-api.yaml`](../src/main/resources/deidentification-api.yaml).

---

## Deploying the external de-identification service

The de-identification image API is a separate service. Deploy and run it so that
it is reachable from Karnak at `DEIDENTIFY_IMAGE_URL`.

> Deployment guide of the de-identification image API: <https://github.com/nroduit/image-ocr-identifier>

### Standard deployment

1. Deploy the de-identification image API (see the repository linked above).
2. Point Karnak to it by setting `DEIDENTIFY_IMAGE_URL` to the service base URL.
3. Restart Karnak so the new configuration is picked up.

### Portable build

The portable package can start the de-identification service as a **sidecar**
alongside Karnak. It is controlled in `run.cfg`:

```sh
### Image Deidentification
DEIDENTIFY_IMAGE_ENABLED=true
DEIDENTIFY_IMAGE_URL=http://localhost:8000
```

| Variable | Default | Description |
|----------|---------|-------------|
| `DEIDENTIFY_IMAGE_ENABLED` | `true` | Start the bundled de-identification sidecar with the portable package. Set to `false` to manage the service yourself. |
| `DEIDENTIFY_IMAGE_URL` | `http://localhost:8000` | Base URL Karnak uses to reach the service. |

When enabled, `run.sh` starts the bundled binary
(`deidentify-karnak/deidentify-karnak`) on launch and stops it on shutdown. If
the binary is missing, the step is skipped and a message is logged.

---

## Troubleshooting

| Symptom | Likely cause | Action |
|---------|--------------|--------|
| Images with automatic masking are not forwarded | API unreachable or returning errors | Check the API is running and reachable at `DEIDENTIFY_IMAGE_URL`; inspect Karnak logs. |
| `Cannot reach de-identification image API ...` in logs | Wrong URL or service down | Verify `DEIDENTIFY_IMAGE_URL` and service health. |
| `SOP Instance UID ... does not match` in logs | API returned a response for a different instance | Verify the API version and that it echoes back the request `sop_instance_uid`. |
| No mask applied although PHI is visible | API detected no sensitive text, or the tag values are absent from the metadata | Confirm the sensitive tags are populated in the source metadata. |

