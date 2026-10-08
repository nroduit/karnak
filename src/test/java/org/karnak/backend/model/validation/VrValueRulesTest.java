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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dcm4che3.data.VR;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class VrValueRulesTest {

	@Test
	void length_within_the_vr_maximum_is_accepted() {
		assertNull(VrValueRules.lengthExpectation(VR.LO, "X".repeat(64)));
		assertNull(VrValueRules.lengthExpectation(VR.CS, "ABCDEF"));
	}

	@Test
	void length_above_the_vr_maximum_is_reported() {
		assertNotNull(VrValueRules.lengthExpectation(VR.LO, "X".repeat(65)));
		assertNotNull(VrValueRules.lengthExpectation(VR.SH, "X".repeat(17)));
		assertNotNull(VrValueRules.lengthExpectation(VR.UI, "1." + "2".repeat(70)));
	}

	@Test
	void person_name_component_group_length_is_bounded() {
		assertNull(VrValueRules.lengthExpectation(VR.PN, "DOE^JOHN"));
		assertNotNull(VrValueRules.lengthExpectation(VR.PN, "X".repeat(65)));
	}

	@Test
	void valid_formats_are_accepted() {
		assertNull(VrValueRules.formatExpectation(VR.DA, "20260101"));
		assertNull(VrValueRules.formatExpectation(VR.TM, "101010"));
		assertNull(VrValueRules.formatExpectation(VR.TM, "07")); // right-truncation
																	// allowed
		assertNull(VrValueRules.formatExpectation(VR.TM, "235960")); // leap second
		assertNull(VrValueRules.formatExpectation(VR.DT, "2026"));
		assertNull(VrValueRules.formatExpectation(VR.DT, "20260101120000.500000+0500"));
		assertNull(VrValueRules.formatExpectation(VR.IS, "-1024"));
		assertNull(VrValueRules.formatExpectation(VR.DS, "1.5e3"));
		assertNull(VrValueRules.formatExpectation(VR.CS, "MONOCHROME2"));
		assertNull(VrValueRules.formatExpectation(VR.AS, "045Y"));
		assertNull(VrValueRules.formatExpectation(VR.UI, "1.2.840.10008.1.2.1"));
		// A lone "0" part is allowed
		assertNull(VrValueRules.formatExpectation(VR.UI, "1.0.840"));
	}

	@Test
	void invalid_formats_are_reported() {
		assertNotNull(VrValueRules.formatExpectation(VR.DA, "2026-01-01")); // legacy
																			// dotted/dashed
																			// form
		assertNotNull(VrValueRules.formatExpectation(VR.DA, "20261301")); // month 13
		assertNotNull(VrValueRules.formatExpectation(VR.DA, "20260132")); // day 32
		assertNotNull(VrValueRules.formatExpectation(VR.TM, "10:10:10")); // legacy colon
																			// form
		assertNotNull(VrValueRules.formatExpectation(VR.TM, "250000")); // hour 25
		assertNotNull(VrValueRules.formatExpectation(VR.TM, "106000")); // minute 60
		assertNotNull(VrValueRules.formatExpectation(VR.DT, "20261301")); // month 13
		assertNotNull(VrValueRules.formatExpectation(VR.DT, "2026010112+2500")); // offset
																					// hour
																					// 25
		assertNotNull(VrValueRules.formatExpectation(VR.IS, "12a"));
		assertNotNull(VrValueRules.formatExpectation(VR.IS, "2147483648")); // overflows
		assertNotNull(VrValueRules.formatExpectation(VR.CS, "lowercase"));
		assertNotNull(VrValueRules.formatExpectation(VR.AS, "45Y"));
		assertNotNull(VrValueRules.formatExpectation(VR.UI, "1.2.x")); // non-digit
		assertNotNull(VrValueRules.formatExpectation(VR.UI, "1..2")); // empty component
		assertNotNull(VrValueRules.formatExpectation(VR.UI, "1.2.")); // trailing dot
		// A part with a leading zero violates PS3.5 §9.1
		assertNotNull(VrValueRules.formatExpectation(VR.UI, "2.20.547.8.406042.068.60.3358.72699779286"));
	}

	@Test
	void person_name_structure_is_bounded() {
		assertNull(VrValueRules.formatExpectation(VR.PN, "DOE^JOHN=ALPHABETIC=IDEOGRAPHIC"));
		assertNotNull(VrValueRules.formatExpectation(VR.PN, "A=B=C=D"));
		assertNotNull(VrValueRules.formatExpectation(VR.PN, "A^B^C^D^E^F"));
	}

	@Test
	void length_overflow_is_an_error_for_small_structured_fields_only() {
		assertTrue(VrValueRules.lengthOverflowIsError(VR.SH));
		assertTrue(VrValueRules.lengthOverflowIsError(VR.UI));
		assertTrue(VrValueRules.lengthOverflowIsError(VR.IS));
		assertFalse(VrValueRules.lengthOverflowIsError(VR.LO));
		assertFalse(VrValueRules.lengthOverflowIsError(VR.ST));
		assertFalse(VrValueRules.lengthOverflowIsError(VR.LT));
		assertFalse(VrValueRules.lengthOverflowIsError(VR.PN));
	}

	@Test
	void unconstrained_vrs_are_never_reported() {
		assertNull(VrValueRules.lengthExpectation(VR.UT, "X".repeat(100_000)));
		assertNull(VrValueRules.formatExpectation(VR.LO, "anything goes here"));
	}

}