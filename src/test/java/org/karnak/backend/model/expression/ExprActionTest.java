/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.karnak.backend.model.action.ExcludeInstance;
import org.karnak.backend.model.action.Keep;
import org.karnak.backend.model.action.Remove;
import org.karnak.backend.model.action.Replace;
import org.karnak.backend.model.action.ReplaceNull;
import org.karnak.backend.model.action.UID;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ExprActionTest {

	private static ExprAction onPatientName(String value) {
		Attributes dcm = new Attributes();
		if (value != null) {
			dcm.setString(Tag.PatientName, VR.PN, value);
		}
		return new ExprAction(Tag.PatientName, VR.PN, dcm);
	}

	@Test
	void rejects_a_null_vr() {
		assertThrows(NullPointerException.class, () -> new ExprAction(Tag.PatientName, null, "value"));
	}

	@Test
	void reads_the_string_value_from_the_copy_at_construction() {
		ExprAction action = onPatientName("Doe^John");

		assertEquals("Doe^John", action.getStringValue());
	}

	@Nested
	class IsHexTag {

		@Test
		void accepts_parenthesised_tag() {
			assertTrue(ExprAction.isHexTag("(0010,0010)"));
		}

		@Test
		void accepts_plain_8_char_hex() {
			assertTrue(ExprAction.isHexTag("00100010"));
		}

		@Test
		void accepts_wildcard_pattern() {
			assertTrue(ExprAction.isHexTag("0010XXXX"));
		}

		@Test
		void rejects_wrong_length() {
			assertFalse(ExprAction.isHexTag("001000"));
		}

		@Test
		void rejects_non_hex_characters() {
			assertFalse(ExprAction.isHexTag("0010ZZZZ"));
		}

	}

	@Nested
	class ActionFactories {

		private final ExprAction action = onPatientName("Doe^John");

		@Test
		void keep_returns_a_keep_action() {
			assertInstanceOf(Keep.class, action.Keep());
			assertEquals("K", action.Keep().getSymbol());
		}

		@Test
		void remove_returns_a_remove_action() {
			assertInstanceOf(Remove.class, action.Remove());
			assertEquals("X", action.Remove().getSymbol());
		}

		@Test
		void replace_returns_a_replace_action_carrying_the_dummy_value() {
			var replace = action.Replace("dummy");

			assertInstanceOf(Replace.class, replace);
			assertEquals("D", replace.getSymbol());
			assertEquals("dummy", replace.getDummyValue());
		}

		@Test
		void uid_returns_a_uid_action() {
			assertInstanceOf(UID.class, action.UID());
			assertEquals("U", action.UID().getSymbol());
		}

		@Test
		void replace_null_returns_a_replace_null_action() {
			assertInstanceOf(ReplaceNull.class, action.ReplaceNull());
			assertEquals("Z", action.ReplaceNull().getSymbol());
		}

		@Test
		void exclude_instance_returns_an_exclude_instance_action() {
			assertInstanceOf(ExcludeInstance.class, action.ExcludeInstance());
			assertEquals("E", action.ExcludeInstance().getSymbol());
		}

		@Test
		void compute_patient_age_returns_a_replace_action() {
			Attributes dcm = new Attributes();
			dcm.setString(Tag.PatientAge, VR.AS, "045Y");
			ExprAction ageAction = new ExprAction(Tag.PatientAge, VR.AS, dcm);

			var replace = ageAction.ComputePatientAge();

			assertInstanceOf(Replace.class, replace);
			assertEquals("D", replace.getSymbol());
		}

	}

	@Nested
	class DicomAccessors {

		@Test
		void get_string_reads_from_the_copy() {
			ExprAction action = onPatientName("Doe^John");

			assertEquals("Doe^John", action.getString(Tag.PatientName));
		}

		@Test
		void tag_is_present_reflects_the_copy_contents() {
			ExprAction action = onPatientName("Doe^John");

			assertTrue(action.tagIsPresent(Tag.PatientName));
			assertFalse(action.tagIsPresent(Tag.StudyDate));
		}

	}

}