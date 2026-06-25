# Profile Builder — New UI Design

A redesign of Karnak's de-identification **Profile** editor: the screen where a
user assembles an ordered pipeline of profile elements (Basic DICOM confidentiality,
tag actions, date shifting, UID replacement, pixel cleaning, defacing, etc.) that is
applied to every study forwarded to a destination.

The goals are to make the *pipeline as a whole* legible at a glance, to cut the
modal-heavy tag-selection flow down to a fluid inline experience, and to surface the
DICOM semantics (VR, module, requirement type, retired status) that today are hidden
behind a separate browser dialog.

---

## 1. Layout overview

A three-pane workspace replacing the current 25 / 75 split:

```
┌───────────────┬──────────────────────────────────────┬──────────────────┐
│  Profiles     │   Pipeline (ordered elements)         │  Inspector       │
│  (browser)    │                                       │  (context panel) │
│               │   ① Action on dates       ⋮ ⌄ ✎ 🗑   │                  │
│  ▸ Default    │   ② Replace UIDs          ⋮ ⌄ ✎ 🗑   │  Selected element│
│  ▾ Custom     │   ③ Action on tags        ⋮ ⌄ ✎ 🗑   │  details + live  │
│    • CT deid  │   ④ Clean pixel data      ⋮ ⌄ ✎ 🗑   │  validation +    │
│    • MR study │   ⑤ Basic DICOM (pinned)  🔒          │  affected tags   │
│               │                                       │                  │
│  [+ New]      │   [+ Add element ▾]                   │                  │
└───────────────┴──────────────────────────────────────┴──────────────────┘
```

- **Left — Profile browser.** Grouped, searchable list of profiles. Default
  (built-in) profiles are read-only and badged; custom profiles are editable. Each
  row exposes *Duplicate*, *Download YAML*, and *Delete*.
- **Center — Pipeline.** The ordered list of profile elements, rendered as cards
  that can be reordered by drag handle (replacing the up/down arrow buttons). Each
  card shows a one-line human-readable summary (action + tag count + condition badge).
  The Basic DICOM element is visually pinned to the bottom and locked.
- **Right — Inspector.** Replaces the modal editor for most edits. Selecting a card
  loads its form here with live validation and a preview of which tags it touches.

---

## 2. Key changes from the current UI

| Area | Today | New design |
|------|-------|------------|
| Element editing | Modal dialog (`ProfileElementEditor`) | Inline inspector pane; modal only for the full tag browser |
| Reordering | Up / down arrow buttons per row | Drag-and-drop handle, keyboard-accessible |
| Tag selection | Repeated open/close of `TagPickerDialog` | Inline type-ahead in the field + dialog for deep browsing; multi-add without closing |
| Validation | On save only | Live, as-you-type, shown in the inspector |
| New element types | Hardcoded `switch` in form builder | Schema-driven forms (see §5) |
| Profile reuse | Recreate from scratch | **Duplicate profile** action |
| DICOM context | VR/module only visible in picker | VR, module, requirement type, retired shown inline next to each selected tag |
| Default profiles | Plain read-only text list | Same rich pipeline view, read-only — so users can study them before duplicating |

---

## 3. The pipeline cards

Each element is a card with:

- **Position index** and **drag handle** (reorder).
- **Type icon + name** — e.g. *Action on dates*, *Replace UIDs*, *Action on tags*.
- **Summary line** — the resolved human description, e.g.
  *"Remove · 12 tags · 2 excluded"* or *"Shift dates by 30 days"*.
- **Condition badge** — shown only when a SpEL condition is set; hovering reveals the
  expression.
- **Row actions** — edit (loads inspector), duplicate element, delete.
- **Status dot** — green (valid), amber (warning), red (invalid) reflecting live
  validation.

Special rules carried over from the model:

- **Basic DICOM** is unique and always last (pinned, lock icon, not draggable past).
- **Clean Pixel Data** and **Defacing** are unique — the *Add element* menu disables
  them once present.
- A **"Basic DICOM profile missing"** warning banner appears above the pipeline when
  absent, with a one-click *Add it* fix.

---

## 4. Add element

`[+ Add element ▾]` opens a categorized menu instead of a flat type dropdown:

- **Confidentiality** — Basic DICOM, Clean Pixel Data, Defacing
- **Tag actions** — Action on tags, Action on private tags, Add tag
- **Identifiers & dates** — Replace UIDs, Action on dates
- **Advanced** — Expression on tags, Replace via API (when exposed)

Picking a type appends a new card and focuses the inspector. Unavailable types
(uniqueness already satisfied) are greyed with an explanatory tooltip.

---

## 5. Schema-driven element forms

Instead of a per-type `switch`, each profile element type declares a small form
schema (fields, allowed actions, argument shape) consumed generically by the
inspector. This makes adding a new element type a data change, not a UI rewrite.

Per-type inspector contents:

- **Action on tags / Action on private tags** — Action selector
  (Keep `K` · Remove `X` · Replace null `Z` · Replace dummy `D` · Default dummy
  `DDum`), included tags, excluded tags, optional condition.
- **Replace UIDs** — Action selector (New UID `U` · Remove `X` · Replace null `Z`),
  tags.
- **Action on dates** — Date strategy (shift · shift range · shift by tag ·
  date format) driving a dynamic argument sub-form (e.g. *days/seconds*, *range
  bounds*, *reference tag*, *format pattern*), optional tags.
- **Add tag** — single tag + value, with VR-aware value hint.
- **Basic DICOM / Clean Pixel Data / Defacing** — no options; show an explanatory
  card describing what the step does and its DICOM code (113100 / 113101 / 113102).

The **Action** vocabulary is sourced from `DeidActionType`, so labels and symbols
stay in sync with the backend.

---

## 6. Tag selection, reimagined

The tag field becomes a first-class control, not a launcher:

- **Inline type-ahead** — typing a keyword, name, or `(gggg,eeee)` queries
  `DicomStandardService.searchAttributes(...)` and offers matches without leaving the
  form. Enter adds; selected tags appear as chips.
- **Chips with semantics** — each chip shows the keyword and, on hover, VR + module +
  requirement type; retired tags carry a subtle badge.
- **Browse dialog for discovery** — the existing `TagPickerDialog` is kept for
  module-based browsing, but in **multi-add** mode: check several tags across modules
  and add them all in one confirm, with a running "N selected" count.
- **Bulk paste** — paste a comma/space-separated list of tags or keywords; valid ones
  resolve to chips, unknown ones are flagged.

---

## 7. Live validation & feedback

- Each inspector edit calls the existing `ProfilePipeService` validation path and
  reflects the result immediately on the card's status dot and inline under the field.
- Errors are specific (e.g. *"Action 'Replace dummy' requires at least one tag"*)
  rather than a single save-time failure.
- The **condition** field gets lightweight SpEL syntax feedback (balanced
  parentheses, known property hints) before save.

---

## 8. Profile-level actions

- **Duplicate profile** — clone name + all elements as `"<name> (copy)"`, editable.
- **Download YAML** — unchanged export via the Jackson YAML path.
- **Import YAML** — drag a `.yml` onto the browser; validation issues render inline in
  the pipeline (per-element) instead of a separate error screen.
- **Metadata strip** — name, version, minimum Karnak version edited in a compact
  header above the pipeline rather than a separate component.

---

## 9. Out of scope (for a first iteration)

- Side-by-side profile diffing/comparison.
- Visual masking/defacing region editor (masks remain managed as today).
- Real study preview ("apply this profile to a sample and show before/after").

These are natural follow-ups once the schema-driven inspector and inline tag
selection are in place.

---

## 10. Implementation notes

- Reuses existing services unchanged: `ProfilePipeService` (CRUD, reorder, validate),
  `DicomStandardService` / `StandardDICOM` (tag search, modules, attribute details),
  `DeidActionType` and `ProfileItemType` (vocabularies).
- The main new frontend work is the three-pane `ProfileView` layout, the inspector
  pane (generic schema-driven form replacing `ProfileElementEditor`'s `switch`), and
  the upgraded `TagPickerField` (inline type-ahead + multi-add).
- No schema/Liquibase changes required — the persistence model
  (`ProfileEntity` / `ProfileElementEntity` + tags/arguments) already supports
  everything described here.