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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class DeidActionTypeTest {

	@Test
	void exposes_the_symbol_and_label() {
		assertEquals("K", DeidActionType.KEEP.getSymbol());
		assertEquals("Keep", DeidActionType.KEEP.getLabel());
		assertEquals("U", DeidActionType.NEW_UID.getSymbol());
		assertEquals("Generate a new UID", DeidActionType.NEW_UID.getLabel());
	}

	@Test
	void to_string_returns_the_label() {
		assertEquals("Remove", DeidActionType.REMOVE.toString());
		assertEquals("Replace with null", DeidActionType.REPLACE_NULL.toString());
	}

	@Test
	void from_symbol_resolves_every_known_symbol() {
		for (DeidActionType type : DeidActionType.values()) {
			assertSame(type, DeidActionType.fromSymbol(type.getSymbol()));
		}
	}

	@Test
	void from_symbol_returns_null_for_unknown_or_null() {
		assertNull(DeidActionType.fromSymbol("does-not-exist"));
		assertNull(DeidActionType.fromSymbol(null));
	}

}