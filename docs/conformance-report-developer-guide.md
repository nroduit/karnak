# DICOM Conformance Report — Developer Guide

This document describes the architecture of the DICOM conformance validation
feature, the responsibility of each class, the integration points in the
forwarding flow, and the remaining tasks to turn it into a complete,
production-grade conformance check.

It complements:

- [`conformance-report-user-guide.md`](conformance-report-user-guide.md) — end-user configuration and report reading.
- [`dicom-standard-json.md`](dicom-standard-json.md) — the vendored DICOM standard JSON and curated rules these checks are driven by.

---

## 1. Goal

For every DICOM instance forwarded to a destination that opted in, validate the
**de-identified data actually sent** against the DICOM standard (the IOD of its
SOP Class), accumulate the findings per study, and email an HTML report once the
study transfer goes idle.

Validation is driven by two data sources:

- The **innolitics DICOM standard JSON** (`StandardDICOM`, under
  `backend/model/standard` / `backend/model/dicominnolitics`) — IOD → modules →
  attributes with their DICOM Type, plus the data dictionary (VR, VM, retired).
- A small **curated supplement** (`CuratedValidationRules`) for things the
  standard JSON does not expose machine-readably: enumerated values, SOP Class ↔
  Modality coherence, retired transfer syntaxes.

---

## 2. End-to-end flow

```
C-STORE SCP (incoming)
   │
   ▼
ForwardService  (per destination, after de-identification editors applied)
   │  forwards instance via DICOM (StoreFromStreamSCU) or DICOMWeb (DicomStowRS)
   │
   ├─► TransferMonitoringEvent           (existing monitoring, unchanged)
   │
   └─► if destination.buildConformanceReport && SOP Class present:
          MetadataSnapshot.of(attributesToSend)   ← built SYNCHRONOUSLY
          publish ConformanceCollectEvent(InstanceConformanceData)
                         │
                         ▼  (Spring @Async @EventListener)
          ConformanceReportService.onConformanceCollect
                         │
                         │ validate one instance
                         ▼
          DicomConformanceValidator.validate(...) → InstanceValidationResult
                         │
                         │ accumulate into the study batch (keyed by StudyKey)
                         ▼
          StudyConformanceAccumulator  (one per study/destination/forward-node)

   ⏰ @Scheduled(60s) flushIdleStudies / @PreDestroy flushAll
                         │ idle (>300s) or expired (>14400s)
                         ▼
          StudyConformanceAccumulator.close()
                         │  runs StudyConsistencyChecker, builds immutable report
                         ▼
          ConformanceReport ──► Thymeleaf (conformanceReportEmail.html) ──► email
```

### The data life cycle: de-identify, tag-morph, or pass-through

The single most important thing to understand about this feature is **what
dataset gets validated**. The conformance check always runs on
`attributesToSend` — the exact `Attributes` object that left Karnak for that
destination, *after* every transformation — never on the dataset Karnak
received. The report therefore describes what the receiver actually got.

Crucially, the validator is **mode-agnostic**. "De-identification" and
"tag-morphing" are not two different code paths: both are just profile items
compiled into the same ordered chain of `AttributeEditor`s
(`destination.getDicomEditors()`). Whether a destination removes PHI, rewrites
tags, does both, or does neither, the conformance feature only ever sees the
dataset that comes out the far end. This is why the feature needs no awareness of
*which* profile is configured — it validates the result, not the recipe.

How `attributesToSend` is produced depends on the destination's configuration.
`ForwardService.transfer(...)` / `transferOther(...)` pick one of two branches
(see `ForwardService.java`):

1. **Pass-through (no transformation).** When the destination has **no editors**
   *and* the requested transfer syntax equals the source's (and, for the first
   destination, no shared `copy` is needed), Karnak takes a fast path: the
   original DICOM bytes are streamed straight to the destination
   (`InputStreamDataWriter` for C-STORE, `stow.uploadDicom(stream, fmi)` for
   STOW-RS) without ever materializing a mutable, edited `Attributes`. For
   conformance, `monitor()` still re-reads the dataset (`new
   DicomInputStream(p.data()).readDataset()`) so `attributesToSend` reflects the
   bytes sent. In this mode the report effectively audits the **source's own
   conformance** — any finding originates upstream, not from Karnak.

2. **Transformed (de-identify and/or tag-morph and/or transcode).** When the
   destination has editors, or the transfer syntax must change, Karnak reads the
   dataset into a mutable `Attributes`, applies each editor in order
   (`editors.forEach(e -> e.apply(attributes, context))`), and points
   `attributesToSend` at that mutated dataset. De-identification (emptying,
   removing, hashing PHI), tag-morphing (`AddTag`, `ReplaceApi`, UID rewriting,
   …), pixel masking and defacing, and transfer-syntax adaptation all happen
   here, in pipeline order. The report then reflects the **net result of the
   whole pipeline** — including any conformance regressions the pipeline itself
   introduced (e.g. a profile that empties a Type 1 attribute, or a tag-morph
   that writes a value violating its VR).

In both branches `monitor()` is the single integration point: it always fires
`TransferMonitoringEvent`, and additionally — when `buildConformanceReport` is on
and a SOP Class is present — snapshots `attributesToSend` and publishes
`ConformanceCollectEvent`. A source fanning out to N destinations is validated N
times independently: each destination has its own editor chain and therefore its
own `attributesToSend`, its own `StudyKey`, and its own report.

### Why the snapshot is built synchronously

`ForwardService.monitor()` builds the `MetadataSnapshot` **on the forwarding
thread, before returning**. This is deliberate: bulk-data values in
`attributesToSend` reference short-lived temporary files that are cleaned up
right after the transfer (see the `finally` blocks that delete the bulk files).
The snapshot prunes those bulk values (replacing them with empty values and
recording their tags) so the rest of the pipeline can run asynchronously without
touching invalid references. Only the lightweight metadata copy crosses the
async boundary into `@Async` validation.

---

## 3. Component reference

All validation classes live in `org.karnak.backend.model.validation`, the
orchestration in `org.karnak.backend.service`, the event in
`org.karnak.backend.model.event`.

### Data carriers (records)

| Class | Role |
|---|---|
| `MetadataSnapshot` | Metadata-only deep copy of the sent dataset; strips pixel/overlay/curve/waveform/encapsulated data, bulk-data URIs, `Fragments`, and binaries > 4 KiB. Copies sequences down to a configurable depth (`of(source)` → `DEFAULT_MAX_SEQUENCE_DEPTH` = 3; `of(source, maxSequenceDepth)` raised to the deep-validation depth). Records stripped top-level tags in `bulkPresentTags`. |
| `InstanceConformanceData` | Per-instance input of the pipeline: identifiers (study/series/SOP/instance UID, modality), transfer syntax, `sent`/`failureReason`, and the `MetadataSnapshot`. `studyKey()` derives the batch key. |
| `StudyKey` | Batch identity: `(forwardNodeId, destinationId, studyInstanceUid)`. |
| `InstanceValidationResult` | Findings for one validated instance. |
| `ConformanceFinding` | One finding: tag path, attribute/module name, module id, `Severity`, `CheckKind`, expected, found. **Record equality is the dedup key** across instances. |
| `ConformanceReport` | Immutable, render-ready aggregate for one study (summary, per-SOP-Class findings, consistency findings, counts, verdict). |
| `Severity` | `ERROR` / `WARNING` / `INFO` (+ render color). |
| `CheckKind` | The check category that produced a finding (+ label and color for the HTML). |
| `VmSpec` | Parsed DICOM VM spec (`1`, `1-n`, `2-2n`, `1-2`…) with `matches(count)`. |
| `CuratedValidationRules` | Loads `curated-validation-rules.json`: `enumeratedValues` (each an `EnumeratedRule` with `values` + a `closed` flag), `sopClassToModalities`, `retiredTransferSyntaxes`, `conditionalRequirements`. |

### Logic

| Class | Role |
|---|---|
| `DicomConformanceValidator` | **Single-instance validation.** Module presence + attribute Type (1/1C/2) against the IOD, then a dataset sweep for VR mismatch, VM violation, enumerated values, retired attributes, private-block structure and (optional, opt-in) value-content conformity. The sweep recurses sequences down to `maxSequenceDepth` levels (`DEFAULT_MAX_SEQUENCE_DEPTH` = 1; the deep-validation option raises it). Stateless; reusable across threads. `validate(...)` overloads: 3-arg, 4-arg (`checkValueConformity`), 5-arg (`checkValueConformity`, `maxSequenceDepth`). |
| `VrValueRules` | Per-VR value-content rules (PS3.5 §6.2): max length and format/character-repertoire for the bounded string VRs (`AE/CS/DS/IS/LO/SH/ST/LT/UI/DT/DA/TM/AS/PN`), plus `lengthOverflowIsError(vr)` classifying small/structured VRs (ERROR) vs long free-text VRs (WARNING). Used only by the optional value-conformity check. |
| `StudyConformanceAccumulator` | **Per-study aggregation.** Thread-safe (`synchronized`). Deduplicates findings per SOP Class with an occurrence count, keeps only lightweight per-series tuples (never datasets). When `deidentified` is false, the **Patient Name is not collected** (PHI redaction — gone from header and consistency checks). `close()` finalizes into a `ConformanceReport`. |
| `StudyConsistencyChecker` | **Study-level cross-checks** run at `close()`: single Study UID, single Patient identity, Frame of Reference uniformity per series, Modality/SOP-Class coherence, retired transfer syntax. |

### Orchestration

| Class | Role |
|---|---|
| `ConformanceReportService` | Spring `@Service`. `@Async @EventListener onConformanceCollect` validates + accumulates; `@Scheduled(60s) flushIdleStudies` and `@PreDestroy flushAll` close idle/expired batches; renders the Thymeleaf template and sends the email to `destination.getNotify()`. |
| `ConformanceCollectEvent` | `ApplicationEvent` wrapping `InstanceConformanceData`. |

### Resources

| Resource | Role |
|---|---|
| `templates/conformanceReportEmail.html` | Thymeleaf HTML email template (inline CSS for mail-client compatibility). Severity/kind/verdict badges use the `pill` fragment; the totals strip uses layout tables with filled `bgcolor` cells for the colour swatches, because Outlook (Word engine) drops margins and ignores width/height/padding on inline spans. |
| `templates/fragments/conformanceBadges.html` | Reusable `pill(text, bg, bold, fontSize, radius)` badge fragment: a one-cell table (`bgcolor` + cell padding) so the background and padding render in Outlook for Windows; `border-radius` rounds the corners where supported and degrades to a clean padded rectangle in Outlook. (A span-based badge collapses/loses padding in Outlook; VML rounding is unreliable for variable-width pills — both were tried and dropped.) |
| `org/karnak/backend/model/validation/curated-validation-rules.json` | Curated enumerated values / SOP↔modality / retired TS. |
| `db/changelog/changes/db.changelog-1.6.xml` | Adds `build_conformance_report`, `conformance_report_notify`, `check_value_conformity` and `deep_sequence_validation` columns to `destination`. |

---

## 4. Integration points (the diff to the existing code)

| File | Change |
|---|---|
| `DestinationEntity` | New `boolean buildConformanceReport`, `boolean checkValueConformity`, `boolean deepSequenceValidation` and `String conformanceReportNotify` fields + getters/setters (persisted columns). |
| `db.changelog-1.6.xml` | `1.6-1` adds `build_conformance_report` (BOOLEAN, not null, default false); `1.6-2` adds `conformance_report_notify` (VARCHAR(255), nullable); `1.6-3` adds `check_value_conformity` (BOOLEAN, not null, default false); `1.6-4` adds `deep_sequence_validation` (BOOLEAN, not null, default false). Auto-discovered: `db.changelog-master.yaml` uses `includeAll` over `db/changelog/changes/`. |
| `ForwardDestination` | New `buildConformanceReport`, `checkValueConformity` and `deepSequenceValidation` flags (Lombok `@Setter`) carried into the in-memory forward model. |
| `GatewaySetUpService` | Copies `dstNode.isBuildConformanceReport()`, `isCheckValueConformity()`, `isDeepSequenceValidation()` and `isDesidentification()` onto both `WebForwardDestination` and `DicomForwardDestination`. |
| `ForwardService` | `monitor(...)` takes the `ForwardDicomNode` + `ForwardDestination` (and the sent transfer syntax) and, when the report flag is on, builds the `MetadataSnapshot` (to `conformance-report.max-sequence-depth` when deep validation is on, else the default depth) and publishes `ConformanceCollectEvent` carrying `checkValueConformity` and `deepSequenceValidation`. |
| `InstanceConformanceData` | Carries `checkValueConformity`, `deepSequenceValidation` and `deidentified` across the async boundary: the first two select the validate-time depth, the last both drives PHI redaction in the accumulator/report and gates the residual-identifier check in the validator (skipped when false — identifiers are expected on a non-de-identifying destination). |
| `ConformanceReportComponent` (frontend) | **Dedicated** UI block (its own `BoxShadowComponent` in `FormDICOM`/`FormSTOW`): the *Build DICOM conformance report* checkbox, the *Conformance report: list of emails* field, the *Check value content conformity (VR rules)* checkbox, and the *Deep sequence validation (SR, functional groups)* checkbox (shown when enabled, all default off), all bound to the entity. |
| `NotificationComponent` (frontend) | No longer owns the conformance checkbox; reverted to validating `notify` only against `activateNotification`. |
| `ConformanceReportService.sendReport` | Recipients resolve to `conformanceReportNotify`, falling back to `notify` when blank. |

### Configuration knobs

| Property | Default | Where |
|---|---|---|
| `conformance-report.idle-timeout-seconds` | 300 | `ConformanceReportService` |
| `conformance-report.max-study-lifetime-seconds` | 14400 | `ConformanceReportService` |
| `conformance-report.max-sequence-depth` | 8 | `ForwardService` (snapshot depth) **and** `ConformanceReportService` (validate depth) — read in both places, must agree; only applied for destinations with `deepSequenceValidation` on |
| `mail.sender` | — | sender address (reused) |
| `spring.application.version` | `Development` | shown in report header |

---

## 5. Validation logic in detail

`DicomConformanceValidator.validate(attrs, bulkPresentTags, transferSyntaxUid)`:

1. **Module requirements** (`checkModuleRequirements`)
   - Resolve the applicable modules for the SOP Class (`StandardDICOM.getModulesBySOP`).
   - A module is *present* if any of its top-level attributes exists.
   - A **mandatory (usage M)** module that is absent **and** has at least one
     Type 1/2 attribute → `MODULE_MISSING` (ERROR).
   - For present modules, gather each attribute's requirement; the **stricter
     Type** wins when an attribute belongs to several modules.
   - Per attribute: Type 1 missing/empty → ERROR; Type 2 missing → WARNING;
     Type 1C empty-when-present → ERROR. Type 2C/3 and absent 1C are conformant.
   - **Conditional promotion** (`effectiveConditionalType`): when the curated
     `conditionalRequirements` map has a predicate for a 1C/2C attribute and
     `ConditionEvaluator` resolves it to `TRUE` for the instance, 1C is treated as
     Type 1 and 2C as Type 2 (so an absent-but-required attribute is flagged).
     `FALSE`/`UNKNOWN`/uncurated → the present-only behavior above.
   - Sequence-nested attributes (`tag:childTag`) are checked one level deep, one
     finding per instance.
   - SOP Class with no IOD in the bundled standard → IOD checks skipped, and the
     dcm4che registry (`UID.nameOf`, `" (Retired)"` suffix) splits the outcome:
     `RETIRED_SOP_CLASS` (WARNING) for a retired class vs `UNKNOWN_SOP_CLASS`
     (WARNING) for a genuinely unknown one.

2. **Dataset sweep** (`sweepDataset`, recurses sequences down to `maxSequenceDepth`
   levels — 1 by default, raised by the deep-validation option so the SR
   `ContentSequence` tree and enhanced-multiframe functional groups are swept)
   - Skips group-length, private groups and bulk tags for the standard checks.
   - Retired attribute in use → `RETIRED_ATTRIBUTE` (INFO).
   - **VR** mismatch vs the dictionary → `VR_MISMATCH` (ERROR; UN → INFO).
     Skipped for Implicit VR LE.
   - **VM** out of bounds (`VmSpec`) → `VM_VIOLATION` (ERROR).
   - **Enumerated value** not in the curated allow-list → `ENUMERATED_VALUE`
     (ERROR for a `closed` Enumerated Values set, WARNING for an open Defined
     Terms set).
   - **Value-content conformity** (only when `checkValueConformity` is on,
     `VrValueRules`): a string value breaking its VR length → `VALUE_TOO_LONG`
     (**ERROR** for small/structured VRs — `AE/AS/CS/DA/DS/DT/IS/SH/TM/UI`, since
     receivers reject them; **WARNING** for long free-text VRs — `LO/ST/LT/PN`),
     or its format/character-repertoire → `VALUE_FORMAT` (WARNING). At most one
     length + one format finding per attribute. Runs under any transfer syntax
     (the value is real even under Implicit VR — no circularity).

3. **Private-block structure** (`checkPrivateBlocks`, per dataset and per
   sequence item, PS3.5 §7.8) — no private dictionary needed:
   - A private data element whose block has no Private Creator →
     `PRIVATE_CREATOR_MISSING` (WARNING, deduped per block).
   - A present-but-empty Private Creator → `PRIVATE_CREATOR_INVALID` (WARNING).
   - Private *values* are not interpreted.

4. **Study consistency** (`StudyConsistencyChecker`, at `close()`): see table above.

### Known limitations (by design)

- Conditional requirements (Type 1C/2C) are evaluated where the curated
  `conditionalRequirements` map encodes their condition as a machine-evaluable
  predicate (a satisfied condition makes the attribute mandatory); clinical or
  not-yet-curated conditions, and usage C modules, remain present-only — the
  underlying conditions are free text in the standard JSON.
- Private tags: only block structure (creator presence) is checked — values are
  not validated against vendor dictionaries.
- Sequence recursion is one level deep by default. The per-destination
  `deepSequenceValidation` option extends the **encoding** sweep (VR/VM/enumerated/
  retired/private-block/value-conformity) to `maxSequenceDepth` levels, covering
  SR content trees and functional groups — but **module/attribute-Type checks are
  still one level deep** (the standard JSON only encodes one-level tag paths,
  `tag:childTag`), and **SR semantics** (template/TID conformance, content-item
  value-type rules, parent/child relationships) are **not** validated.
- VR is not checked for Implicit VR Little Endian.
- Pixel/bulk data content is not inspected.
- Enumerated-value and SOP↔modality coverage is limited to the curated file;
  curated sets marked `closed` raise ERROR, open Defined-Term sets WARNING.

---

## 6. Concurrency & memory model

- Collection is `@Async`; many instances may be validated in parallel.
- `StudyConformanceAccumulator` is fully `synchronized`. The flusher may
  `close()` a batch concurrently with a late collector — after close, `add()`
  returns `false` and `onConformanceCollect` retries with a fresh accumulator
  (`computeIfAbsent` + `remove(key, accumulator)` CAS loop). Late instances thus
  produce a small follow-up report instead of being lost.
- Memory stays bounded: identical findings are deduplicated with a count, only
  per-series UID sets are retained, datasets are never held — only the pruned
  `MetadataSnapshot` lives between collection and validation, and is dropped
  immediately after.
- The `deepSequenceValidation` option enlarges that transient `MetadataSnapshot`
  (it is copied to `max-sequence-depth` levels instead of 3), so a destination
  receiving deeply-nested objects (large SR trees, enhanced multiframe) holds a
  bigger snapshot per in-flight instance. `max-sequence-depth` is the bound; it is
  only paid when the option is on.

---

## 7. Tests

Existing coverage (JUnit 6 + Mockito, real DICOM `Attributes`, no AssertJ):

| Test | Covers |
|---|---|
| `DicomConformanceValidatorTest` | Type/VR/VM/enumerated/module checks on real datasets, plus deep-sequence recursion (a finding nested two levels deep is skipped at the default depth and caught when the depth is raised). |
| `MetadataSnapshotTest` | Bulk stripping, sequence depth, `bulkPresentTags`. |
| `StudyConformanceAccumulatorTest` | Dedup, counts, close/`ConformanceReport` shape, closed-rejects-add. |
| `VmSpecTest` | VM grammar parsing and matching. |
| `ConformanceReportServiceTest` | Event handling, idle/expired flush, subject, render. |

Run a single class: `mvn test -Dtest=DicomConformanceValidatorTest`.

---

## 8. Remaining tasks

### 8.1 Must-fix before shipping

1. **Verify the schema migration on a real DB.** `db.changelog-1.6.xml`
   (changesets `1.6-1` … `1.6-4`) is auto-discovered via the master
   `includeAll`, so no manual registration is needed — but run it against
   Postgres (and the portable H2 profile) to confirm the
   `build_conformance_report`, `conformance_report_notify`,
   `check_value_conformity` and `deep_sequence_validation` columns are created
   and the `DestinationEntity` mapping resolves.
2. **Confirm `mail.sender` / `spring.mail.*` are documented** as a prerequisite
   in the deployment config (reuse the existing notification mail setup).
3. **Manually verify the UI round-trip:** enable the report on a destination,
   set (or leave blank) the conformance email list, save, reload, and confirm
   `GatewaySetUpService` picks the flag up on the next 5 s refresh.

> The dedicated `ConformanceReportComponent` is now wired end to end (checkbox +
> recipients bound to the entity; service-side recipient fallback to `notify`).
> The custom report **options** (severity threshold, *email only on FAILED*,
> per-destination idle/lifetime overrides) are intentionally **not yet defined**
> — `ConformanceReportComponent` is the place to add their fields and bindings,
> and `ConformanceReportService` the place to apply them before sending.

### 8.2 Toward a "real" conformance check

These extend coverage beyond the current conservative subset:

- **Evaluate conditional requirements.** Parse (or hand-encode) the most common
  Type 1C/2C and usage-C module conditions so conditionally-required attributes
  that are *missing* can be flagged, not just silently skipped. Start with a
  curated condition table for high-value modules.
- **Validate value formats, not just presence/VM.** *Done* as an opt-in
  per-destination option (`checkValueConformity` → `VrValueRules`): VR length +
  format for the common string VRs. Still open: more VRs (UR/UC), and stricter
  semantic checks (valid calendar date/time ranges, registered UID roots).
- **Expand curated rules.** Grow `enumeratedValues` and `sopClassToModalities`
  coverage; consider generating enumerated values from the DICOM PS3.3 source
  rather than hand-curating.
- **Deeper sequence validation.** *Partly done* as an opt-in per-destination
  option (`deepSequenceValidation` → `max-sequence-depth`): the **encoding** sweep
  (VR/VM/enumerated/retired/private-block/value-conformity) now recurses every
  sequence level, covering SR content trees and functional groups. Still open:
  (1) deepen the **module/attribute-Type** checks (the standard JSON only encodes
  one-level `tag:childTag` paths, so nested mandatory attributes are not checked);
  (2) **SR-specific semantics** — content-item value-type required children
  (e.g. a NUM item's `MeasuredValueSequence`), parent/child relationship rules,
  and template (TID) conformance — which need a typed SR content model and a
  curated template/rule set the standard JSON does not provide.
- **Private tag handling.** Block-structure validation (orphan blocks, empty
  creators — PS3.5 §7.8) is **done** (`checkPrivateBlocks`). Still open: a
  "private creators present" INFO summary for de-id review, and optional
  dictionary-backed VR/VM checks for recognized creators.
- **UID integrity & referential checks.** Verify `SOPInstanceUID` uniqueness
  within the batch, that referenced UIDs (e.g. `ReferencedSOPInstanceUID`)
  resolve within the study, and Frame of Reference cross-series rules.
- **Persist / surface reports in the UI.** Currently the report is email-only.
  Consider storing a `ConformanceReport` summary (counts, verdict) per study for
  the monitoring views, with a downloadable HTML/JSON artifact.
- **Per-destination report policy.** Options such as "email only on FAILED",
  severity threshold, or aggregating multiple studies into a digest.
- **Internationalisation / templating** of the email if Karnak gains i18n.

### 8.3 Maintenance

- Keep `CuratedValidationRules.DICOM_STANDARD_SOURCE` and
  `doc/dicom-standard-json.md` in sync when the vendored standard JSON is
  updated (see that doc's update procedure).
