/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.action;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.model.profilepipe.HashContext;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * {@link MultipleActions} resolves the strictest de-identification action for a tag using
 * the DICOM standard, so it needs the Spring-managed {@code AppConfig} singleton (which
 * exposes {@code StandardDICOM}). A {@code @SpringBootTest} context provides it.
 */
@SpringBootTest
@DisplayNameGeneration(ReplaceUnderscores.class)
class MultipleActionsStandardTest {

	private static final HMAC HMAC_KEY = new HMAC(
			new HashContext(HMAC.hexToByte("0123456789abcdef0123456789abcdef"), "PATIENT-1"));

	// CR Image Storage.
	private static final String SOP_CLASS_UID = "1.2.840.10008.5.1.4.1.1.1";

	private static Attributes crImage() {
		Attributes dcm = new Attributes();
		dcm.setString(Tag.SOPClassUID, VR.UI, SOP_CLASS_UID);
		dcm.setString(Tag.PatientName, VR.PN, "Doe^John");
		dcm.setString(Tag.PatientID, VR.LO, "PID-1");
		return dcm;
	}

	@Test
	void x_z_d_resolves_an_action_for_a_known_tag() {
		Attributes dcm = crImage();

		// PatientName is a type-2 attribute -> replaced with null (kept but emptied).
		new MultipleActions("X/Z/D").execute(dcm, Tag.PatientName, HMAC_KEY);

		assertTrue(dcm.contains(Tag.PatientName));
	}

	@Test
	void x_z_u_resolves_an_action_for_a_uid_tag() {
		Attributes dcm = crImage();
		dcm.setString(Tag.StudyInstanceUID, VR.UI, "1.2.3.4.5");

		new MultipleActions("X/Z/U").execute(dcm, Tag.StudyInstanceUID, HMAC_KEY);

		// A type-1 UID is hashed; the value must change and stay present.
		assertTrue(dcm.contains(Tag.StudyInstanceUID));
		assertFalse("1.2.3.4.5".equals(dcm.getString(Tag.StudyInstanceUID)));
	}

	@Test
	void applies_the_strictest_action_for_each_supported_symbol() {
		for (String symbol : new String[] { "Z/D", "X/D", "X/Z/D", "X/Z", "X/Z/U", "X/Z/U*" }) {
			Attributes dcm = crImage();
			// Should resolve and apply without throwing for a known type-2 tag.
			new MultipleActions(symbol).execute(dcm, Tag.PatientID, HMAC_KEY);
		}
	}

	@Test
	void falls_back_to_the_default_action_for_an_unknown_tag() {
		Attributes dcm = crImage();
		// A private tag is not part of the SOP definition -> strictest default action.
		dcm.setString(0x00090010, VR.LO, "private");

		new MultipleActions("X/Z/D").execute(dcm, 0x00090010, HMAC_KEY);

		// The default for X/Z/D is the dummy replacement, so the attribute survives.
		assertTrue(dcm.contains(0x00090010));
	}

}