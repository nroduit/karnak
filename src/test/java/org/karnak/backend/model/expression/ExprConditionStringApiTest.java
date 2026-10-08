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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ExprConditionStringApiTest {

	private static ExprCondition withPatientName(String value) {
		Attributes dcm = new Attributes();
		dcm.setString(Tag.PatientName, VR.PN, value);
		return new ExprCondition(dcm);
	}

	@Test
	void parses_a_parenthesised_hexadecimal_tag() {
		assertEquals(Tag.PatientName, ExprCondition.intFromHexString("(0010,0010)"));
		assertEquals(Tag.PatientName, ExprCondition.intFromHexString("00100010"));
	}

	@Test
	void default_constructor_starts_with_empty_attributes() {
		assertFalse(new ExprCondition().tagIsPresent(Tag.PatientName));
	}

	@Test
	void validates_an_expression_without_throwing() {
		ExprCondition.expressionValidation("tagIsPresent('00100010')");
	}

	@Nested
	class StringTagOverloads {

		@Test
		void tag_is_present() {
			assertTrue(withPatientName("Doe^John").tagIsPresent("(0010,0010)"));
			assertFalse(withPatientName("Doe^John").tagIsPresent("(0008,0020)"));
		}

		@Test
		void tag_value_is_present() {
			assertTrue(withPatientName("Doe^John").tagValueIsPresent("(0010,0010)", "Doe^John"));
			assertFalse(withPatientName("Doe^John").tagValueIsPresent("(0010,0010)", "Other"));
		}

		@Test
		void tag_value_contains() {
			assertTrue(withPatientName("Doe^John").tagValueContains("(0010,0010)", "John"));
			assertFalse(withPatientName("Doe^John").tagValueContains("(0010,0010)", "Jane"));
		}

		@Test
		void tag_value_begins_with() {
			assertTrue(withPatientName("Doe^John").tagValueBeginsWith("(0010,0010)", "Doe"));
			assertFalse(withPatientName("Doe^John").tagValueBeginsWith("(0010,0010)", "John"));
		}

		@Test
		void tag_value_ends_with() {
			assertTrue(withPatientName("Doe^John").tagValueEndsWith("(0010,0010)", "John"));
			assertFalse(withPatientName("Doe^John").tagValueEndsWith("(0010,0010)", "Doe"));
		}

	}

	@Nested
	class MissingValueReturnsFalse {

		@Test
		void comparisons_on_an_absent_tag_are_false() {
			ExprCondition empty = new ExprCondition();

			assertFalse(empty.tagValueIsPresent(Tag.PatientName, "x"));
			assertFalse(empty.tagValueContains(Tag.PatientName, "x"));
			assertFalse(empty.tagValueBeginsWith(Tag.PatientName, "x"));
			assertFalse(empty.tagValueEndsWith(Tag.PatientName, "x"));
		}

	}

}