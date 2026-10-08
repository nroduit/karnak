/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.standard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

/** Exercises the search / lookup helpers of the bundled DICOM attribute dictionary. */
@DisplayNameGeneration(ReplaceUnderscores.class)
class AttributeDetailsTest {

	// PatientName tag (0010,0010) -> dictionary id "00100010".
	private static final String PATIENT_NAME_ID = "00100010";

	private static AttributeDetails attributeDetails;

	@BeforeAll
	static void loadDictionary() {
		attributeDetails = new AttributeDetails();
	}

	@Test
	void get_attribute_detail_resolves_a_known_id() {
		AttributeDetail detail = attributeDetails.getAttributeDetail(PATIENT_NAME_ID);

		assertEquals(PATIENT_NAME_ID, detail.id());
		assertEquals("PatientName", detail.keyword());
		assertEquals("Patient's Name", detail.name());
	}

	@Test
	void get_attribute_detail_returns_null_for_an_unknown_id() {
		assertNull(attributeDetails.getAttributeDetail("ffffffff"));
	}

	@Test
	void search_by_keyword_finds_the_matching_attribute() {
		List<AttributeDetail> results = attributeDetails.search("PatientName", false, 50);

		assertTrue(results.stream().anyMatch(a -> PATIENT_NAME_ID.equals(a.id())));
	}

	@Test
	void search_is_case_insensitive_and_matches_a_substring_of_the_name() {
		List<AttributeDetail> results = attributeDetails.search("patient's name", false, 50);

		assertTrue(results.stream().anyMatch(a -> PATIENT_NAME_ID.equals(a.id())));
	}

	@Test
	void search_by_tag_digits_finds_the_attribute() {
		List<AttributeDetail> results = attributeDetails.search("0010,0010", false, 50);

		assertTrue(results.stream().anyMatch(a -> PATIENT_NAME_ID.equals(a.id())));
	}

	@Test
	void search_respects_the_limit_and_sorts_by_keyword() {
		List<AttributeDetail> results = attributeDetails.search("", true, 5);

		assertEquals(5, results.size());
		List<String> keywords = results.stream().map(AttributeDetail::keyword).toList();
		List<String> sorted = keywords.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
		assertEquals(sorted, keywords);
	}

	@Test
	void search_excludes_retired_attributes_unless_requested() {
		List<AttributeDetail> withoutRetired = attributeDetails.search("", false, Integer.MAX_VALUE);
		List<AttributeDetail> withRetired = attributeDetails.search("", true, Integer.MAX_VALUE);

		assertFalse(withoutRetired.stream().anyMatch(a -> "Y".equalsIgnoreCase(a.retired())));
		assertTrue(withRetired.size() > withoutRetired.size());
	}

	@Test
	void get_list_attribute_detail_returns_only_the_requested_ids() {
		List<AttributeDetail> results = attributeDetails.getListAttributeDetail(List.of(PATIENT_NAME_ID, "no-such-id"));

		assertEquals(1, results.size());
		assertEquals(PATIENT_NAME_ID, results.getFirst().id());
	}

}