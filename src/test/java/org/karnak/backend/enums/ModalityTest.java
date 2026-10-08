/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ModalityTest {

	@Test
	void exposes_its_description() {
		assertEquals("Computed Tomography", Modality.CT.getDescription());
	}

	@Test
	void get_modality_resolves_a_known_name() {
		assertEquals(Modality.CT, Modality.getModality("CT"));
	}

	@Test
	void get_modality_falls_back_to_all_for_an_unknown_name() {
		assertEquals(Modality.ALL, Modality.getModality("NOPE"));
	}

	@Test
	void get_modality_falls_back_to_all_for_null() {
		assertEquals(Modality.ALL, Modality.getModality(null));
	}

	@Test
	void to_string_combines_name_and_description() {
		assertTrue(Modality.CT.toString().contains("CT"));
		assertTrue(Modality.CT.toString().contains("Computed Tomography"));
	}

}