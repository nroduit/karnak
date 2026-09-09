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

### Color and pixel encoding handling

Some encodings need to be normalized before the image is sent to the API:

* **Multi-frame instances.** Only the first frame is sent: uncompressed pixel
  data holds every frame back to back, while the API sizes its buffer for a
  single frame and rejects anything bigger.
* **`PALETTE COLOR` without a usable LUT.** A palette is only sent along when its
  descriptors and its data are consistent with each other; otherwise the image is
  declared as `MONOCHROME2`. The pixel values are single-channel indexes, so text
  detection is unaffected, and the API never receives a palette declaration it
  cannot honour.
* **`MONOCHROME1` instances.** The polarity is passed as a dedicated flag, which
  is what the API reads to invert raw pixel data before running the detection.

Masking a `PALETTE COLOR` instance also changes what is **forwarded**. Applying a
mask requires decoding the image, and the codec decodes a palette image to 8-bit
RGB while keeping the sample depth declared by the source dataset: a 16-bit
palette instance would be stored as "3 samples of 16 bits" holding 8-bit samples,
which makes the receiver read several frames as one and shifts the colors. Karnak
therefore applies the Palette Color LUT itself and forwards a plain RGB instance,
its palette attributes (LUT descriptors and data) removed. Compressed palette
instances are left to the codec, which derives every tag from the decoded image.

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

A complete example is documented in the
[Karnak documentation](https://weasis.org/karnak-documentation/en/profiles/masks/#automatic-pixel-data-de-identification).

---

## Configuring the external API endpoint

Karnak calls the de-identification image API at the URL configured by the
`OCR_URL` environment variable (default
`http://localhost:8000`). It maps to the `ocr.url` property
in `application.yml`:

```yaml
ocr:
  url: ${OCR_URL:http://localhost:8000}
```

| Variable | Default | Description |
|----------|---------|-------------|
| `OCR_URL` | `http://localhost:8000` | Base URL of the de-identification image API. |

The API contract is documented in
[`src/main/resources/deidentification-api.yaml`](../src/main/resources/deidentification-api.yaml).

---

## Deploying the external de-identification service

The de-identification image API is a separate service. Deploy and run it so that
it is reachable from Karnak at `OCR_URL`.

> Deployment guide of the de-identification image API: <https://github.com/nroduit/image-ocr-identifier>

### Standard deployment

1. Deploy the de-identification image API (see the repository linked above).
2. Point Karnak to it by setting `OCR_URL` to the service base URL.
3. Restart Karnak so the new configuration is picked up.

### Portable build

The portable package does not bundle the service, but its launcher can download
it and then starts it as a **sidecar** alongside Karnak. On the first launch,
`run.sh` / `run.bat` proposes to fetch the prebuilt release from the
[image-ocr-identifier releases](https://github.com/nroduit/image-ocr-identifier/releases)
and installs it in the `image-ocr-identifier` folder next to the launcher.
Prebuilt archives exist for Windows x86_64, Linux x86_64 and macOS arm64; on any
other platform the launcher logs that no prebuilt service is available and
starts Karnak without it. Everything is controlled in `run.cfg`:

```sh
OCR_ENABLED=true
OCR_AUTO_INSTALL=ask
OCR_VERSION=v0.1.0
OCR_MODEL=PP-OCRv5_mobile
OCR_URL=http://localhost:8000
OCR_SERVICE_NAME=image-ocr-identifier
```

| Variable | Default | Description |
|----------|---------|-------------|
| `OCR_ENABLED` | `true` | Start the de-identification sidecar with the portable package. Set to `false` to manage the service yourself (and to stop being asked to download it). |
| `OCR_AUTO_INSTALL` | `ask` | What the launcher does when the service is missing: `ask` before downloading, `always` download without asking (useful when the script is not run from a terminal), `never` do nothing. |
| `OCR_VERSION` | *(pinned)* | Release of the service downloaded by the launcher. It is chosen to match the Karnak version; keep the value shipped in `run.cfg`. |
| `OCR_MODEL` | `PP-OCRv5_mobile` | OCR model bundled in the downloaded archive. |
| `OCR_URL` | `http://localhost:8000` | Base URL Karnak uses to reach the service. |
| `OCR_SERVICE_NAME` | `image-ocr-identifier` | Name of the service folder and executable. The sidecar lives in `<app>/<name>/` and its binary is named `<name>` (`<name>.exe` on Windows). |

When enabled, `run.sh` / `run.bat` starts the binary on launch; `run.sh` also
stops it when Karnak exits (on Windows the service runs in its own minimized
window). If the binary is missing and the download is declined or fails, the
step is skipped, a message is logged and Karnak starts without the service.

---

## Troubleshooting

| Symptom | Likely cause | Action |
|---------|--------------|--------|
| Images with automatic masking are not forwarded | API unreachable or returning errors | Check the API is running and reachable at `OCR_URL`; inspect Karnak logs. |
| `Cannot reach de-identification image API ...` in logs | Wrong URL or service down | Verify `OCR_URL` and service health. |
| `SOP Instance UID ... does not match` in logs | API returned a response for a different instance | Verify the API version and that it echoes back the request `sop_instance_uid`. |
| No mask applied although PHI is visible | API detected no sensitive text, or the tag values are absent from the metadata | Confirm the sensitive tags are populated in the source metadata. |

