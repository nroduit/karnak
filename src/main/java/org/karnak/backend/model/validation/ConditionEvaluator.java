/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.validation;

import java.util.List;
import org.dcm4che3.data.Attributes;
import org.jspecify.annotations.Nullable;

/**
 * Evaluates a {@link Condition} predicate against a dataset using three-valued logic.
 *
 * <p>
 * The guiding rule is conservative: a condition resolves to {@link Ternary#TRUE} only
 * when it is definitely satisfied, to {@link Ternary#FALSE} only when it is definitely
 * not, and to {@link Ternary#UNKNOWN} whenever the attribute it depends on is itself
 * absent (so the requirement cannot be decided). Callers must never raise a finding on
 * {@code UNKNOWN}.
 */
public final class ConditionEvaluator {

	private ConditionEvaluator() {
	}

	/**
	 * Evaluates {@code condition} against {@code attrs}. A {@code null} condition is
	 * UNKNOWN.
	 */
	public static Ternary evaluate(Attributes attrs, Condition condition) {
		if (condition == null) {
			return Ternary.UNKNOWN;
		}
		if (condition.getAllOf() != null) {
			return evaluateAllOf(attrs, condition.getAllOf());
		}
		if (condition.getAnyOf() != null) {
			return evaluateAnyOf(attrs, condition.getAnyOf());
		}
		return evaluateLeaf(attrs, condition);
	}

	private static Ternary evaluateAllOf(Attributes attrs, List<Condition> conditions) {
		if (conditions.isEmpty()) {
			return Ternary.UNKNOWN;
		}
		boolean anyUnknown = false;
		for (Condition child : conditions) {
			Ternary result = evaluate(attrs, child);
			if (result == Ternary.FALSE) {
				return Ternary.FALSE;
			}
			if (result == Ternary.UNKNOWN) {
				anyUnknown = true;
			}
		}
		return anyUnknown ? Ternary.UNKNOWN : Ternary.TRUE;
	}

	private static Ternary evaluateAnyOf(Attributes attrs, List<Condition> conditions) {
		if (conditions.isEmpty()) {
			return Ternary.UNKNOWN;
		}
		boolean anyUnknown = false;
		for (Condition child : conditions) {
			Ternary result = evaluate(attrs, child);
			if (result == Ternary.TRUE) {
				return Ternary.TRUE;
			}
			if (result == Ternary.UNKNOWN) {
				anyUnknown = true;
			}
		}
		return anyUnknown ? Ternary.UNKNOWN : Ternary.FALSE;
	}

	private static Ternary evaluateLeaf(Attributes attrs, Condition condition) {
		Integer tag = parseTag(condition.getTag());
		if (tag == null) {
			return Ternary.UNKNOWN;
		}
		if (condition.getPresent() != null) {
			boolean present = attrs.containsValue(tag);
			return condition.getPresent() == present ? Ternary.TRUE : Ternary.FALSE;
		}
		if (condition.getEquals() != null) {
			return matchesAny(attrs, tag, List.of(condition.getEquals()));
		}
		if (condition.getIn() != null) {
			return matchesAny(attrs, tag, condition.getIn());
		}
		if (condition.getNotIn() != null) {
			Ternary inSet = matchesAny(attrs, tag, condition.getNotIn());
			return switch (inSet) {
				case TRUE -> Ternary.FALSE;
				case FALSE -> Ternary.TRUE;
				case UNKNOWN -> Ternary.UNKNOWN;
			};
		}
		return Ternary.UNKNOWN;
	}

	/**
	 * @return TRUE if any (trimmed) value of {@code tag} equals one of
	 * {@code candidates}, FALSE if the tag has a value but none match, UNKNOWN if the tag
	 * is absent/empty.
	 */
	private static Ternary matchesAny(Attributes attrs, int tag, List<String> candidates) {
		if (!attrs.containsValue(tag)) {
			return Ternary.UNKNOWN;
		}
		String[] values = attrs.getStrings(tag);
		if (values == null || values.length == 0) {
			return Ternary.UNKNOWN;
		}
		for (String value : values) {
			String trimmed = value == null ? "" : value.trim();
			if (candidates.contains(trimmed)) {
				return Ternary.TRUE;
			}
		}
		return Ternary.FALSE;
	}

	private static @Nullable Integer parseTag(String hexTag) {
		if (hexTag == null) {
			return null;
		}
		try {
			return (int) Long.parseLong(hexTag, 16);
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

}