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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.exception.ModuleNotFoundException;

/** Exercises the module → attribute lookups over the bundled DICOM standard mapping. */
@DisplayNameGeneration(ReplaceUnderscores.class)
class ModuleToAttributesTest {

	private static ModuleToAttributes moduleToAttributes;

	@BeforeAll
	static void loadMapping() {
		moduleToAttributes = new ModuleToAttributes();
	}

	@Test
	void get_module_ids_is_sorted_and_contains_the_patient_module() {
		List<String> ids = moduleToAttributes.getModuleIds();

		assertTrue(ids.contains("patient"));
		List<String> sorted = ids.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
		assertTrue(sorted.equals(ids));
	}

	@Test
	void get_attributes_by_module_returns_the_module_attributes() {
		Map<String, ModuleAttribute> attributes = moduleToAttributes.getAttributesByModule("patient");

		assertFalse(attributes.isEmpty());
		assertTrue(attributes.values().stream().allMatch(a -> "patient".equals(a.getModuleId())));
	}

	@Test
	void get_attributes_by_module_returns_null_for_an_unknown_module() {
		assertNull(moduleToAttributes.getAttributesByModule("no-such-module"));
	}

	@Test
	void get_module_attributes_by_type_keeps_only_the_requested_type() throws ModuleNotFoundException {
		Map<String, ModuleAttribute> type2 = moduleToAttributes.getModuleAttributesByType("patient", "2");

		assertFalse(type2.isEmpty());
		assertTrue(type2.values().stream().allMatch(a -> "2".equals(a.getType())));
	}

	@Test
	void get_module_attributes_by_type_throws_for_an_unknown_module() {
		assertThrows(ModuleNotFoundException.class,
				() -> moduleToAttributes.getModuleAttributesByType("no-such-module", "1"));
	}

}