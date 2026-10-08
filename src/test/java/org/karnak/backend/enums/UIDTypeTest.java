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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class UIDTypeTest {

	@Test
	void exposes_code_and_description() {
		assertEquals("1.2.840.10008.1.2.1", UIDType.EXPLICIT_VR_LITTLE_ENDIAN.getCode());
		assertEquals("Explicit VR - Little Endian", UIDType.EXPLICIT_VR_LITTLE_ENDIAN.getDescription());
	}

	@Test
	void from_code_matches_case_insensitively_and_trims() {
		assertEquals(UIDType.JPEG_2000, UIDType.fromCode("  1.2.840.10008.1.2.4.91  "));
	}

	@Test
	void from_code_returns_null_for_unknown_or_null() {
		assertNull(UIDType.fromCode("9.9.9"));
		assertNull(UIDType.fromCode(null));
	}

	@Test
	void from_description_matches_case_insensitively() {
		assertEquals(UIDType.JPEG_LS_LOSSLESS, UIDType.fromDescription("jpeg-ls lossless image compression"));
	}

	@Test
	void from_description_returns_null_for_unknown_or_null() {
		assertNull(UIDType.fromDescription("nope"));
		assertNull(UIDType.fromDescription(null));
	}

	@Test
	void description_of_a_known_code_returns_its_description() {
		assertEquals("JPEG 2000 Image Compression", UIDType.descriptionOf("1.2.840.10008.1.2.4.91"));
	}

	@Test
	void description_of_an_unknown_code_returns_the_default() {
		assertEquals(UIDType.DEFAULT_DESCRIPTION, UIDType.descriptionOf("9.9.9"));
	}

}