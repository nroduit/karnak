/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.enums;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * The de-identification actions that can be applied to a tag, as a stable mapping between
 * the symbol persisted on a profile element ({@code action} column) and a human label for
 * the UI. The symbols are exactly the ones understood by
 * {@code AbstractAction.convertAction}.
 */
@Getter
public enum DeidActionType {

	KEEP("K", "Keep"), REMOVE("X", "Remove"), REPLACE_NULL("Z", "Replace with null"),
	REPLACE_DUMMY("D", "Replace with a dummy value"), DEFAULT_DUMMY("DDum", "Replace with the default dummy value"),
	NEW_UID("U", "Generate a new UID");

	private final String symbol;

	private final String label;

	DeidActionType(String symbol, String label) {
		this.symbol = symbol;
		this.label = label;
	}

	/** Resolve the action matching the given symbol, or {@code null} when unknown. */
	public static @Nullable DeidActionType fromSymbol(String symbol) {
		if (symbol == null) {
			return null;
		}
		for (DeidActionType type : values()) {
			if (type.symbol.equals(symbol)) {
				return type;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return label;
	}

}
