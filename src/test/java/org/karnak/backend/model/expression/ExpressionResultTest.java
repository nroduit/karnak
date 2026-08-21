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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.karnak.backend.model.action.Keep;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ExpressionResultTest {

	private static ExprCondition conditionWithPatientName() {
		Attributes dcm = new Attributes();
		dcm.setString(Tag.PatientName, VR.PN, "Doe^John");
		return new ExprCondition(dcm);
	}

	@Nested
	class Get {

		@Test
		void evaluates_a_boolean_condition_to_true() {
			Object result = ExpressionResult.get("tagIsPresent('00100010')", conditionWithPatientName(), Boolean.class);

			assertEquals(Boolean.TRUE, result);
		}

		@Test
		void evaluates_a_boolean_condition_to_false() {
			Object result = ExpressionResult.get("tagIsPresent('00080020')", conditionWithPatientName(), Boolean.class);

			assertEquals(Boolean.FALSE, result);
		}

		@Test
		void resolves_the_Tag_helper_variable() {
			Attributes dcm = new Attributes();
			dcm.setString(Tag.PatientName, VR.PN, "Doe^John");
			ExprAction action = new ExprAction(Tag.PatientName, VR.PN, dcm);

			Object result = ExpressionResult.get("getString(#Tag.PatientName)", action, String.class);

			assertEquals("Doe^John", result);
		}

		@Test
		void returns_an_action_item_from_an_action_expression() {
			Attributes dcm = new Attributes();
			ExprAction action = new ExprAction(Tag.PatientName, VR.PN, dcm);

			Object result = ExpressionResult.get("Keep()", action, org.karnak.backend.model.action.ActionItem.class);

			assertInstanceOf(Keep.class, result);
		}

		@Test
		void throws_illegal_state_on_an_invalid_expression() {
			assertThrows(IllegalStateException.class,
					() -> ExpressionResult.get("this is ((( not valid", conditionWithPatientName(), Boolean.class));
		}

	}

	@Nested
	class IsValid {

		@Test
		void reports_a_valid_expression() {
			ExpressionError error = ExpressionResult.isValid("tagIsPresent('00100010')", conditionWithPatientName(),
					Boolean.class);

			assertTrue(error.isValid());
			assertNull(error.getMsg());
		}

		@Test
		void reports_an_invalid_expression_with_a_message() {
			ExpressionError error = ExpressionResult.isValid("this is ((( not valid", conditionWithPatientName(),
					Boolean.class);

			assertFalse(error.isValid());
			assertTrue(error.getMsg().startsWith("Expression is not valid"));
		}

		@Test
		void reports_the_message_of_the_failure_only_once() {
			ExpressionError error = ExpressionResult.isValid("this is ((( not valid", conditionWithPatientName(),
					Boolean.class);

			assertEquals(1, error.getMsg().split("Expression is not valid", -1).length - 1);
		}

		@Test
		void names_the_unknown_method_of_an_expression() {
			ExpressionError error = ExpressionResult.isValid("thisMethodDoesNotExist()", conditionWithPatientName(),
					Boolean.class);

			assertFalse(error.isValid());
			assertTrue(error.getMsg().contains("thisMethodDoesNotExist"), error.getMsg());
		}

		@Test
		void reports_an_expression_that_cannot_be_converted_to_the_expected_type() {
			// A condition must evaluate to a boolean
			ExpressionError error = ExpressionResult.isValid("'hello'", conditionWithPatientName(), Boolean.class);

			assertFalse(error.isValid());
			assertTrue(error.getMsg().contains("Type conversion problem"), error.getMsg());
		}

		@ParameterizedTest
		@NullSource
		@ValueSource(strings = { "", "   " })
		void reports_a_blank_expression_as_invalid(String condition) {
			ExpressionError error = ExpressionResult.isValid(condition, conditionWithPatientName(), Boolean.class);

			assertFalse(error.isValid());
			assertEquals("Expression is not valid: it is empty", error.getMsg());
		}

		@Test
		void appends_the_root_cause_of_a_wrapped_failure() {
			// The SpEL message says which types could not be converted, the cause says
			// why
			ExpressionError error = ExpressionResult.isValid("'hello'", conditionWithPatientName(), Boolean.class);

			assertTrue(error.getMsg().contains("Caused by:"), error.getMsg());
			assertTrue(error.getMsg().contains("Invalid boolean value 'hello'"), error.getMsg());
		}

		@Test
		void reports_a_parse_error_without_a_cause() {
			ExpressionError error = ExpressionResult.isValid("this is ((( not valid", conditionWithPatientName(),
					Boolean.class);

			assertTrue(error.getMsg().contains("EL1041E"), error.getMsg());
			assertFalse(error.getMsg().contains("Caused by:"), error.getMsg());
		}

	}

	@Nested
	class ParsedExpressionCache {

		@BeforeEach
		void emptyTheCache() {
			ExpressionResult.clearCache();
		}

		@Test
		void parses_the_same_expression_only_once() {
			assertSame(ExpressionResult.parse("tagIsPresent('00100010')"),
					ExpressionResult.parse("tagIsPresent('00100010')"));
			assertEquals(1, ExpressionResult.cacheSize());
		}

		@Test
		void keeps_distinct_expressions_apart() {
			ExpressionResult.parse("tagIsPresent('00100010')");
			ExpressionResult.parse("tagIsPresent('00080020')");

			assertEquals(2, ExpressionResult.cacheSize());
		}

		@Test
		void does_not_cache_an_expression_that_does_not_parse() {
			assertThrows(Exception.class, () -> ExpressionResult.parse("this is ((( not valid"));

			assertEquals(0, ExpressionResult.cacheSize());
		}

		@Test
		void a_cached_expression_is_evaluated_against_the_object_of_each_call() {
			// The parsed form must hold nothing of the dataset it was first evaluated on
			Attributes withName = new Attributes();
			withName.setString(Tag.PatientName, VR.PN, "Doe^John");

			Object first = ExpressionResult.get("tagIsPresent('00100010')", new ExprCondition(withName), Boolean.class);
			Object second = ExpressionResult.get("tagIsPresent('00100010')", new ExprCondition(new Attributes()),
					Boolean.class);

			assertEquals(Boolean.TRUE, first);
			assertEquals(Boolean.FALSE, second);
		}

		@Test
		void empties_the_cache_instead_of_growing_past_the_cap() {
			for (int i = 0; i < 501; i++) {
				ExpressionResult.parse("tagIsPresent('%08d')".formatted(i));
			}

			assertEquals(1, ExpressionResult.cacheSize());
		}

	}

}