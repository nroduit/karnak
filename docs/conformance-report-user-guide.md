# DICOM Conformance Report — User Guide

Karnak can validate the DICOM data it forwards against the DICOM standard and
email you a **conformance report** for each study. The report tells you whether
the de-identified data actually leaving Karnak still conforms to the DICOM
standard — mandatory attributes present, correct encoding, plausible values and
geometry, valid codes, consistent identifiers, and no direct identifier left
behind — so you can catch problems introduced by acquisition devices or by the
de-identification / tag-morphing pipeline before they reach the destination.

The checks mirror the validations performed by David Clunie's widely used
`dciodvfy` IOD verifier (from `dicom3tools`), adapted to run inline on the
de-identified stream. See [Categories of checks performed](#categories-of-checks-performed)
for the full list.

> The report validates the **de-identified data actually sent** to the
> destination — not the original images Karnak received. This is intentional:
> it shows you exactly what the receiver gets.

---

## 1. Enabling the report

The report is configured **per destination**.

1. Open the web UI and go to the **forward node** that owns the destination.
2. Edit the **destination** (DICOM C-STORE or DICOMWeb/STOW-RS — both are
   supported).
3. In its own **DICOM conformance report** section (separate from
   *Notification*):
   - Tick **Build DICOM conformance report**.
   - Optionally fill **Conformance report: list of emails** with one or more
     recipient addresses (comma-separated). **Leave it empty to reuse the
     notification emails** of the destination.
   - Optionally tick **Check value content conformity (VR rules)** to also
     validate that attribute *values* obey their VR length and format rules
     (see [Scope & limitations](#6-scope--limitations)). Off by default — it is
     stricter and tends to surface many warnings on real-world data.
   - Optionally tick **Deep sequence validation (SR, functional groups)** to run
     the encoding checks through *every* sequence level instead of only the first
     one. Use it for objects whose content lives deep in nested sequences —
     **Structured Reports** (the `ContentSequence` tree), enhanced multiframe
     CT/MR/PET (per-frame functional groups), Segmentation, RT objects. Off by
     default: deeper recursion makes each report take a little more time and
     memory (see [Scope & limitations](#6-scope--limitations)).
4. Save the destination.

Conformance reporting is independent from the *Activate notification* feature
(error/rejection alerts): each has its own activation, and the conformance
report has its own recipient list. If you leave the conformance email list blank
and have no notification emails either, the report has nowhere to go and is
dropped (a warning is logged).

> **Prerequisite — outgoing mail must be configured.** The report is delivered
> by email, so Karnak's mail server settings (`spring.mail.*` and the
> `mail.sender` / sender address) must be set, exactly as for the existing
> error/rejection notifications. If mail is not configured, no report is sent.

---

## 2. When is a report sent?

Reports are **batched per study**, not per image. Karnak groups every instance
of the same study going to the same destination and sends **one report per
study** once the transfer settles:

- A report is sent when **no new instance** of the study has arrived for a
  quiet period (default **5 minutes**).
- A safety cap forces a report for a study that keeps trickling in for a very
  long time (default **4 hours**).
- Pending reports are also flushed when Karnak shuts down.

If a few late instances arrive after a report was already sent, they produce a
small follow-up report for the same study.

These timings are tunable by an administrator (see
[Administrator settings](#5-administrator-settings)).

### How instances are grouped into reports

A report covers **one study, sent to one destination, from one source** — the
grouping key is *(source forward node, destination, Study Instance UID)*. This is
**independent of the DICOM connection (C-STORE association)** used to send the
images: Karnak sorts each incoming instance into its study bucket and closes that
bucket on inactivity, not when a connection opens or closes. So:

| Situation | What you get |
|---|---|
| One connection carries **several studies** | **One report per study** — each sent when *that* study goes quiet |
| One study arrives over **several connections** (a resend, a paused or resumed transfer, images trickling in) | **One report** — as long as the gaps stay under the idle timeout |
| Instances arrive **after** a study's report was already sent | A small **follow-up report** for the same study |
| The source sends to **several destinations** | **One report per destination** (each destination has its own profile, so its de-identified result is validated separately) |

### Several studies for the same patient

There is **no per-patient report** — grouping is per *study*. If a patient has
three studies, you receive **three separate reports** (per destination), even
though they share the same (de-identified) patient identity, which is shown in
each report's header. Patient consistency is only checked *within* a single study
(a study whose instances carry more than one Patient ID/Name is flagged); it is
never aggregated across studies.

---

## 3. Reading the report

The email is a self-contained HTML page with the following sections.

### Header & verdict

- **PASSED** (green) — no `ERROR`-level finding. Warnings/info may still be
  present.
- **FAILED** (red) — at least one `ERROR`-level finding (a DICOM standard
  violation).
- A totals strip shows the count of **errors**, **warnings** and **info**
  items, plus when the report was generated, the Karnak version and the DICOM
  standard edition used for validation.

### Study summary

Patient ID / name, Study UID, study date, description, accession number, the
content sent (series count, instance count, modalities) and the number of
**failed transfers** with their reasons (if any). All identifiers shown are the
de-identified values that were actually transmitted.

### Content sent

The list of **SOP Classes** and **transfer syntaxes** seen in the study, by
name and UID — a quick check that the data type and encoding are what you
expect.

### Series

One row per series: series UID, modality, SOP Class(es) and instance count.

### Conformance findings per SOP Class

The core of the report. Findings are grouped by SOP Class and deduplicated: an
identical issue affecting many instances appears **once**, with an **Instances**
count. Each row has:

| Column | Meaning |
|---|---|
| **Severity** | `ERROR`, `WARNING` or `INFO` (see legend below). |
| **Issue** | The category of check (e.g. *Type 1 missing*, *VR mismatch*, *Invalid value*). |
| **Tag** | The attribute location, e.g. `(0010,0010)`, or `(0040,0275) > (0008,0050)` for an attribute inside a sequence. |
| **Attribute / Module** | The attribute or IOD module name. |
| **Expected** | What the standard requires (the Type, VR, VM, or the allowed values). |
| **Found** | What was actually present. |
| **Instances** | How many instances of that SOP Class had this finding. |

### Categories of checks performed

The validator runs the following families of checks on every instance. Severity
is the default; a few checks are **optional** (value-conformity option) or
**deep-only** (deep-sequence option) as noted.

| Family | Checks | Severity |
|---|---|---|
| **IOD presence & structure** | Mandatory module missing; Type 1 missing/empty; Type 2 missing; Type 1C/2C evaluated where the condition is curated; standard attribute not part of this IOD (*Standard Extended SOP Class*); unknown / retired SOP Class. | ERROR / WARNING / INFO |
| **Encoding (VR / VM)** | Value Representation does not match the dictionary; value multiplicity out of range. | ERROR (VR `UN` → INFO) |
| **Value content** *(optional)* | Value too long for its VR; malformed date/time, Code String, UID, numbers, Person Name. Enabled only with **Check value content conformity**. | ERROR / WARNING |
| **Enumerated values** | Value outside a closed Enumerated Values set (e.g. Patient Sex); unexpected Defined Term. | ERROR / WARNING |
| **Plausible values** | Zero where illegal — structural counts & geometry (Rows, Columns, Bits Allocated/Stored, Samples per Pixel, Number of Frames, Pixel/Imager Spacing, Slice Thickness, Rescale Slope) → ERROR; zero where implausible — acquisition/physics parameters (KVP, X-Ray Tube Current, Exposure(s), distances, Echo/Repetition Time, Magnetic Field Strength, Flip Angle, Patient Weight/Size…) → WARNING; negative Spacing Between Slices → ERROR (Nuclear Medicine exempt). | ERROR / WARNING |
| **Pixel geometry** | Pixel Aspect Ratio present with a 1:1 ratio (must be omitted); Pixel Spacing differs from Imager / Nominal Scanned Pixel Spacing without a Pixel Spacing Calibration Type. | ERROR / WARNING |
| **Image geometry** | Image Orientation (Patient) row/column are not unit vectors, or not mutually orthogonal. | ERROR |
| **Patient orientation** | Illegal direction code, opposing directions in one value (A/P, H/F, L/R), or identical row and column directions (biped; quadruped skipped). | ERROR |
| **Laterality** | A paired (non-midline) Body Part Examined carries no Laterality (skipped when another laterality is conveyed, or for segmentation / specimen / waveform objects). | WARNING |
| **Codes** | Code Value uses characters not allowed by its Coding Scheme Designator (SNOMED / DICOM); code denotes the "Unknown" concept. | WARNING |
| **Identifiers & consistency** | The SOP Instance / Series / Study / Frame of Reference UIDs reuse one another's value; (study-level) more than one Study UID, Patient identity, or Frame of Reference; Modality ↔ SOP Class coherence; retired transfer syntax. | ERROR / WARNING |
| **Privacy (de-identification verification)** | A direct identifier (e.g. Patient's Telephone Numbers, Address, Other Patient Names, Institution / physician names, Occupation, Patient Comments) is still present after de-identification. **Only run for a de-identifying destination.** | WARNING |
| **Private blocks** | A private attribute is not covered by a non-empty Private Creator (orphan or empty private block). | WARNING |
| **Enhanced multi-frame** | Per-frame Functional Groups item count ≠ Number of Frames; *(deep)* Dimension Index Values count ≠ Dimension Index Sequence; *(deep)* a functional group present in both the Shared and a Per-frame Functional Groups item. | ERROR |
| **Segmentation** | Segment Numbers do not increase monotonically from one by one (LABELMAP segmentations exempt). | ERROR |
| **Retired usage** | A retired attribute, SOP Class or transfer syntax is in use. | INFO / WARNING |

> **Note on the privacy check.** It is *not* part of `dciodvfy` (which is a pure
> IOD verifier); it is a Karnak addition for the de-identification use case, and
> its attribute list mirrors the identifiers removed by `dciodvfy`'s companion
> de-identifier, `dcanon`. It runs **only on a de-identifying destination**,
> where a residual identifier indicates a gap in the profile; on a destination
> that forwards identified data by design these attributes are expected, so the
> check is skipped and never appears in the report.

### Study consistency

Cross-dataset checks that only make sense across the whole study: a single
Study UID, a single Patient identity, consistent Frame of Reference within a
series, Modality ↔ SOP Class coherence and non-retired transfer syntaxes.

### How to read this report

Every email ends with a legend explaining the severities and issue colors.

---

## 4. Severity meanings

| Severity | Meaning | Examples |
|---|---|---|
| **ERROR** | A violation of the DICOM standard. The data is non-conformant. | Missing or empty **Type 1** attribute, missing **mandatory module**, wrong **VR**, wrong **VM**, a value outside a **closed enumerated set** (e.g. Patient Sex), a **zero in a structural/geometry field** (Rows, Columns, Pixel Spacing…), a **1:1 Pixel Aspect Ratio**, **non-unit or non-orthogonal Image Orientation**, an illegal **Patient Orientation**, a **reused UID** across entity levels, a **per-frame group count ≠ Number of Frames**, **non-monotonic Segment Numbers**, an **over-long value in a small/structured field** (when value-conformity is enabled), two different Study UIDs in one study, mismatched Patient identity. |
| **WARNING** | A deviation from recommended practice — usually accepted by receivers, but worth reviewing. | Missing **Type 2** attribute, an unexpected **defined term** value, a **zero in an acquisition/physics field** (KVP, exposure…), a **paired body part without a laterality**, an **invalid or "Unknown" code**, a **residual direct identifier** still present after de-identification, Modality not matching the SOP Class, a **retired SOP Class** or **retired transfer syntax**. |
| **INFO** | Informational only, no action required. | A **retired attribute** is still in use, an **unknown VR (UN)**, a **standard attribute outside this IOD** (Standard Extended SOP Class). |

The issue color groups checks into categories: **presence & privacy**
(red), **encoding — VR/VM, multi-frame & segmentation** (purple),
**value, geometry & code constraints** (teal), **identifiers & consistency**
(blue), **retired / unknown usage** (gray).

---

## 5. Administrator settings

These application properties (e.g. in `application.yml` or as environment
overrides) control batching. Defaults are shown:

| Property | Default | Meaning |
|---|---|---|
| `conformance-report.idle-timeout-seconds` | `300` | Quiet period (seconds) after the last instance of a study before its report is sent. |
| `conformance-report.max-study-lifetime-seconds` | `14400` | Hard cap (seconds): a study still receiving instances after this long is reported anyway. |
| `conformance-report.max-sequence-depth` | `8` | Number of sequence levels the checks recurse into for destinations with **Deep sequence validation** enabled. Bounds how deep SR content trees / functional groups are walked (and how deep the in-memory snapshot is kept). No effect when deep validation is off. |

Outgoing mail uses the same configuration as Karnak's other notifications
(`spring.mail.*` and the configured sender address).

---

## 6. Scope & limitations

The validator is **automated and conservative** — it flags issues it is
confident about and stays silent where the standard cannot be checked
mechanically. Keep these limits in mind when interpreting a PASSED verdict:

- **Conditional requirements are checked where the condition can be evaluated.**
  For many DICOM Type 1C/2C attributes the condition refers to another attribute
  (e.g. *required if attribute X is present / equals a value*); Karnak evaluates
  these and flags a required attribute that is missing. Conditions expressed in
  clinical free text (e.g. *required if contrast media was used*), conditional
  (usage *C*) modules, and conditions not yet curated are **only checked when
  present** — a legitimately-absent such attribute is not flagged, and the tool
  cannot confirm it *should* have been present.
- **De-identification is respected.** The pipeline may legitimately empty or
  remove attributes; the report shows the de-identified result and does not flag
  removals that the standard permits.
- **Value-content conformity (VR rules) is optional and off by default.** When
  the *Check value content conformity* option is enabled, the report also flags
  values that break their VR's length or format rules. An **over-long value in a
  small/structured field** (codes, identifiers, dates, numbers — e.g. Code
  String, Short String, UID, Integer/Decimal String) is an **ERROR**, because
  many PACS reject such objects; an over-long value in a **long free-text field**
  (Long String, Short/Long Text, Person Name) is a **WARNING**. Malformed
  formats (bad date/time, lowercase in a Code String, non-numeric Integer
  String, …) are **WARNINGs**. It is opt-in because real-world devices deviate
  from these rules frequently, which would otherwise make every report noisy.
- **Pixel data and other bulk values are not inspected** (only their presence is
  considered). Image content is not validated.
- **Private tags** are checked for *block structure* only — every private
  attribute must be covered by a non-empty Private Creator (a WARNING is raised
  for an orphan or empty private block). Their *values* are not validated, since
  that needs vendor-specific dictionaries Karnak does not have.
- **Deeply nested attributes** are validated only one sequence level down by
  default. Enable **Deep sequence validation** on the destination to extend the
  encoding checks (VR, VM, enumerated values, retired attributes, private-block
  structure, optional value-conformity) through every sequence level, up to
  `conformance-report.max-sequence-depth` (default 8). This covers, for example,
  the **Structured Report** `ContentSequence` tree and enhanced multiframe
  functional groups. Two **enhanced multi-frame** consistency checks also require
  this option, because they read per-frame functional groups: *Dimension Index
  Values count* and *functional group present in both Shared and Per-frame*. The
  lighter multi-frame and segmentation checks (per-frame item count vs Number of
  Frames, Segment Number monotonicity) run regardless of the option. Deep
  validation still checks the *encoding* and the specific structural rules listed
  in [Categories of checks performed](#categories-of-checks-performed); it does
  **not** check SR-specific semantics (template/TID conformance, content-item
  value-type rules, parent/child relationships) or whole-slide/SR-reference
  relationships, which remain out of scope.
- **VR is not checked for Implicit VR Little Endian** datasets (the VR would be
  inferred from the dictionary, making the check meaningless).

A PASSED report therefore means *"no detectable standard violation"*, which is a
strong signal but not a formal certification of full DICOM conformance.

---

## 7. Privacy note

The report shows the study metadata **actually sent** to the destination, and no
pixel data is ever included. What that metadata contains depends on the
destination:

- **De-identifying destination** — the values shown are the pseudonymized ones
  produced by your profile (Patient ID/name, UIDs, dates as transformed). Safe to
  share with the report's recipients. The report additionally **flags any direct
  identifier still present** after de-identification (see the *Privacy* row in
  [Categories of checks performed](#categories-of-checks-performed)), so a gap in
  the profile shows up as a `WARNING` rather than silently leaking.
- **Non-de-identifying destination** (de-identification is off) — the values are
  real. To limit exposure, Karnak **omits the Patient Name** from such reports
  (the header shows *"(hidden — destination not de-identified)"* and the name is
  never used in any finding). Other identifiers it still needs to locate the
  study — **Patient ID, Accession Number, Study/Series UIDs, study date and
  description** — are shown as-is. The residual-identifier (Privacy) check is
  **skipped** here: nothing was meant to be removed, so the identifiers still
  present are expected rather than a leak.

Either way, treat the email as sensitive and send it only to trusted recipients.