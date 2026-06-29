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
import org.jspecify.annotations.NullUnmarked;

/**
 * A single node of the conditional-requirement predicate DSL, deserialized from the
 * curated rules file. A node is either a leaf (a {@code tag} plus exactly one operator)
 * or a boolean composite ({@code allOf}/{@code anyOf}). The fields are populated by Gson
 * reflection; only the relevant subset is set for any given node.
 *
 * <p>
 * Leaf operators on the referenced {@code tag} (lowercase 8-digit hex):
 * <ul>
 * <li>{@code present} — true: tag present with a value; false: tag absent.</li>
 * <li>{@code equals} — the tag holds this exact (trimmed) value.</li>
 * <li>{@code in} — the tag holds one of these values.</li>
 * <li>{@code notIn} — the tag holds none of these values.</li>
 * </ul>
 */
@NullUnmarked
public class Condition {

	private String tag;

	private Boolean present;

	private String equals;

	private List<String> in;

	private List<String> notIn;

	private List<Condition> allOf;

	private List<Condition> anyOf;

	public String getTag() {
		return tag;
	}

	public Boolean getPresent() {
		return present;
	}

	public String getEquals() {
		return equals;
	}

	public List<String> getIn() {
		return in;
	}

	public List<String> getNotIn() {
		return notIn;
	}

	public List<Condition> getAllOf() {
		return allOf;
	}

	public List<Condition> getAnyOf() {
		return anyOf;
	}

}
