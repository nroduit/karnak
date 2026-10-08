/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.dicom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class DefacingUtilTest {

	// CT Image Storage.
	private static final String CT_SOP_CLASS_UID = "1.2.840.10008.5.1.4.1.1.2";

	// Enhanced CT Image Storage (a derived CT SOP class).
	private static final String ENHANCED_CT_SOP_CLASS_UID = "1.2.840.10008.5.1.4.1.1.2.1";

	// CR Image Storage (not a CT).
	private static final String CR_SOP_CLASS_UID = "1.2.840.10008.5.1.4.1.1.1";

	private static Attributes withImageOrientation(double... iop) {
		Attributes dcm = new Attributes();
		if (iop.length > 0) {
			dcm.setDouble(Tag.ImageOrientationPatient, VR.DS, iop);
		}
		return dcm;
	}

	private static Attributes withSopClass(String sopClassUID) {
		Attributes dcm = new Attributes();
		dcm.setString(Tag.SOPClassUID, VR.UI, sopClassUID);
		return dcm;
	}

	@Test
	void is_axial_returns_true_for_an_axial_orientation() {
		assertTrue(DefacingUtil.isAxial(withImageOrientation(1, 0, 0, 0, 1, 0)));
	}

	@Test
	void is_axial_returns_false_for_a_sagittal_orientation() {
		assertFalse(DefacingUtil.isAxial(withImageOrientation(0, 1, 0, 0, 0, -1)));
	}

	@Test
	void is_axial_returns_false_for_a_coronal_orientation() {
		assertFalse(DefacingUtil.isAxial(withImageOrientation(1, 0, 0, 0, 0, -1)));
	}

	@Test
	void is_axial_returns_false_when_orientation_is_absent() {
		assertFalse(DefacingUtil.isAxial(withImageOrientation()));
	}

	@Test
	void is_ct_recognises_the_ct_sop_class() {
		assertTrue(DefacingUtil.isCT(withSopClass(CT_SOP_CLASS_UID)));
	}

	@Test
	void is_ct_recognises_a_derived_ct_sop_class() {
		assertTrue(DefacingUtil.isCT(withSopClass(ENHANCED_CT_SOP_CLASS_UID)));
	}

	@Test
	void is_ct_rejects_a_non_ct_sop_class() {
		assertFalse(DefacingUtil.isCT(withSopClass(CR_SOP_CLASS_UID)));
	}

	@Test
	void is_ct_returns_false_when_the_sop_class_is_absent() {
		assertFalse(DefacingUtil.isCT(new Attributes()));
	}

	@Test
	void hounsfield_to_pixel_value_applies_the_rescale_slope_and_intercept() {
		Attributes dcm = new Attributes();
		dcm.setDouble(Tag.RescaleIntercept, VR.DS, -1024);
		dcm.setDouble(Tag.RescaleSlope, VR.DS, 2);

		// (0 - (-1024)) / 2 = 512
		assertEquals(512.0, DefacingUtil.hounsfieldToPxlValue(dcm, 0));
	}

	@Test
	void hounsfield_to_pixel_value_uses_identity_rescale_by_default() {
		assertEquals(40.0, DefacingUtil.hounsfieldToPxlValue(new Attributes(), 40));
	}

	@Test
	void random_y_is_deterministic_for_a_zero_width_range() {
		assertEquals(5, DefacingUtil.randomY(5, 5, 0));
	}

	@RepeatedTest(20)
	void random_y_stays_within_the_requested_bounds() {
		int value = DefacingUtil.randomY(0, 10, 1);

		assertTrue(value >= 0 && value <= 10, "value=" + value);
	}

}