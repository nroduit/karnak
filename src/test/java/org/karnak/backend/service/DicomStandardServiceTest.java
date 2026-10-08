/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.karnak.backend.model.standard.AttributeDetail;
import org.karnak.backend.model.standard.StandardDICOM;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DicomStandardServiceTest {

	private DicomStandardService dicomStandardService;

	@BeforeEach
	void setUp() {
		dicomStandardService = new DicomStandardService(new StandardDICOM());
	}

	@Test
	void searches_an_attribute_by_keyword() {
		List<AttributeDetail> results = dicomStandardService.searchAttributes("PatientName", false);

		assertTrue(results.stream().anyMatch(a -> "(0010,0010)".equals(a.tag())));
	}

	@Test
	void searches_an_attribute_by_tag_digits() {
		List<AttributeDetail> results = dicomStandardService.searchAttributes("00100010", false);

		assertTrue(results.stream().anyMatch(a -> "PatientName".equals(a.keyword())));
	}

	@Test
	void excludes_retired_attributes_by_default() {
		List<AttributeDetail> results = dicomStandardService.searchAttributes("", false);

		assertFalse(results.stream().anyMatch(a -> "Y".equalsIgnoreCase(a.retired())));
	}

	@Test
	void lists_modules_and_their_attributes() {
		assertFalse(dicomStandardService.listModuleIds().isEmpty());
		assertFalse(dicomStandardService.moduleAttributes("patient").isEmpty());
	}

	@Test
	void returns_no_attributes_for_an_unknown_module() {
		assertTrue(dicomStandardService.moduleAttributes("does-not-exist").isEmpty());
	}

}
