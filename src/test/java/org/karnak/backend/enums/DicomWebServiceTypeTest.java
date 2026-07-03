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

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class DicomWebServiceTypeTest {

	@Test
	void enumerates_the_six_dicomweb_services() {
		assertEquals(6, DicomWebServiceType.values().length);
	}

	@Test
	void exposes_the_display_name_and_role() {
		assertEquals("STOW-RS", DicomWebServiceType.STOW_RS.getDisplayName());
		assertEquals("Store", DicomWebServiceType.STOW_RS.getRole());
		assertEquals("QIDO-RS", DicomWebServiceType.QIDO_RS.getDisplayName());
		assertEquals("Query", DicomWebServiceType.QIDO_RS.getRole());
		assertEquals("WADO-URI", DicomWebServiceType.WADO_URI.getDisplayName());
		assertEquals("Retrieve (legacy)", DicomWebServiceType.WADO_URI.getRole());
		assertEquals("Capabilities", DicomWebServiceType.CAPABILITIES.getDisplayName());
		assertEquals("Discovery", DicomWebServiceType.CAPABILITIES.getRole());
	}

}