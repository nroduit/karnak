/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ArgumentEntity;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ArgumentUtilTest {

	private static final List<ArgumentEntity> ARGS = List.of(new ArgumentEntity("days", "10"),
			new ArgumentEntity("label", "study"), new ArgumentEntity("bad", "not-a-number"));

	@Test
	void string_value_returns_the_matching_argument() {
		assertEquals("study", ArgumentUtil.stringValue(ARGS, "label", "default"));
	}

	@Test
	void string_value_returns_the_default_when_key_is_absent() {
		assertEquals("default", ArgumentUtil.stringValue(ARGS, "missing", "default"));
	}

	@Test
	void int_value_parses_the_matching_argument() {
		assertEquals(10, ArgumentUtil.intValue(ARGS, "days", -1));
	}

	@Test
	void int_value_returns_the_default_when_key_is_absent() {
		assertEquals(-1, ArgumentUtil.intValue(ARGS, "missing", -1));
	}

	@Test
	void int_value_returns_the_default_when_value_is_not_a_number() {
		assertEquals(7, ArgumentUtil.intValue(ARGS, "bad", 7));
	}

	@Test
	void parse_int_returns_the_default_for_null() {
		assertEquals(42, ArgumentUtil.parseInt(null, 42));
	}

	@Test
	void parse_int_returns_the_default_for_a_non_number() {
		assertEquals(42, ArgumentUtil.parseInt("abc", 42));
	}

	@Test
	void parse_int_parses_a_valid_number() {
		assertEquals(-15, ArgumentUtil.parseInt("-15", 0));
	}

}