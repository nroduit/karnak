/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Color;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ExcludedTagEntity;
import org.karnak.backend.data.entity.IncludedTagEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.exception.ProfileException;
import org.karnak.backend.model.action.Remove;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ActionTagsTest {

	private static ProfileElementEntity element(String action) {
		return new ProfileElementEntity("name", "action.on.specific.tags", null, action, null, 0, null);
	}

	private static ActionTags removeOnPatientName() throws ProfileException {
		ProfileElementEntity element = element("X");
		element.addIncludedTag(new IncludedTagEntity("(0010,0010)", element));
		return new ActionTags(element);
	}

	@Nested
	class GetAction {

		@Test
		void returns_the_configured_action_for_an_included_tag() throws ProfileException {
			ActionTags profile = removeOnPatientName();

			assertInstanceOf(Remove.class,
					profile.getAction(new Attributes(), new Attributes(), Tag.PatientName, null));
		}

		@Test
		void returns_null_for_a_tag_that_is_not_included() throws ProfileException {
			ActionTags profile = removeOnPatientName();

			assertNull(profile.getAction(new Attributes(), new Attributes(), Tag.StudyDate, null));
		}

		@Test
		void returns_null_for_an_excepted_tag() throws ProfileException {
			ProfileElementEntity element = element("X");
			element.addIncludedTag(new IncludedTagEntity("(0010,0010)", element));
			element.addExceptedtags(new ExcludedTagEntity("(0010,0010)", element));
			ActionTags profile = new ActionTags(element);

			assertNull(profile.getAction(new Attributes(), new Attributes(), Tag.PatientName, null));
		}

	}

	@Nested
	class Validation {

		@Test
		void rejects_missing_action_and_missing_tags() {
			assertThrows(ProfileException.class, () -> new ActionTags(element(null)));
		}

		@Test
		void rejects_missing_action_when_tags_are_present() {
			ProfileElementEntity element = element(null);
			element.addIncludedTag(new IncludedTagEntity("(0010,0010)", element));

			assertThrows(ProfileException.class, () -> new ActionTags(element));
		}

		@Test
		void rejects_action_without_any_tag() {
			assertThrows(ProfileException.class, () -> new ActionTags(element("X")));
		}

		@Test
		void rejects_an_unknown_action_symbol() {
			// "DDum" was retired in favour of "D": an unknown symbol must fail at build
			// time.
			ProfileElementEntity element = element("DDum");
			element.addIncludedTag(new IncludedTagEntity("(0010,0010)", element));

			ProfileException e = assertThrows(ProfileException.class, () -> new ActionTags(element));
			assertEquals(
					"Cannot build the profile action.on.specific.tags: Unknown Action 'DDum', only K (keep), X (remove),"
							+ " Z (replace with null), D (replace with a dummy value) and U (new UID) are allowed",
					e.getMessage());
		}

	}

	@Nested
	class ColorConversion {

		@Test
		void converts_color_to_hexadecimal_without_alpha() {
			assertEquals("ff0000", ActionTags.color2Hexadecimal(Color.RED, false));
		}

		@Test
		void converts_color_to_hexadecimal_with_alpha() {
			assertEquals("ffff0000", ActionTags.color2Hexadecimal(Color.RED, true));
		}

		@Test
		void parses_a_six_digit_hexadecimal_into_a_color() {
			Color color = ActionTags.hexadecimal2Color("ff0000");

			assertEquals(255, color.getRed());
			assertEquals(0, color.getGreen());
			assertEquals(0, color.getBlue());
		}

		@Test
		void parses_an_eight_digit_hexadecimal_into_a_color() {
			Color color = ActionTags.hexadecimal2Color("ffff0000");

			assertEquals(255, color.getRed());
			assertEquals(255, color.getAlpha());
		}

		@Test
		void falls_back_to_black_on_an_invalid_hexadecimal() {
			Color color = ActionTags.hexadecimal2Color("not-a-color");

			assertEquals(0, color.getRed());
			assertEquals(0, color.getGreen());
			assertEquals(0, color.getBlue());
		}

	}

}