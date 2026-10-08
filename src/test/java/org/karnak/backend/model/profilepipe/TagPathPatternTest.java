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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dcm4che3.data.Tag;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class TagPathPatternTest {

	/** The location of ScheduledProcedureStepID inside RequestAttributesSequence. */
	private static final int[] IN_REQUEST_ATTRIBUTES = { Tag.RequestAttributesSequence, Tag.ScheduledProcedureStepID };

	/** The same tag two sequences deep. */
	private static final int[] TWO_LEVELS_DEEP = { Tag.ReferencedStudySequence, Tag.RequestAttributesSequence,
			Tag.ScheduledProcedureStepID };

	private static final int[] AT_ROOT = { Tag.ScheduledProcedureStepID };

	@Nested
	class IsPath {

		@Test
		void recognises_a_dotted_value_as_a_path() {
			assertTrue(TagPathPattern.isPath("(0040,0275).(0040,0009)"));
		}

		@Test
		void recognises_a_leading_dot_as_a_path() {
			assertTrue(TagPathPattern.isPath(".(0040,0009)"));
		}

		@Test
		void does_not_treat_a_flat_tag_as_a_path() {
			assertFalse(TagPathPattern.isPath("(0040,0009)"));
		}

		@Test
		void does_not_treat_a_flat_wildcard_pattern_as_a_path() {
			assertFalse(TagPathPattern.isPath("0040XXXX"));
		}

	}

	@Nested
	class Anchoring {

		@Test
		void an_anchored_tag_matches_only_at_the_root() {
			TagPathPattern pattern = TagPathPattern.parse(".(0040,0009)");

			assertTrue(pattern.matches(AT_ROOT));
			assertFalse(pattern.matches(IN_REQUEST_ATTRIBUTES));
		}

		@Test
		void an_anchored_path_requires_the_sequence_to_be_at_the_root() {
			TagPathPattern pattern = TagPathPattern.parse(".(0040,0275).(0040,0009)");

			assertTrue(pattern.matches(IN_REQUEST_ATTRIBUTES));
			assertFalse(pattern.matches(TWO_LEVELS_DEEP));
		}

		@Test
		void a_floating_path_matches_the_sequence_at_any_depth() {
			TagPathPattern pattern = TagPathPattern.parse("(0040,0275).(0040,0009)");

			assertTrue(pattern.matches(IN_REQUEST_ATTRIBUTES));
			assertTrue(pattern.matches(TWO_LEVELS_DEEP));
			assertFalse(pattern.matches(AT_ROOT));
		}

	}

	@Nested
	class Wildcards {

		@Test
		void a_single_star_matches_exactly_one_level() {
			TagPathPattern pattern = TagPathPattern.parse("*.(0040,0009)");

			assertTrue(pattern.matches(IN_REQUEST_ATTRIBUTES));
			assertFalse(pattern.matches(AT_ROOT));
			// Floating, so the single level may itself be nested
			assertTrue(pattern.matches(TWO_LEVELS_DEEP));
		}

		@Test
		void an_anchored_single_star_pins_the_nesting_level() {
			TagPathPattern pattern = TagPathPattern.parse(".*.(0040,0009)");

			assertTrue(pattern.matches(IN_REQUEST_ATTRIBUTES));
			assertFalse(pattern.matches(TWO_LEVELS_DEEP));
			assertFalse(pattern.matches(AT_ROOT));
		}

		@Test
		void a_double_star_matches_any_depth_except_the_root() {
			TagPathPattern pattern = TagPathPattern.parse(".**.(0040,0009)");

			assertTrue(pattern.matches(IN_REQUEST_ATTRIBUTES));
			assertTrue(pattern.matches(TWO_LEVELS_DEEP));
			assertFalse(pattern.matches(AT_ROOT));
		}

		@Test
		void a_trailing_star_matches_any_tag_of_the_sequence() {
			TagPathPattern pattern = TagPathPattern.parse("(0040,0275).*");

			assertTrue(pattern.matches(IN_REQUEST_ATTRIBUTES));
			assertTrue(pattern.matches(new int[] { Tag.RequestAttributesSequence, Tag.StudyID }));
			assertFalse(pattern.matches(AT_ROOT));
		}

		@Test
		void a_segment_accepts_the_x_wildcard_of_flat_patterns() {
			TagPathPattern pattern = TagPathPattern.parse("(0040,0275).0040XXXX");

			assertTrue(pattern.matches(IN_REQUEST_ATTRIBUTES));
			assertFalse(pattern.matches(new int[] { Tag.RequestAttributesSequence, Tag.PatientName }));
		}

	}

	@Nested
	class Specificity {

		@Test
		void a_named_sequence_beats_a_wildcard() {
			assertTrue(TagPathPattern.parse("(0040,0275).(0040,0009)")
				.specificity() > TagPathPattern.parse("*.(0040,0009)").specificity());
		}

		@Test
		void an_anchored_pattern_beats_the_same_floating_pattern() {
			assertTrue(TagPathPattern.parse(".(0040,0275).(0040,0009)")
				.specificity() > TagPathPattern.parse("(0040,0275).(0040,0009)").specificity());
		}

	}

	@Nested
	class Parsing {

		@Test
		void ignores_the_parentheses_and_spaces_of_a_segment() {
			assertTrue(TagPathPattern.parse(" ( 0040 , 0275 ) . ( 0040 , 0009 ) ").matches(IN_REQUEST_ATTRIBUTES));
		}

		@Test
		void keeps_the_segments_in_the_written_order() {
			assertEquals(2, TagPathPattern.parse("(0040,0275).(0040,0009)").segments().size());
		}

		@Test
		void rejects_a_trailing_separator() {
			assertThrows(IllegalArgumentException.class, () -> TagPathPattern.parse("(0040,0275)."));
		}

		@Test
		void rejects_an_empty_segment() {
			assertThrows(IllegalArgumentException.class, () -> TagPathPattern.parse("(0040,0275)..(0040,0009)"));
		}

		@Test
		void rejects_a_segment_that_is_not_a_tag() {
			assertThrows(IllegalArgumentException.class, () -> TagPathPattern.parse("(0040,0275).notATag"));
		}

		@Test
		void reports_an_unparseable_path_as_invalid() {
			assertFalse(TagPathPattern.isValid("(0040,0275)."));
		}

		@Test
		void reports_a_flat_tag_as_not_a_valid_path() {
			assertFalse(TagPathPattern.isValid("(0040,0009)"));
		}

	}

}