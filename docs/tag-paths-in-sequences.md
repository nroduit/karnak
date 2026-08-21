# Tag paths: scoping a profile item to a sequence

This note documents the change that lets a profile item target a tag **inside a given
sequence** instead of wherever the tag happens to appear, and lets `AddTag` write an
attribute into a sequence. It concerns the de-identification / tag-morphing pipeline
(`org.karnak.backend.service.profilepipe.Profile`), the profile items built from
user-configured tags, and the profile element editor.

It builds on [`dcm` vs `original`, and the scope of a nested tag](dcm-and-original-scope.md),
which fixed *how* nested tags are read; this one is about *which* nested tags an item
applies to.

## The gap

Nested tags were already visited: `Profile.applyAction` recurses into every item of every
sequence and re-runs the whole profile item list on the nested tags. But matching was by
tag number alone — `TagActionMap.get(int)` knows nothing of where the tag sits — so an
item configured with `(0008,1030)` hit StudyDescription at the root **and** every
occurrence inside any sequence, with no way to say "only inside RequestAttributesSequence"
or "only at the root".

No path syntax existed anywhere in the chain:

- `TagActionMap.put` stripped `[(),\s]` and then required exactly 8 hex/`X` characters, so
  `(0040,0275).(0040,0009)` reached `TagUtils.intFromHexString("00400275.00400009")` and
  blew up.
- `StandardDICOM.cleanTagPath` is named "path" but only removes parentheses.
- The picker only produced flat dictionary tags.
- `AddTag` could only write to the top-level dataset: its `tagAdded` latch is keyed on
  SOPInstanceUID and fires on the first tag visited, which is the first tag of the root
  dataset.

The only workaround was incidental. `ExprCondition` resolves tags from the current dataset
*outwards* (`DicomObjectTools.getStringInScope`), so a condition like
`tagIsPresent("(0040,0009)")` happens to be true only while inside RequestAttributesSequence
items. That breaks as soon as the discriminating tag also exists at study level.

## The grammar

A configured tag value may now be a dot-separated path. The last segment designates the
tag itself, the preceding ones the sequences enclosing it.

| Written as | Matches |
| --- | --- |
| `(0040,0009)` | the tag at **any depth**, root included — unchanged, and what every stored profile means today |
| `.(0040,0009)` | the tag at the **root only** |
| `(0040,0275).(0040,0009)` | the tag directly inside a `(0040,0275)` item, itself at any depth |
| `.(0040,0275).(0040,0009)` | same, but the sequence must be at the root |
| `*.(0040,0009)` | the tag exactly one sequence below its enclosing dataset |
| `**.(0040,0009)` | the tag at any depth except the root |
| `(0040,0275).*` | any tag directly inside a `(0040,0275)` item |

Two rules carry most of the meaning:

- **A value without a dot is not a path.** It keeps its current "match at any depth"
  semantics, so no stored profile changes behaviour. Narrowing an existing rule to the root
  is a migration the profile author makes deliberately, by adding the dot.
- **A leading dot anchors, its absence floats.** A floating pattern is matched against the
  *end* of the location, so the sequences it names may themselves be nested anywhere.

Each tag segment also accepts the `X` wildcards already supported for flat tags, so
`(0040,0275).0040XXXX` is valid.

### Where it lives

`TagPathPattern` (`backend/model/profilepipe/`) parses a path into segments — a tag with
its bit mask, `*` (exactly one level) or `**` (one or more) — and matches it against a
location. `TagPath` is the location itself: the chain of sequence tags enclosing the tag
being visited, `TagPath.ROOT` at the top level, `descend(sequenceTag)` on the way in.

## Threading the location through the walk

`Profile.applyAction` carries a `TagPath` and descends with it:

```java
TagPath itemPath = path.descend(tag);
for (int i = 0; i < seq.size(); i++) {
    this.applyAction(seq.get(i), originalItem(originalSeq, i, original), hmac, currentProfile,
            currentAction, context, itemPath);
}
```

The public six-argument `applyAction` stays the entry point and delegates to a private
overload taking the path, so recursion is the only thing that ever sets it.

`ProfileItem` gained a default overload rather than a changed signature:

```java
default @Nullable ActionItem getAction(Attributes dcm, Attributes original, int tag, HMAC hmac, TagPath path) {
    return getAction(dcm, original, tag, hmac);
}
```

The pipeline calls the five-argument form. **Only the items whose tags the user configures
override it** — `ActionTags`, `ActionDates`, `Expression`, `ReplaceApi`, `PrivateTags`, and
`BasicProfile`, which forwards the path to the confidentiality-profile items it delegates
to. Items selecting a fixed set of standard tags (`UpdateUIDsProfile`, `CleanPixelData`,
`Defacing`) are location-independent and keep the four-argument form.

## Resolution order in `TagActionMap`

Three kinds of entry can be registered, and `get(tag, path)` tries them in order:

1. **path** entries — the most specific match wins;
2. **exact tag** entries, matched at any depth;
3. **wildcard tag** entries, matched at any depth.

So a path takes precedence over a flat tag, which keeps matching everywhere else:

```java
map.put("(0020,0010)", anywhere);
map.put("(0040,0275).(0020,0010)", inSequence);

map.get(Tag.StudyID, TagPath.ROOT.descend(Tag.RequestAttributesSequence)); // inSequence
map.get(Tag.StudyID, TagPath.ROOT);                                        // anywhere
```

Specificity is `namedSegments * 2 + (anchored ? 1 : 0)`: a named sequence beats a wildcard
level, and an anchored pattern breaks a tie against the same floating one.

`get(int)` without a path is kept for callers that do not know the location, and **skips
path entries** rather than silently evaluating them at the root.

## Adding a tag inside a sequence

`AddTag` accepts the same path syntax, with two restrictions that follow from writing
rather than matching.

**Literal paths only.** `(0040,0275).(0040,0009)` names one destination; `**.(0040,0009)`
names a set, and there is no answer to which one should be created. `TagPathPattern`
exposes this as `literalTags()`, empty when any segment is a wildcard, and the profile
fails to build with a message saying so.

**Always read from the root.** A path given to `AddTag` is the chain of sequences from the
top-level dataset, whatever the leading dot. The attribute has to land somewhere definite.

`AddInSequence extends Add` resolves the chain on the dataset it is executed on:

| Situation | What happens |
| --- | --- |
| sequence absent | created, with one item |
| sequence present but empty | given one item |
| sequence with several items | **every** item receives the attribute |
| item already holds the tag | left untouched, as `Add` does at the top level |
| tag held by a non-sequence value | nothing created, warning logged — overwriting would destroy data the profile was not asked to touch |

### Why it runs after the walk

A root-level `AddTag` fires while visiting the first tag, and what it adds is never
revisited: `dcm.tags()` is a snapshot taken when the loop starts. A nested add does not get
that for free. If the target sequence already exists, the walk descends into it *after* the
add, and every other profile item then runs over the freshly written value — so a profile
that adds `(0040,0009)` and also removes `(0040,0009)` would silently add nothing. Worse,
it would only misbehave when the sequence happened to pre-exist, since a sequence created
by the add is not in the root snapshot and is never descended into.

Sequence-targeting `AddTag` items are therefore pulled out of `tagProfiles` — exactly as
`CleanPixelData` already is — and applied once the walk is over:

```java
public void applyAction(Attributes dcm, Attributes original, HMAC hmac, ...) {
    this.applyAction(dcm, original, hmac, profilePassedInSequence, actionPassedInSequence, context, TagPath.ROOT);
    this.applyAddInSequence(dcm, original, hmac, context);
}
```

`applyAddInSequence` evaluates each item's optional condition against `original`, then
executes the action on the top-level dataset. Nothing is added to an instance a profile
item excluded (`context.getAbort() != NONE`).

### The SOP-class guard

`AddTag` has always refused to add an attribute that is not part of the SOP class of the
instance. The standard keys its module attributes by colon-separated path
(`ModuleToAttributes`), so the check extends to a nested destination for free — the path is
translated to the form the standard writes:

```
(0040,0275).(0040,0009)  ->  00400275:00400009
```

The match is **exact, not by suffix**, and deliberately so. A suffix match would accept
`(0008,0121).(0008,010F)` for a SOP class where the standard only defines it at
`00400555:004008ea:00080121:0008010f`, and the add would then create the sequence at the
root — a non-conformant object.

Validation additionally rejects an enclosing tag that is not `SQ`, so
`(0010,0010).(0040,0009)` fails with "PatientName is not a sequence".

## Editor support

`TagPickerDialog` gained an "Apply the selected tags to" selector:

| Scope | Produces |
| --- | --- |
| Any level (default) | `(0020,0010)` |
| Root only | `.(0020,0010)` |
| Inside its sequence | `(0040,0275).(0020,0010)` |

The last one needs the hierarchy, which only the module browse knows — the standard gives
each module attribute a colon-separated path — so it stays disabled while the grid shows
search results, and switching back to search resets the scope. An "In sequence" column
names the enclosing sequences of each row. Paths built this way are floating.

`TagPickerField` gained a free-text entry validated on Add / Enter, the only way to reach
`*` and `**`, which the dictionary cannot express. Chips name every segment of a path
(`RequestAttributesSequence › StudyID`, `root › StudyID` for an anchored tag).

How much of the grammar an element accepts is `TagPickerField.PathMode`:

| Mode | Used by | Accepts |
| --- | --- | --- |
| `ANY` | `ActionTags`, `PrivateTags`, `ActionDates`, `ReplaceUID` | the whole grammar, wildcards included |
| `LITERAL` | `AddTag` | paths naming every sequence exactly |
| `NONE` | — | flat tags only |

The value-shaping logic lives in `TagScopes` rather than in the widget, so it is testable
without a UI. Save-time validation needed no new wiring: `ProfilePipeService.validateElement`
instantiates the profile class, so `profileValidation()` surfaces in the editor's error
label.

## Authoring outside the editor

`ProfilePipeService` passes YAML tag strings through verbatim, so a path can be authored
in a profile file:

```yaml
profileElements:
  - name: "Remove study id in request attributes"
    codename: "action.on.specific.tags"
    action: "X"
    tags:
      - "(0040,0275).(0020,0010)"

  - name: "Add scheduled step id in request attributes"
    codename: "action.add.tag"
    arguments:
      value: "STEP-1"
    tags:
      - "(0040,0275).(0040,0009)"
```

A path stored this way is displayed correctly by the editor even on an element whose
`PathMode` would not let one be typed.

## Tests

| Class | Covers |
| --- | --- |
| `TagPathPatternTest` | the grammar: anchoring, floating, `*` / `**`, `X` segments, specificity, parse failures |
| `TagActionMapTest.PathScoping` | resolution order, path over flat tag, most specific wins, unknown location |
| `ProfileSequenceScopingTest` | end to end on a dataset holding StudyID at the root and in two different sequences, one test per form |
| `AddTagInSequenceTest` | writing into an existing item, creating the sequence, filling every item, leaving an existing value, the SOP-class refusal, the add surviving a remove rule in the same profile, and the four validation refusals |
| `TagScopesTest` | the picker's value shaping, plus every nested attribute of the real standard run through the grammar |
| `TagPickerFieldTest` | path round-trip, multi-segment chip label, both dialog variants build |

## Known, not addressed

- **`AddPrivateTag` still rejects paths** (`rejectTagPaths()`). It shares the latch
  structure but not the problem: private attributes inside sequence items bring
  private-creator reservation per item, which is separate work.
- **`UpdateUIDsProfile` ignores its configured tags entirely** — its `tagMap` has no caller
  in main code, so `getAction` always returns `null`. Pre-existing and unrelated, but it
  means the tag list on the "Replace UIDs" element has no effect, scoped or not.
- **A path is never inferred.** An existing profile using a bare tag keeps matching at every
  depth; nothing rewrites stored values.