# `dcm` vs `original`, and the scope of a nested tag

This note documents the defect fixed by the current working tree and the changes it
required. It concerns the de-identification pipeline
(`org.karnak.backend.service.profilepipe.Profile`), the two `Attributes` it threads
through every profile item, and the way sequences are visited.

A second, independent part of the working tree cleans up `ExpressionResult`, the SpEL entry
point every condition and expression goes through — see
[Unrelated: `ExpressionResult`](#unrelated-expressionresult) at the end.

## Background: the two datasets

`Profile.apply` builds a snapshot before anything is modified:

```java
Attributes original = new Attributes(dcm);
applyAction(dcm, original, hmac, null, null, context);
```

The snapshot was called `dcmCopy` until this change; see
[step 8](#8-rename-dcmcopy-to-original) for why it was renamed. The snippets of pre-change
code below are quoted with the name they had at the time.

The two are not interchangeable:

| | `dcm` | `original` |
| --- | --- | --- |
| role | the object that will be forwarded | a read-only snapshot taken before the pipeline started |
| mutated | yes, in place | never |
| read for | the value of the tag currently visited, the current state of the object | anything else: conditions, expressions, referenced attributes |

The distinction matters because `applyAction` walks `dcm.tags()` in **ascending tag
order** and mutates as it goes. When the tag `(0010,0020)` is visited, every attribute
sorting before it has already been through the pipeline. So any decision that reads an
attribute *other* than the one being processed must read it from `original`, or the answer
depends on where that attribute sorts relative to the current tag — a silent,
order-dependent bug.

## Issue 1 — commit `991429a7` over-applied `dcm` in `ActionDates`

`991429a7 fix: shift date sequence` fixed a real problem: dates nested in a sequence were
being dropped, because the date options were reading the value to shift from `dcmCopy`
(the *root* copy) while the tag being visited lived in a sequence item, so
`dcmCopy.getString(tag)` returned `null` and the date was removed instead of shifted.

The fix was to pass `dcm` instead of `dcmCopy`:

```java
case "shift_by_tag"   -> ShiftByTagDate.shift(dcm, tag, argumentEntities, hmac);
case "shift_from_api" -> ShiftApiDate.shift(dcm, tag, argumentEntities, hmac);
```

That is correct for the *value being shifted* — `dcm` is indeed the only dataset holding
it at the right nesting level. But two of the five options also read **other** attributes,
and for those the swap moved the read onto the mutating dataset:

### `shift_by_tag`

The shift amounts come from two attributes named by the `days_tag` / `seconds_tag`
arguments. Read from `dcm`, a referenced tag sorting before the date has already been
de-identified. The old code then did:

```java
int shiftDays = ArgumentUtil.parseInt(dcm.getString(...), 0);
```

`parseInt` falls back to `0`, so the failure was **silent**: the attribute was emitted
with a zero shift, i.e. **the original date reached the destination**. That is strictly
worse than the pre-`991429a7` behaviour, where the date was dropped. A de-identification
component must not fail open.

### `shift_from_api`

The `url` / `body` arguments carry `{{expression}}` placeholders, typically the patient
identifier, resolved against the dataset passed in. Read from `dcm`:

- at the top level, the endpoint is queried with the **already pseudonymized**
  identifier, so the per-patient shift lookup returns the wrong record (or nothing);
- inside a sequence, `dcm` is the item, which holds no patient identifier at all — the
  placeholder resolved to `null` and `String.replaceFirst(regex, null)` threw an NPE that
  aborted the transfer.

## Issue 2 — sequence recursion passed the root copy

Independently of `ActionDates`, `applyAction` descended into sequences carrying the *root*
copy along:

```java
for (Attributes d : seq) {
    this.applyAction(d, dcmCopy, hmac, currentProfile, currentAction, context);
}
```

`d` moves down a level, `dcmCopy` does not. Every profile item evaluated for a nested tag
therefore received a mismatched pair, and everything reading it resolved against the top
level:

- `ExprAction` seeds its `stringValue` from the copy's `getString(tag)` — for a nested tag
  that is the *root's* value of that tag, or `null`. An expression like
  `stringValue.contains("...")` on a nested `SeriesDescription` silently tested the study's
  `SeriesDescription`.
- `ExprCondition` was built on the item but used flat lookups (`dcm.getString(tag)`), so
  the reverse hole existed too: a condition naming a study-level tag returned `null` once
  inside a sequence.
- `ExprAction.tagIsPresent` searched only downwards from the dataset it was given, so from
  inside a sequence it could not see the study.
- `ExprAction.ComputePatientAge()` read the patient/study modules from whatever dataset it
  was given — absent in an item.

Net effect: **no** tag of a sequence item was addressable from a condition or expression,
and study-level tags were addressable only by accident.

## Issue 3 — the once-per-instance latch read the mutating dataset

`AddTag` and `AddPrivateTag` are stateful: they must add their tag once per instance, and
they detect a new instance by watching the SOP Instance UID.

```java
String currentUID = dcm.getString(Tag.SOPInstanceUID);
if (!currentInstanceUID.equals(currentUID) && currentUID != null) {
    currentInstanceUID = currentUID;
    tagAdded = false;
}
```

`(0008,0018)` sorts near the start of the walk and any realistic profile replaces it
(`U`), so the latch key **changes mid-instance**: the tags before it see the original UID,
the tags after it see the pseudonymized one, and the latch reads that as a second
instance. Driving `AddPrivateTag.getAction` across a walk that replaces the UID on the
third tag returns a second `Add`:

```
tag=00080016 action=Add@5b6e8f77
tag=00080018 action=null
tag=00080020 action=Add@19f040ba     <- second Add, same instance
total adds for one instance = 2 (expected 1)
```

The forwarded object is unaffected — the second `Add` writes the same tag and value — so
this is a broken invariant rather than corrupted data, plus a duplicated warning in the
log. `AddTag` also read `(0008,0016)` from `dcm` to check that the tag belongs to the SOP
class; the confidentiality profile leaves the SOP Class UID alone, so that one is masked in
practice, but nothing stops a user profile from acting on it, and the tag would then be
silently not added.

## The change

### 1. Split the two roles in `ActionDates`

```java
case "shift_by_tag"   -> ShiftByTagDate.shift(dcm, original, tag, argumentEntities);
case "shift_from_api" -> ShiftApiDate.shift(dcm, original, tag, argumentEntities);
```

Both now take the value from `dcm` and everything else from the copy. The now-unused
`HMAC` parameter was dropped from both signatures.

### 2. Descend both datasets together

`Profile.applyAction`:

```java
Sequence originalSeq = original.getSequence(tag);
for (int i = 0; i < seq.size(); i++) {
    this.applyAction(seq.get(i), originalItem(originalSeq, i, original), hmac, currentProfile,
            currentAction, context);
}
```

`new Attributes(dcm)` deep-copies sequences in order, so the items match one to one.
`originalItem` falls back to the enclosing copy when they do not — a sequence *added* to
`dcm` by a profile item has no counterpart in the copy — which preserves the previous
behaviour rather than passing `null`.

### 3. Resolve tags outwards, not flat

Passing the item copy alone would fix nested lookups and break study-level ones. dcm4che
gives us the missing half: `Sequence.add` calls `setParent`, so every item can walk back up
to its root. New helper in `DicomObjectTools`:

```java
public static String getStringInScope(Attributes dcm, int tag) {
    for (Attributes level = dcm; level != null; level = level.getParent()) {
        String value = DicomUtils.getStringFromDicomElement(level, tag);
        if (value != null) {
            return value;
        }
    }
    return null;
}
```

Resolution is **nearest scope first, then outwards**, like a lexical scope chain. It does
*not* descend into sequences — an enclosing dataset never sees into its items.

Wired into:

- `ExprCondition` — all five predicates (`tagValueIsPresent`, `tagValueContains`,
  `tagValueBeginsWith`, `tagValueEndsWith`, `tagIsPresent`);
- `ExprAction.getString`;
- `ShiftByTagDate`, to resolve `days_tag` / `seconds_tag`.

`ExprAction.tagIsPresent` now searches from `getRoot()` (object-wide, since it is
documented as "is this tag anywhere in the object"), and `ComputePatientAge()` reads from
`getRoot()`, because the patient and study modules it derives from never live in an item.

### 4. Fail closed in `ShiftByTagDate`

`shift` now returns `Integer`/`null` rather than defaulting to `0`:

- argument **not configured** → `0`, i.e. no shift on that unit (unchanged);
- argument configured but the tag is **absent** or **not a number** → `null`, logged as a
  warning.

`null` propagates up to `ActionDates`, which leaves the attribute to the following profile
items — typically removed by the basic DICOM profile. **This is a deliberate behaviour
change**: a misconfigured `shift_by_tag` used to emit the unshifted date, it now drops it.

### 5. Same split in `ReplaceApi`

`ReplaceApi` builds its endpoint call the same way `shift_from_api` does, and had the same
defect: `parseArguments(dcm)` resolved the `{{...}}` placeholders of the `url` and `body`
against the dataset being de-identified. It now takes `original`:

```java
ApiArguments args = parseArguments(original);
```

The consequence was the more damaging of the two, because the placeholder is essentially
always the patient identifier: `(0010,0020)` sorts before most tags a profile replaces, so
the endpoint was queried with an **already pseudonymized** identifier and the per-patient
lookup — the entire point of the profile item — returned the wrong record or nothing. The
same call from inside a sequence resolved to nothing at all.

### 6. Harden `EndpointService.evaluateStringWithExpression`

Two defects in the placeholder substitution, both reachable from `shift_from_api` and
`ReplaceApi`:

- an expression evaluating to `null` was passed straight to `String.replaceFirst`, which
  throws an NPE. It is now replaced by an empty string and logged.
- the replacement string was not escaped, so a value containing `$` or `\` (a DICOM PN can
  contain neither, but a free-text description can) either threw
  `IllegalArgumentException` or injected a group reference. Now wrapped in
  `Matcher.quoteReplacement`.

### 7. Read the instance latch from the copy

In both `AddTag` and `AddPrivateTag`:

```java
String currentUID = original.getString(Tag.SOPInstanceUID);
```

The copy answers the same UID for the whole walk, which is what a per-instance latch needs.
`AddTag`'s SOP-class check moved to `original` for the same reason, and so did the
`SOPInstanceUID` in both warning logs — read from `dcm` they were non-deterministic,
naming the original or the pseudonymized instance depending on which tag was being visited.
Logging the source UID matches `Profile.putSourceMdc`, which records the pre-pipeline
identifiers.

One read deliberately stays on `dcm`:

```java
if (privateCreator != null && dcm.contains(creatorTag)
        && !dcm.getString(creatorTag).equals(privateCreator)) {
```

It asks which private creator the object *about to be forwarded* already holds — the
current state, which is exactly what the contract reserves `dcm` for.

### 8. Rename `dcmCopy` to `original`

`dcmCopy` describes *how* the snapshot is made, not *what it is for*, and so reads as "a
spare, interchangeable with `dcm`". That reading is the root of every defect above: commit
`991429a7` swapped one for the other, `ReplaceApi` passed the wrong one, and the `Add*`
latch watched the mutating dataset. A name that says **original** makes
`original.getString(tag)` look wrong at a glance wherever the current value was wanted.

70 occurrences across 14 files, compiler-verified, no behaviour change.

`dcm` was deliberately left alone. It is not the misleading half, and it is a project-wide
idiom — 292 occurrences across 48 files, most of them outside the pipeline
(`ForwardService`, `DicomObjectTools`, `AttributesByDefault`). Renaming it would churn
blame across unrelated code for little gain.

Three parameters introduced earlier in this same working tree were named `context`
(`ShiftByTagDate.shift`, `ShiftApiDate.shift`, `ReplaceApi.parseArguments`); they were
renamed too. `context` collides with `AttributeEditorContext context`, already a parameter
of `applyAction`. Also renamed for consistency: `seqCopy` → `originalSeq`, and
`originalItem`'s `sequenceCopy` / `enclosingCopy` → `originalSequence` /
`enclosingOriginal`.

Names considered and rejected: `source` (collides with a first-class Karnak concept —
`DicomSourceNodeEntity`, `SourceNodeService`, "a source fans out to destinations") and
`snapshot` (suggests a point in time that might be refreshed; this one is taken once).

The rename does not replace the javadoc. The ascending-tag-order hazard — *every attribute
sorting before the current tag may already be de-identified* — is the part that explains
why the two differ, and no identifier can carry it.

### 9. Documentation

Javadoc was added on the API that this contract runs through, since none of it was
discoverable from the signatures:

- `ProfileItem.getAction` — the `dcm` / `original` contract, the ascending-order hazard, and
  the fact that both are at the same nesting level;
- `Profile.applyAction` and `Profile.originalItem` — the joint descent;
- `DicomObjectTools.getStringInScope` — the scope chain;
- `ActionDates.applyOption`, `ShiftByTagDate.shift` (+ `resolveShiftAmount`),
  `ShiftApiDate.shift`, `ReplaceApi.parseArguments` — which dataset each parameter is and
  why;
- `AddTag` / `AddPrivateTag` — why the latch reads the copy, and why the private-creator
  check does not;
- `ExprCondition` and `ExprAction` class-level javadoc;
- `EndpointService.evaluateStringWithExpression`.

While documenting `ExprCondition`, one inconsistency surfaced and was fixed: the
`tagIsPresent(String)` overload called `dcm.getString` directly instead of delegating to
`tagIsPresent(int)`, so it alone would have stayed non-scoped.

`ShiftApiDate` also gained `@NullUnmarked` / `@Nullable` per `docs/nullness-jspecify.md`,
and the formatter fixed pre-existing space-indented lines in it.

## Consistency audit

All twelve `ProfileItem` implementations and the expression path were reviewed against the
contract above. State after the change:

| Profile item | Reads | Verdict |
| --- | --- | --- |
| `ActionTags`, `PrivateTags`, `UpdateUIDsProfile` | neither — pure tag-map lookup | consistent |
| `CleanPixelData`, `Defacing` | neither — `return null`, they act on the pixels | consistent |
| `BasicProfile` | passes both through to its sub-items | consistent |
| `Expression` | `new ExprAction(tag, dcm.getVR(tag), original)` | consistent — the VR of the current tag from `dcm`, everything else from the copy |
| `ActionDates` | value from `dcm`, referenced tags from `original` | fixed, issue 1 |
| `ReplaceApi` | `parseArguments(original)` | fixed, issue 1 |
| `AddTag` | latch and SOP class from `original` | fixed, issue 3 |
| `AddPrivateTag` | latch from `original`, private creator from `dcm` | fixed, issue 3 |

`Profile` itself is consistent: `ExprCondition` is built on `original`, and the mask and
defacing lookups (`StationName`, `Columns`, `Rows`) read `original`.

One theoretical divergence is left alone: `ExprAction` seeds its `stringValue` from
`original.getString(tag)` while the contract says the value of the tag itself comes from
`dcm`. The two agree at the moment `getAction` is called, because the current tag has not
been touched yet — unless an earlier profile item in the same chain executed an `Add` on
it, which `Profile.applyAction` does inline before continuing the chain. Reaching that
requires an `AddTag` and an `Expression` targeting the same tag, and the existing
`addTagThenIgnoreAction` test shows the chain stops before the expression anyway.

## Behaviour changes to be aware of

1. **Nearest scope wins.** A condition or expression naming a tag present at *both* the
   item and the study level now sees the **item's** value while visiting that item. This is
   the intended semantic, but it is a real change for any profile relying on the old
   behaviour (where it always saw the study's value, or `null`).
2. **`ExprAction.tagIsPresent` is now object-wide**, not subtree-wide.
3. **A misconfigured `shift_by_tag` now drops the date** instead of emitting it unshifted.
4. **An unresolvable `{{placeholder}}` now yields an empty string** instead of aborting the
   transfer with an NPE.
5. **`AddTag` / `AddPrivateTag` warnings now name the source SOP Instance UID**
   deterministically, where they previously named whichever UID the working dataset held at
   that point of the walk.

Items 1–2 widen what profiles can express; item 3 is the privacy-relevant one; item 4
trades a hard failure for a degraded request that is visible in the logs; item 5 only
affects log content.

## Tests

All new tests were verified to be discriminating: the source change was reverted, the
expected failures observed, then restored.

| File | What it covers |
| --- | --- |
| `backend/util/ShiftByTagDateTest` | reads the shift tags from the copy, not the de-identified dataset; nested date using top-level shift tags; fallback to the working dataset; `null` on an absent / non-numeric configured shift tag |
| `backend/util/DicomObjectToolsTest` (`ScopedLookup`) | own level, enclosing, deeply nested, nearest-scope-wins, does not descend into sequences, absent tag |
| `backend/service/EndpointServiceTest` | unresolved placeholder → empty string; regex special characters in the value; `null` / empty input |
| `profilepipe/ProfileTest` | `shift_by_tag` nested in a sequence; `shift_by_tag` whose shift tag was already de-identified (private group `0007`, which sorts *before* `StudyDate`); study-level condition still applies inside a sequence, and still does not when it does not match; item-level condition applies to that item only; expression on a nested tag reads its own item; expression on a nested tag still sees the study |
| `profilepipe/option/datemanager/ShiftApiDateTest` | URL template resolved on the copy; date nested in a sequence using the top-level copy for the URL; end-to-end through `Profile.applyAction` |
| `profilepipe/option/datemanager/ShiftByTagDateTest` | signatures updated; `shiftByBadTag` / `shiftByBadTag2` flipped to `assertNull` per the behaviour change above |
| `backend/model/profiles/ReplaceApiTest` | URL resolved on the copy when the working dataset already holds the pseudonymized identifier; same for a tag nested in a sequence |
| `backend/model/profiles/AddTagTest` | the tag is added once when the instance UID is replaced during the walk; the SOP class is read from the copy |
| `backend/model/profiles/AddPrivateTagTest` | the private tag is added once when the instance UID is replaced during the walk |

The two `Add*` latch tests drive `getAction` tag by tag rather than going through
`Profile`: re-adding the same tag with the same value is idempotent, so the defect is
invisible in the resulting dataset and only the number of actions returned exposes it.

Full suite: **1559 tests, 0 failures, 0 errors** (including the `ExpressionResult` tests
below).

## Also in the working tree

- `pom.xml`: `weasis-dicom-tools` `5.34.3.2` → `5.34.3.3`. Unrelated to this work, it was
  already modified before it started.

## How a profile targets a tag inside a sequence

Since the question comes up naturally from the above, here is what the pipeline actually
supports.

**There is no path syntax.** `TagActionMap.put` accepts only an 8-hexadecimal-digit tag,
with `(`, `)`, `,` and whitespace stripped, optionally using `X` as a wildcard digit
(`0009XX10` matches that element in every private group `0009xx`). `RequestAttributesSequence.StudyDescription`
is not a thing.

Instead, **a profile item is matched by tag value at every nesting level**. `applyAction`
recurses into each item of each sequence and re-runs the whole profile on it, so an
`action.on.specific.tags` item including `(0008,1030)` hits `StudyDescription` wherever it
appears — top level, in an item, three levels down. This is why the joint descent above
matters: without it, the item was visited but every decision was read from the top level.

Three ways to control the scope.

### 1. Name the leaf tag — it applies at every level

Nothing special to write. `(0008,1030)` below reaches `StudyDescription` at the top level
*and* the one inside `RequestAttributesSequence`, `ReferencedStudySequence`, or anywhere
else it occurs:

```yaml
profileElements:
  - name: "Replace study description"
    codename: "action.on.specific.tags"
    action: "D"          # X remove, Z empty, K keep, U new UID, D dummy, DDum default dummy
    tags:
      - "0008,1030"
```

The `tags` entries are matched by value only: `"0008,1030"`, `"(0008,1030)"` and
`"00081030"` are the same thing, and `x` / `X` is a wildcard digit (`"0009xx10"` matches
that element in every private group `0009xx`). `excludedTags` uses the same syntax.

### 2. Name the sequence tag — the action covers the whole subtree

When a profile item matches the sequence's own tag, its action is handed to the recursion
and applied to **every** tag of **every** item of that sequence:

```yaml
  - name: "Keep everything in the request attributes"
    codename: "action.on.specific.tags"
    action: "K"
    tags:
      - "0040,0275"      # RequestAttributesSequence
```

`X` (remove) and `Z` (empty) are the exception: they short-circuit the recursion and drop
the whole subtree in one step, which is the usual way to get rid of a sequence wholesale.

The propagated action is a *fallback*, not an override: inside the items, a profile item
placed earlier in the chain that claims the tag still wins. Only the tags no earlier item
matched fall through to it.

### 3. Restrict with a condition

Conditions are evaluated per visited tag, against the copy at that nesting level, and
resolve outwards. An item-level tag gates the item only; a study-level tag gates
everything, nested tags included:

```yaml
  - name: "Replace the description of the requested procedure only"
    codename: "action.on.specific.tags"
    condition: "tagValueIsPresent(#Tag.RequestedProcedureID, \"RP-1\")"
    action: "D"
    tags:
      - "0008,1030"
```

Applied to a study whose `RequestAttributesSequence` item carries `RequestedProcedureID`,
this replaces the item's `StudyDescription` and leaves the study's own untouched — the
condition is false while the top level is being walked, because `RequestedProcedureID`
lives only in the item.

Swap it for `tagValueIsPresent(#Tag.Modality, "CT")` and it becomes a study-level gate that
holds at every level, since the item resolves `Modality` through its parent.

This item-level form is the capability the fix above unlocked: before it, a condition
naming a tag of the item could never be true.

### What is still not expressible

"This tag, but only when nested" (or only at the top level) has no direct form — there is
no `isRoot()` / `getLevel()` predicate exposed to conditions. The workaround is (3): gate
on a tag only the item carries. Adding such a predicate to `ExprCondition` would be a
small, self-contained change if a profile ever needs it.

## Unrelated: `ExpressionResult`

The rest of the working tree is a separate cleanup of `ExpressionResult`, the SpEL entry
point every condition and expression above goes through. It fixes no de-identification
defect; it makes the failures readable and the hot path cheaper.

### The two methods were copies of each other

`get` and `isValid` each built a parser, an evaluation context, registered the `#Tag` and
`#VR` variables and parsed — the same eight lines twice. Both now call one private
`evaluate`, and the parser is a single static instance instead of one per call
(`SpelExpressionParser` is stateless and thread safe). `isValid` also dropped an unused
`Object o = exp.getValue(...)`.

### `isValid` reported failures badly

Four defects, all visible to whoever is writing a profile:

- **Blank input went through SpEL.** `null`, `""` and `"   "` reached the parser and came
  back as an exception whose message could itself be `null`, so the user was shown
  `Expression is not valid: \n\rnull`. Blank is now answered directly with
  `Expression is not valid: it is empty`.
- **A `null` exception message printed as the literal `"null"`.** It now falls back to
  `e.toString()`, which always names the exception class.
- **The root cause was dropped.** For the most common mistake — a condition that does not
  evaluate to a boolean — the SpEL message alone says only which types could not be
  converted. The cause says why, and is now appended:

  ```
  Expression is not valid:
  EL1001E: Type conversion problem, cannot convert from java.lang.String to java.lang.Boolean
  Caused by: java.lang.IllegalArgumentException: Invalid boolean value 'hello'
  ```

  A parse error has no cause and stays single-line.
- **`\n\r` was reversed**, and is now `%n`.

The prefix is now the constant `ExpressionResult.INVALID_EXPRESSION`.

### The prefix was applied twice

Four call sites prepended `"Expression is not valid: \n\r"` to a message that already
started with it, which is why a bad profile expression reported:

```
Expression is not valid:
Expression is not valid:
Expression [getString(#Tag.PatientID] @9: EL1051E: Unexpectedly ran out of arguments
```

`Expression`, `ReplaceApi` and both sites in `ShiftApiDate` now propagate `getMsg()` as is.

In `AbstractProfileItem.validateCondition`, the `condition != null` guard was applied
*after* calling `isValid`, so a null condition was pushed through SpEL and its result
discarded. It returns early now.

### Parsed expressions are cached

```java
static Expression parse(String condition) {
    Expression parsed = PARSED_EXPRESSIONS.get(condition);
    if (parsed != null) {
        return parsed;
    }
    parsed = PARSER.parseExpression(condition);
    if (PARSED_EXPRESSIONS.size() >= MAX_CACHED_EXPRESSIONS) {
        log.warn("More than {} distinct expressions have been parsed, the cache is emptied",
                MAX_CACHED_EXPRESSIONS);
        PARSED_EXPRESSIONS.clear();
    }
    PARSED_EXPRESSIONS.put(condition, parsed);
    return parsed;
}
```

A `ConcurrentHashMap` keyed by the expression source, used by `evaluate`, so `get` and
`isValid` both benefit. Reusing a parsed `Expression` across threads is what Spring itself
does (`CachedExpressionEvaluator`): the parsed form is immutable and holds nothing of the
object it was evaluated against.

The key space is bounded by the configuration — profile conditions, `expr` arguments, and
the `{{...}}` templates of `url` / `body` — never by patient data. The cap at 500 only
guards against an unforeseen source of distinct strings; reaching it empties the cache and
logs rather than growing without bound. An expression that fails to parse is not cached, so
a later call reports the failure again.

**Measured, not assumed.** 200 000 evaluations of a two-clause condition, three rounds:

```
parse-each-time 5814 ms | cached 5277 ms | speedup 1.1x
```

**About 10%** — not the order-of-magnitude win that "parse once, evaluate many" suggests,
because parsing is not the dominant cost, reflective evaluation is. Worth the fifteen
lines on a path that runs once per visited tag per profile item, but the javadoc says so
rather than overstating it.

### Tests

All in `backend/model/expression/ExpressionResultTest`:

| Block | What it covers |
| --- | --- |
| `IsValid` | blank input (`@NullSource` + `""` / `"   "`); the prefix appears exactly once; an unknown method is named in the message; a type-conversion failure; the root cause is appended; a parse error carries no cause |
| `ParsedExpressionCache` | the same string is parsed once (`assertSame`); distinct strings are kept apart; an invalid expression is not cached; the cache is emptied past the cap; **a cached expression is evaluated against the object of each call** — the same string returns `true` for a dataset holding `PatientName` and `false` for an empty one |

`clearCache()` and `cacheSize()` are package-private, for those tests.

### Left alone

`isValid` **evaluates**, it does not only parse — that is how the return type gets checked
against `typeOfReturn`. With a real (non-probe) `expressionItem`, a valid expression can
therefore still be reported invalid if it happens to throw on that particular data. Every
current caller passes an empty probe, so it does not bite today; splitting parse-checking
from type-checking would be the fix if it ever does.

## Known, not addressed

- `ShiftByTagDateTest`, `ShiftDateTest` and `ShiftRangeDateTest` each exist twice, under
  `org.karnak.backend.util` and `org.karnak.profilepipe.option.datemanager` (pre-existing
  duplication).