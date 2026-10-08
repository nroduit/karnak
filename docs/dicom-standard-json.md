# DICOM standard JSON files

## Source

Karnak bundles the DICOM standard in machine-readable JSON form, produced by the
[innolitics/dicom-standard](https://github.com/innolitics/dicom-standard) project,
which parses the official DICOM PS3 publication from NEMA.

The files live under
`src/main/resources/org/karnak/backend/model/dicominnolitics/`:

| File | Content |
|---|---|
| `sops.json` | SOP Class UID → name → CIOD |
| `ciods.json` | Composite IODs (id, name, description) |
| `ciod_to_modules.json` | CIOD → module usage (`M`/`U`/`C`) + conditional statement (free text) |
| `module_to_attributes.json` | Module → attribute tag paths with DICOM Type (`1`, `1C`, `2`, `2C`, `3`) |
| `attributes.json` | Data dictionary: tag, name, keyword, VR, VM, retired flag |
| `confidentiality_profile_attributes.json` | PS3.15 confidentiality profile actions |

They are parsed by the Gson POJOs in `org.karnak.backend.model.dicominnolitics`
(`JsonStandardReader`) and exposed through `org.karnak.backend.model.standard.StandardDICOM`.

**Vendored version: files last updated 2025-02 (commit `8cf020b6`,
"Update DICOM's JSON reference files from Innolitics").** The label shown in the
header of the conformance report emails is read from the `source` field at the top
of `curated-validation-rules.json` (the single source of truth) — update that field
whenever you re-vendor the standard files.

## Who uses them

- The de-identification profile pipeline (`AddTag`, `MultipleActions`, attribute lookups).
- The **DICOM conformance validation report**
  (`org.karnak.backend.model.validation.DicomConformanceValidator` +
  `ConformanceReportService`): IOD module presence, attribute Type (1/1C/2/2C/3),
  VR and VM checks are all driven by these files.

## Curated supplement

The innolitics JSON has two gaps relevant to validation:

1. **No machine-readable enumerated values** — they only exist in HTML descriptions.
2. **Module and attribute conditions are free text** — `conditionalStatement` and the
   "Required if …" sentences in attribute descriptions cannot be evaluated as-is. The
   curated file encodes the machine-evaluable subset of Type 1C/2C conditions (those
   referring to another attribute) so they can be checked; the remainder (clinical
   free text, usage C modules) is still validated only when present.

Gap 1 is partially covered by a small curated file,
`src/main/resources/org/karnak/backend/model/validation/curated-validation-rules.json`,
which contains:

- `enumeratedValues`: allowed values for a curated set of common attributes (Patient
  Sex, Image/Laterality, Photometric Interpretation, Pixel/Planar configuration,
  Presentation Intent Type, Burned In Annotation, Lossy Image Compression, …). Keys are
  lowercase 8-digit hex tags; each entry is `{ "values": [...], "closed": <bool> }`.
  `closed: true` marks a DICOM **Enumerated Values** set (a value outside it is an ERROR);
  `closed: false`/omitted marks an open **Defined Terms** set (an unexpected value is only
  a WARNING).
- `sopClassToModalities`: expected Modality value(s) per common storage SOP Class,
  used for the SOP Class ↔ Modality coherence check.
- `retiredTransferSyntaxes`: retired transfer syntax UIDs flagged with a WARNING.
- `conditionalRequirements`: machine-evaluable Type 1C/2C conditions, keyed by
  `moduleId/tagPath` (the `tagPath` is the innolitics `path` with the leading
  `moduleId` segment removed). Each entry has a `requiredWhen` predicate consumed by
  `ConditionEvaluator`; a `_condition` field carries the original sentence for
  provenance and is ignored at runtime. Predicate grammar:
  - Leaf: `{ "tag": "<8-hex>", <op> }` where `<op>` is one of `present` (boolean),
    `equals` (string), `in` / `notIn` (string list). `tag` references another
    top-level attribute in the same dataset (lowercase 8-digit hex).
  - Composite: `{ "allOf": [ … ] }` or `{ "anyOf": [ … ] }`.

  A condition resolves to TRUE / FALSE / UNKNOWN; only TRUE promotes the attribute to
  mandatory (1C→Type 1, 2C→Type 2). UNKNOWN (the referenced attribute is itself
  absent) and FALSE keep the present-only behavior, so a curated rule can never raise
  a false positive.

Extend `enumeratedValues` / `sopClassToModalities` / `retiredTransferSyntaxes` when new
checks are needed; entries must be obviously correct per the DICOM standard (PS3.3 for
enumerated values, PS3.6/PS3.5 for UIDs).

To grow `conditionalRequirements`, run the bootstrap helper
`doc/tools/extract_conditional_requirements.py` (not part of the build). It mines the
unambiguous "Required if (gggg,eeee) is/equals/present …" sentences from
`module_to_attributes.json` into draft DSL entries for **human review** before they are
merged. It deliberately handles only top-level attributes and rejects multi-clause,
negated or numeric conditions, so anything subtle stays out and is checked only when
present. The shipped set covers the safe top-level subset; the
`CuratedConditionalRequirementsTest` guards the file's well-formedness.

## How to update the standard JSON

1. Get the latest generated files from the innolitics repository
   (`standard/*.json` in <https://github.com/innolitics/dicom-standard>), or regenerate
   them with that project's tooling.
2. Replace the files under `src/main/resources/org/karnak/backend/model/dicominnolitics/`
   (keep the same file names).
3. Update the vendored version in:
   - this document (source/date above), and
   - `CuratedValidationRules.DICOM_STANDARD_SOURCE`.
4. Run the test suite: `mvn test`. The dicominnolitics and validation tests parse all
   files and will catch schema drift (new/renamed JSON fields require updating the
   `Json*` POJOs).
