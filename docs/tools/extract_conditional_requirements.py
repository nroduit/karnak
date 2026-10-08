#!/usr/bin/env python3
# Copyright (c) 2026 Karnak Team and other contributors.
# SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
#
# One-off bootstrap helper (NOT part of the build). Mines the innolitics
# module_to_attributes.json for the small, unambiguous subset of Type 1C/2C
# "Required if ..." conditions that reference another DICOM attribute by tag and
# can be reduced to the curated condition DSL consumed by ConditionEvaluator.
#
# It deliberately handles only three safe sentence shapes and rejects anything
# with extra clauses, so its output is meant to be human-reviewed before being
# pasted into curated-validation-rules.json -> "conditionalRequirements".
#
#   python3 doc/tools/extract_conditional_requirements.py [--all-depths]
#
# Without --all-depths only top-level attributes are emitted (the condition then
# unambiguously references a top-level sibling, matching how the validator
# evaluates conditions against the full dataset).

import argparse
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(
    HERE, "..", "..", "src", "main", "resources",
    "org", "karnak", "backend", "model", "dicominnolitics",
    "module_to_attributes.json",
)

TAG = re.compile(r"\((\d{4}),\s*([0-9A-Fa-f]{4})\)")
TAGS = lambda s: TAG.findall(s)


def strip_html(text: str) -> str:
    return re.sub(r"\s+", " ", re.sub(r"<[^>]+>", " ", text or "")).strip()


def first_required_if(desc: str) -> str | None:
    text = strip_html(desc)
    m = re.search(r"Required if .*?\.(?:\s|$)", text)
    if not m:
        return None
    sentence = m.group(0).strip()
    # Drop a trailing "May be present otherwise." which does not change the rule
    return sentence


def hex_key(group: str, element: str) -> str:
    return (group + element).lower()


def parse_condition(sentence: str):
    """Return a DSL dict for the supported shapes, else None."""
    # Exactly one referenced tag, no boolean conjunctions we cannot model
    refs = TAGS(sentence)
    if len(refs) != 1:
        return None
    # Reject obvious multi-clause / negation / numeric comparisons
    lowered = sentence.lower()
    if any(tok in lowered for tok in (" and ", " not ", " greater ", " less ", " unless ",
                                      " equal to ", "value 1", "value of value")):
        return None
    tag = hex_key(*refs[0])

    # Shape A: "... (gggg,eeee) is present."
    if re.search(r"\)\s+is present\.?$", sentence) or re.search(r"\)\s+is sent\.?$", sentence):
        return {"tag": tag, "present": True}

    # Shape B: "... (gggg,eeee) is one of A, B or C." / "is A or B." / "is X." / "equals X."
    m = re.search(r"\)\s+(?:is|equals)\s+(?:one of\s+)?(.+?)\.?$", sentence)
    if not m:
        return None
    tail = m.group(1)
    # Values are uppercase enumerated tokens separated by , / or
    raw = re.split(r",|\bor\b", tail)
    values = []
    for v in raw:
        v = v.strip().strip('"').strip()
        if not v:
            continue
        # Accept only enumerated-looking tokens (UPPERCASE, digits, underscore, space)
        if not re.fullmatch(r"[A-Z0-9_][A-Z0-9_ ]*", v):
            return None
        values.append(v)
    if not values:
        return None
    if len(values) == 1:
        return {"tag": tag, "equals": values[0]}
    return {"tag": tag, "in": values}


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--all-depths", action="store_true")
    args = ap.parse_args()

    data = json.load(open(SRC))
    out = {}
    seen_sentences = set()
    skipped = 0
    for entry in data:
        if entry.get("type") not in ("1C", "2C"):
            continue
        path = entry["path"]
        colons = path.count(":")
        if not args.all_depths and colons != 1:
            continue
        if colons > 2:  # validator does not check deeper than one sequence level
            continue
        sentence = first_required_if(entry.get("description"))
        if not sentence:
            continue
        dsl = parse_condition(sentence)
        if dsl is None:
            skipped += 1
            continue
        module_id = entry["moduleId"]
        tag_path = ":".join(p for p in path.split(":") if p != module_id)
        key = f"{module_id}/{tag_path}"
        out[key] = {"requiredWhen": dsl, "_condition": sentence}
        seen_sentences.add(sentence)

    json.dump(out, sys.stdout, indent=2)
    sys.stdout.write("\n")
    sys.stderr.write(
        f"\nEmitted {len(out)} entries ({len(seen_sentences)} distinct conditions); "
        f"skipped {skipped} unparsed conditional attributes.\n"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())