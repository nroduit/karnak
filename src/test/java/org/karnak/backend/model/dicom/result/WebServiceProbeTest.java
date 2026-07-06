/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.dicom.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.enums.DicomWebServiceType;
import org.karnak.backend.model.dicom.result.WebServiceProbe.Support;

@DisplayNameGeneration(ReplaceUnderscores.class)
class WebServiceProbeTest {

	@Test
	void support_levels_expose_a_label() {
		assertEquals("supported", Support.SUPPORTED.getLabel());
		assertEquals("not supported", Support.UNSUPPORTED.getLabel());
		assertEquals("inconclusive", Support.INCONCLUSIVE.getLabel());
	}

	@Test
	void summary_includes_the_detail_when_present() {
		WebServiceProbe probe = new WebServiceProbe(DicomWebServiceType.STOW_RS, Support.SUPPORTED, 200, "HTTP 200");

		assertEquals("STOW-RS: supported — HTTP 200", probe.getSummary());
	}

	@Test
	void summary_omits_the_detail_when_null() {
		WebServiceProbe probe = new WebServiceProbe(DicomWebServiceType.QIDO_RS, Support.INCONCLUSIVE, 0, null);

		assertEquals("QIDO-RS: inconclusive", probe.getSummary());
	}

	@Test
	void exposes_its_record_components() {
		WebServiceProbe probe = new WebServiceProbe(DicomWebServiceType.WADO_RS, Support.UNSUPPORTED, 404, "not found");

		assertEquals(DicomWebServiceType.WADO_RS, probe.type());
		assertEquals(Support.UNSUPPORTED, probe.support());
		assertEquals(404, probe.status());
		assertEquals("not found", probe.detail());
	}

	@Test
	void records_are_equal_by_value() {
		WebServiceProbe a = new WebServiceProbe(DicomWebServiceType.UPS_RS, Support.SUPPORTED, 200, "ok");
		WebServiceProbe b = new WebServiceProbe(DicomWebServiceType.UPS_RS, Support.SUPPORTED, 200, "ok");

		assertTrue(a.equals(b));
		assertEquals(a.hashCode(), b.hashCode());
		assertFalse(a.equals(new WebServiceProbe(DicomWebServiceType.UPS_RS, Support.SUPPORTED, 500, "ok")));
	}

}