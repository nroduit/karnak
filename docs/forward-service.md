# ForwardService — Developer & Evolution Guide

> Scope: `src/main/java/org/karnak/backend/service/ForwardService.java`
> Audience: developers maintaining or extending Karnak's DICOM forwarding core.

`ForwardService` is the heart of Karnak's forwarding mechanism. It takes one
incoming DICOM object (already received by the C-STORE SCP listener) and pushes
it to **every** configured destination of a forward node, applying each
destination's own editor pipeline (de-identification, tag morphing, masking,
defacing, transfer-syntax adaptation) along the way. It is one of the most
complex classes in the codebase because it multiplexes over two transport
protocols (DICOM C-STORE and DICOMWeb STOW-RS), two transfer paths (first vs.
subsequent destinations), and a thicket of error/abort/monitoring semantics.

---

## 1. Where it sits in the flow

```
Source PACS / modality
        │  C-STORE
        ▼
CStoreSCPService.store()                 (service/CStoreSCPService.java:~87-126)
        │  builds Params p, resolves ForwardDicomNode + List<ForwardDestination>
        ▼
ForwardService.storeMultipleDestination(fwdNode, destinations, p)   ← ENTRY POINT
        │  loop over destinations
        ▼
prepareAndTransfer(...)   ──► transfer(...) / transferOther(...)
        │
        ├─ DICOM  ──► StoreFromStreamSCU.cstore(...)
        └─ Web    ──► DicomStowRS.uploadDicom / uploadPayload(...)
        │
        ▼
progressNotify(...) + monitor(...)  ──► TransferMonitoringEvent (Spring event)
```

- **Caller:** `CStoreSCPService.store()` is the sole production caller of
  `storeMultipleDestination`. The destination list and `ForwardDicomNode` come
  from the in-memory config maintained by `GatewaySetUpService` (reloaded every
  5 s).
- **Editors** (`destination.getDicomEditors()`) are assembled per-destination in
  `GatewaySetUpService` (~lines 186-231): Condition → Filter → SwitchingAlbum
  (KHEOPS) → StreamRegistry → DeIdentify → TagMorphing.

---

## 2. Key collaborators

| Type | Role |
|------|------|
| `Params` (record) | `iuid, cuid, tsuid, priority, data(InputStream), as(Association)`. The incoming object's identity + raw stream. |
| `ForwardDicomNode` | The source/gateway node (AE title, id, accepted source nodes). |
| `ForwardDestination` (abstract) | Base for a destination: `getDicomEditors()`, `getOutputTransferSyntax(orig)`, `getState()`, `getId()`. |
| `DicomForwardDestination` | DICOM transport: `getStreamSCU()` (`StoreFromStreamSCU`), `getStreamSCUService()`. |
| `WebForwardDestination` | DICOMWeb transport: `getStowrsSingleFile()` (`DicomStowRS`). |
| `AttributeEditor` / `AttributeEditorContext` | Weasis pipeline contract. Editors mutate `Attributes` and may set `context.getAbort()`. |
| `Abort` enum | `FILE_EXCEPTION` (skip this object) vs `CONNECTION_EXCEPTION` (abort the whole association). |
| `TransformedPlanarImage` | Holds the editable + realized `PlanarImage` for masking/defacing; must be released. |
| `TransferMonitoringEvent` / `TransferStatusEntity` | Per-object audit record (original vs. sent attributes, sent/error flags, reason, modality). |

---

## 3. The four transfer methods (mental model)

The class has a **2×2 matrix** of transfer logic. Understanding this matrix is
the single most important thing for working in this file.

|                | First destination (`index == 0`) | Other destinations (`index > 0`) |
|----------------|-----------------------------------|-----------------------------------|
| **DICOM**      | `transfer(fwd, DicomForwardDestination, copy, p)`  L156 | `transferOther(fwd, DicomForwardDestination, copy, p)` L319 |
| **DICOMWeb**   | `transfer(fwd, WebForwardDestination, copy, p)`    L398 | `transferOther(fwd, WebForwardDestination, copy, p)` L508 |

### Why two paths?

`Params.data()` is a **single-pass `InputStream`** (a `PDVInputStream` straight
off the association). It can only be read **once**. So:

- **First destination** (`transfer`): reads the stream, parses `Attributes`. If
  there are ≥2 destinations, it copies the parsed `Attributes` into the shared
  `attributes` buffer (`copy`) so later destinations can reuse them.
- **Other destinations** (`transferOther`): never touch the stream; they work
  from the in-memory `Attributes` copy and re-apply *their own* editors to a
  fresh `new Attributes(copy)` clone.

`prepareAndTransfer` (L116) orchestrates this: index 0 takes the stream path and
populates `attributes`; subsequent indices reuse `attributes` (only if non-empty).
Note `copy.addAll(attributes)` runs *before* the first destination's editors and
abort check, so the shared buffer is populated even if that destination aborts —
the buffer is only empty if `readDataset` itself fails (a genuine read error that
correctly fails the whole object).

### Fast path vs. editor path

Inside each method there is an optimization branch:

- **Fast path** — taken when there are no editors **and** the requested transfer
  syntax already matches the source TS (and, for `transfer`, `copy == null`).
  The object is streamed through largely untouched (`InputStreamDataWriter` /
  `DataWriterAdapter` / raw `stow.uploadDicom(stream, fmi)`).
- **Editor path** — parse `Attributes`, run every editor, check abort, transcode
  the pixel data if masking/defacing/TS change is required, then send.

---

## 4. Transfer-syntax & image transformation

- `AdaptTransferSyntax(requested, suitable)` reconciles the source TS, the
  destination's `getOutputTransferSyntax`, and what the negotiated association
  actually supports (`streamSCU.selectTransferSyntax`).
- `transformImage()` only produces a `TransformedPlanarImage` when a `MaskArea`
  is present or `Defacer.APPLY_DEFACING` is set in the context properties.
  `buildPlanarImage` applies defacing first, then mask drawing.
- `ImageAdapter.imageTranscode(...)` returns a `BytesWithImageDescriptor`; if
  `null` for the web path, the dataset is uploaded as-is, otherwise the
  transcoded/transformed payload is uploaded.
- **Resource discipline:** every realized `PlanarImage` must be `release()`d.
  Done in `launchCStore`'s finally and `uploadPayLoadFromTransformedImage`'s
  finally. `transformImage` is a pure in-place mutator returning `boolean`; the
  caller that owns the `TransformedPlanarImage` is responsible for releasing it.

---

## 5. Error, abort & monitoring semantics

Each transfer method wraps its body in the same cascade of catch blocks:

1. `AbortException` → notify progress as failed, monitor with `error=false`
   (it's a deliberate skip, not a system error). **Re-throws only when
   `CONNECTION_EXCEPTION`** (so the association tears down); `FILE_EXCEPTION` is
   swallowed so other objects keep flowing.
2. `IOException` → monitor with `error=true`, **re-throw** (connection problem
   bubbles up to `storeMultipleDestination`, which logs & rethrows).
3. `Exception` (catch-all) → restore interrupt flag if `InterruptedException`,
   monitor `error=true`, **log but do not rethrow**.
4. `finally` → `streamSCU.triggerCloseExecutor()` (DICOM) and/or
   `cleanOrGetBulkDataFiles(in, copy == null)`.

`monitor(...)` publishes a `TransferMonitoringEvent` capturing original vs.
to-send attributes for the audit trail (the monitoring UI / `transfer_status`
table). `progressNotify` updates the per-destination `DicomState` progress.

**Bulk-data temp files:** when editors run, `DicomInputStream` is configured
with `IncludeBulkData.URI`, spilling pixel data to temp files. The first
destination *returns* those files (so later destinations can read them); they're
deleted in `storeMultipleDestination`'s finally. Subsequent destinations clean
their own. Getting `clean == copy == null` right is critical: `copy != null`
(multi-destination first object) keeps the temp files alive for later
destinations; `copy == null` deletes them immediately.

---

## 6. Open risks

Only unresolved items are tracked here; risks that were fixed or verified as
false positives have been removed (see git history for the audit). Status
legend: ⏳ open (deferred) · 🔧 operational.

- **R4 — ⏳ Deferred: unify the four transfer-method bodies.** The concrete bug
  (web `transferOther` not monitoring the fast path) and the 4×-duplicated abort
  block (now `abortIfRequested(...)`) are done. What remains is collapsing the
  ~80% structural duplication across the four methods behind one
  transport-strategy template. Two blockers make a big-bang rewrite unsafe right
  now: (a) `attributesToSend` is *reassigned* mid-method (not just mutated), so a
  captured-reference template would not see the parsed/cloned dataset without a
  mutable holder; (b) the DICOM first-destination editor path and all
  pixel/mask/deface transcoding remain untested (no pixel-data fixtures yet). Add
  those fixtures + tests (Section 7 items 1 & 4) before attempting it.

- **R7 — 🔧 Operational, by design.** The catch-all `Exception` branches log and
  continue so one bad object/editor doesn't stall the gateway. This is
  intentional resilience and was left as-is. Every such path already publishes a
  monitoring event with `error=true`; ensure dashboards/alerting watch
  `error=true` volume so swallowed failures stay visible.

---

## 7. Testing strategy

Existing collaborators' tests (per `src/test/java/org/karnak/backend/dicom/`):

- `ForwardUtilTest` — `storeMultipleDestination` guard paths (null destinations →
  `IllegalStateException`, DICOMDIR exclusion, unreachable remote → `IOException`).
- `ForwardDestinationTest`, `WebForwardDestinationTest` — output-transfer-syntax
  logic and destination accessors.
- `TransferStatusEntityTest` — the monitoring entity built by `monitor(...)`.

**`ForwardServiceTest`** (`src/test/java/org/karnak/backend/service/`, 13 tests)
added to lock the control flow this guide describes. Approach: mock
`StoreFromStreamSCU` / `DicomStowRS` (both non-final) + capture the published
`TransferMonitoringEvent`; build real `Attributes` in memory and, where the
stream is parsed, serialize with `DicomOutputStream` (FMI for the fast path, raw
dataset for the editor path). Covers:

- ✅ `selectTransferSyntax` — requested / ExplicitVRLE / ImplicitVRLE fallbacks.
- ✅ **Abort handling** — DICOM `transferOther` `FILE_EXCEPTION` is swallowed
  (not forwarded, monitored `sent=false,error=false`, reason captured);
  `CONNECTION_EXCEPTION` rethrows; not-ready association → `error=true`, no throw.
  Web `transferOther` `FILE_EXCEPTION` swallowed. (gap item 2)
- ✅ **Fast path vs. editor path** — DICOM/web `transferOther` both paths forward
  and monitor success. (gap item 1, transferOther flavors)
- ✅ **Monitoring** — web `transferOther` fast path monitors success (R4
  regression); HTTP 409 treated as already-present success.
- ✅ **Stream parsing** — web `transfer` (first dest) fast path uploads the stream.
- ✅ **Fan-out** — `storeMultipleDestination` to two destinations: first parses &
  populates the copy, second reuses it, both monitored `sent=true`. (gap item 3,
  partial)

**Remaining gaps** (need a real DICOM fixture *with pixel data* + OpenCV native):

1. DICOM `transfer` (first destination) editor path with a real `cstore`.
2. **PlanarImage release** on the masking/defacing path — verify the realized
   image is released (the `transformImage` ownership contract).
3. Bulk temp-file lifecycle assertion — files returned by the first destination
   are deleted exactly once after all destinations have read them.

Real DICOM fixtures + OpenCV native lib are required for image paths — the Maven
`-Djava.library.path` wiring handles this (run via `mvn test`, not bare IntelliJ
unless the lib path is set).

---

## 8. Guidelines for evolving this class

- **Preserve the single-read invariant on `Params.data()`.** Any change must keep
  exactly one consumer of the stream (the first destination); everything else
  works off the `Attributes` copy.
- **Keep abort semantics intact:** `FILE_EXCEPTION` = skip this object only;
  `CONNECTION_EXCEPTION` = propagate and tear down. Don't broaden the rethrow.
- **Every exit path must `monitor(...)` once and `progressNotify(...)` once.** When
  adding a branch, wire both, matching `sent`/`error` to reality.
- **Release every realized `PlanarImage`.** Image transforms allocate native
  memory; leaks here are not GC-visible.
- **Before adding a 5th variation, extract the shared template (R4).** The matrix
  is already at its complexity ceiling; new transport features should plug into a
  common skeleton, not copy a fifth method.
- **Schema/monitoring fields** flow through `TransferStatusEntity` — add new audit
  fields there + a Liquibase changeset, never via Hibernate ddl.

---

## 9. Quick reference — line map

| Concern | Location |
|---------|----------|
| Entry point / fan-out loop | `storeMultipleDestination` L74, `prepareAndTransfer` L114 |
| DICOM prepare | `prepareTransfer` L145 |
| DICOM first / other | `transfer` L154 / `transferOther` L314 |
| Web first / other | `transfer` L387 / `transferOther` L485 |
| C-STORE + image release | `launchCStore` L236 |
| Transcode + transform | `buildDataWriterFromTransformedImage` L249, `transformImage` L268 (returns `boolean`) |
| Web payload upload | `uploadPayLoadFromTransformedImage` L468 |
| Bulk-file cleanup | `cleanOrGetBulkDataFiles` L301 |
| Abort handling (shared) | `abortIfRequested` L582 |
| Progress notify | `progressNotify` L555 / L563 |
| TS selection | `selectTransferSyntax` L599 |
| Monitoring event | `monitor` L623 |