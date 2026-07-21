/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profilepipe;

import org.dcm4che3.data.Tag;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.karnak.backend.model.action.Keep;
import org.karnak.backend.model.action.Remove;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayNameGeneration(ReplaceUnderscores.class)
class TagActionMapTest {

	@Nested
	class IsValidPattern {

		@Test
		void accepts_8_char_pattern_containing_x() {
			assertTrue(TagActionMap.isValidPattern("0010XXXX"));
		}

		@Test
		void is_case_insensitive_for_the_x_wildcard() {
			assertTrue(TagActionMap.isValidPattern("0010xxxx"));
		}

		@Test
		void rejects_pattern_without_wildcard() {
			assertFalse(TagActionMap.isValidPattern("00100010"));
		}

		@Test
		void rejects_pattern_of_wrong_length() {
			assertFalse(TagActionMap.isValidPattern("0010XXX"));
		}

		@Test
		void rejects_pattern_with_non_hex_character() {
			assertFalse(TagActionMap.isValidPattern("0010XXXG"));
		}

		@Test
		void rejects_null_or_blank() {
			assertFalse(TagActionMap.isValidPattern(null));
			assertFalse(TagActionMap.isValidPattern(""));
		}

	}

	@Nested
	class GetMask {

		@Test
		void turns_wildcards_into_zero_and_fixed_chars_into_f() {
			assertEquals("FFFF0000", TagActionMap.getMask("0010XXXX"));
		}

		@Test
		void handles_interleaved_wildcards() {
			assertEquals("F0F0F0F0", TagActionMap.getMask("0X1X2X3X"));
		}

	}

	@Nested
	class PutAndGet {

		@Test
		void resolves_an_exact_tag_with_parentheses_notation() {
			TagActionMap map = new TagActionMap();
			Keep keep = new Keep("K");

			map.put("(0010,0010)", keep);

			assertSame(keep, map.get(Tag.PatientName));
		}

		@Test
		void resolves_a_tag_matching_a_wildcard_pattern() {
			TagActionMap map = new TagActionMap();
			Remove remove = new Remove("X");

			// Matches any tag in group 0010.
			map.put("0010XXXX", remove);

			assertSame(remove, map.get(Tag.PatientName));
			assertSame(remove, map.get(Tag.PatientID));
		}

		@Test
		void prefers_an_exact_match_over_a_pattern_match() {
			TagActionMap map = new TagActionMap();
			Keep exact = new Keep("K");
			Remove pattern = new Remove("X");

			map.put("0010XXXX", pattern);
			map.put("(0010,0010)", exact);

			assertSame(exact, map.get(Tag.PatientName));
			assertSame(pattern, map.get(Tag.PatientID));
		}

		@Test
		void returns_null_when_no_entry_matches() {
			TagActionMap map = new TagActionMap();
			map.put("0010XXXX", new Keep("K"));

			assertNull(map.get(Tag.StudyDate));
		}

		@Test
		void ignores_spaces_in_an_exact_tag() {
			TagActionMap map = new TagActionMap();
			Keep keep = new Keep("K");

			map.put("(0010, 0010)", keep);

			assertSame(keep, map.get(Tag.PatientName));
		}

		@Test
		void ignores_surrounding_and_inner_spaces_in_an_exact_tag() {
			TagActionMap map = new TagActionMap();
			Keep keep = new Keep("K");

			map.put("  ( 0010 , 0010)  ", keep);

			assertSame(keep, map.get(Tag.PatientName));
		}

		@Test
		void ignores_spaces_in_a_wildcard_pattern() {
			TagActionMap map = new TagActionMap();
			Remove remove = new Remove("X");

			map.put("(0010, XXXX)", remove);

			assertSame(remove, map.get(Tag.PatientName));
		}

	}

	@Nested
	class SizeAndEmptiness {

		@Test
		void is_empty_when_no_entry_was_added() {
			TagActionMap map = new TagActionMap();

			assertTrue(map.isEmpty());
			assertEquals(0, map.size());
		}

		@Test
		void counts_both_exact_and_pattern_entries() {
			TagActionMap map = new TagActionMap();
			map.put("(0010,0010)", new Keep("K"));
			map.put("0010XXXX", new Remove("X"));

			assertFalse(map.isEmpty());
			assertEquals(2, map.size());
		}

	}

}
