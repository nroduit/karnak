/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ConditionEvaluatorTest {

	private static Condition condition(String json) {
		return new Gson().fromJson(json, Condition.class);
	}

	private static Attributes withModality(String modality) {
		var dcm = new Attributes();
		dcm.setString(Tag.Modality, VR.CS, modality);
		return dcm;
	}

	@Test
	void present_true_is_true_when_tag_has_a_value() {
		var dcm = withModality("MG");

		assertEquals(Ternary.TRUE,
				ConditionEvaluator.evaluate(dcm, condition("{\"tag\":\"00080060\",\"present\":true}")));
	}

	@Test
	void present_true_is_false_when_tag_absent() {
		assertEquals(Ternary.FALSE,
				ConditionEvaluator.evaluate(new Attributes(), condition("{\"tag\":\"00080060\",\"present\":true}")));
	}

	@Test
	void present_true_is_false_when_tag_present_but_empty() {
		var dcm = new Attributes();
		dcm.setNull(Tag.Modality, VR.CS);

		assertEquals(Ternary.FALSE,
				ConditionEvaluator.evaluate(dcm, condition("{\"tag\":\"00080060\",\"present\":true}")));
	}

	@Test
	void equals_matches_value() {
		assertEquals(Ternary.TRUE,
				ConditionEvaluator.evaluate(withModality("MG"), condition("{\"tag\":\"00080060\",\"equals\":\"MG\"}")));
	}

	@Test
	void equals_is_false_on_mismatch() {
		assertEquals(Ternary.FALSE,
				ConditionEvaluator.evaluate(withModality("CT"), condition("{\"tag\":\"00080060\",\"equals\":\"MG\"}")));
	}

	@Test
	void equals_is_unknown_when_referenced_tag_absent() {
		assertEquals(Ternary.UNKNOWN,
				ConditionEvaluator.evaluate(new Attributes(), condition("{\"tag\":\"00080060\",\"equals\":\"MG\"}")));
	}

	@Test
	void in_matches_one_of_the_values() {
		assertEquals(Ternary.TRUE, ConditionEvaluator.evaluate(withModality("RF"),
				condition("{\"tag\":\"00080060\",\"in\":[\"XA\",\"RF\"]}")));
	}

	@Test
	void not_in_is_true_when_value_outside_set() {
		assertEquals(Ternary.TRUE, ConditionEvaluator.evaluate(withModality("CT"),
				condition("{\"tag\":\"00080060\",\"notIn\":[\"MG\",\"US\"]}")));
	}

	@Test
	void any_of_is_true_when_one_branch_is_true() {
		var dcm = withModality("MG");

		assertEquals(Ternary.TRUE, ConditionEvaluator.evaluate(dcm, condition(
				"{\"anyOf\":[{\"tag\":\"00080060\",\"equals\":\"CT\"},{\"tag\":\"00080060\",\"equals\":\"MG\"}]}")));
	}

	@Test
	void all_of_is_false_when_one_branch_is_false() {
		var dcm = withModality("MG");

		assertEquals(Ternary.FALSE, ConditionEvaluator.evaluate(dcm, condition(
				"{\"allOf\":[{\"tag\":\"00080060\",\"equals\":\"MG\"},{\"tag\":\"00080060\",\"equals\":\"CT\"}]}")));
	}

	@Test
	void all_of_is_unknown_when_a_branch_is_unknown_and_none_false() {
		// Modality matches; the other branch references an absent tag -> UNKNOWN
		// dominates
		var dcm = withModality("MG");

		assertEquals(Ternary.UNKNOWN, ConditionEvaluator.evaluate(dcm, condition(
				"{\"allOf\":[{\"tag\":\"00080060\",\"equals\":\"MG\"},{\"tag\":\"00181271\",\"equals\":\"X\"}]}")));
	}

	@Test
	void null_condition_is_unknown() {
		assertEquals(Ternary.UNKNOWN, ConditionEvaluator.evaluate(new Attributes(), null));
	}

	@Test
	void value_index_restricts_the_comparison_to_one_value() {
		var dcm = new Attributes();
		dcm.setString(Tag.ImageType, VR.CS, "DERIVED", "PRIMARY", "ORIGINAL");

		assertEquals(Ternary.TRUE, ConditionEvaluator.evaluate(dcm,
				condition("{\"tag\":\"00080008\",\"valueIndex\":1,\"equals\":\"DERIVED\"}")));
		assertEquals(Ternary.FALSE, ConditionEvaluator.evaluate(dcm,
				condition("{\"tag\":\"00080008\",\"valueIndex\":1,\"in\":[\"ORIGINAL\",\"MIXED\"]}")));
	}

	@Test
	void value_index_beyond_the_values_is_unknown() {
		var dcm = new Attributes();
		dcm.setString(Tag.ImageType, VR.CS, "ORIGINAL", "PRIMARY");

		assertEquals(Ternary.UNKNOWN, ConditionEvaluator.evaluate(dcm,
				condition("{\"tag\":\"00080008\",\"valueIndex\":3,\"equals\":\"ASL\"}")));
	}

}
