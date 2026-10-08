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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class SpecialCharacterTest {

	@Test
	void escapes_regex_metacharacters_with_a_backslash() {
		assertEquals("a\\.b", SpecialCharacter.escapeSpecialRegexChars("a.b"));
		assertEquals("\\(x\\)", SpecialCharacter.escapeSpecialRegexChars("(x)"));
	}

	@Test
	void leaves_plain_text_unchanged() {
		assertEquals("PatientName", SpecialCharacter.escapeSpecialRegexChars("PatientName"));
	}

	@Test
	void escaped_output_matches_the_literal_input_as_a_pattern() {
		String literal = "1.2.840+[test]";

		String escaped = SpecialCharacter.escapeSpecialRegexChars(literal);

		assertTrue(Pattern.compile(escaped).matcher(literal).matches());
	}

}