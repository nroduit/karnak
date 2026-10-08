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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * {@link DICOMType#getBySOP} looks the attribute up in the DICOM standard through
 * {@code AppConfig}; a {@code @SpringBootTest} context provides that singleton.
 */
@SpringBootTest
@DisplayNameGeneration(ReplaceUnderscores.class)
class DICOMTypeTest {

	private static final String SOP_CLASS_UID = "1.2.840.10008.5.1.4.1.1.1";

	private static Attributes crImage() {
		Attributes dcm = new Attributes();
		dcm.setString(Tag.SOPClassUID, VR.UI, SOP_CLASS_UID);
		dcm.setString(Tag.PatientName, VR.PN, "Doe^John");
		return dcm;
	}

	@Test
	void returns_the_attribute_type_for_a_known_tag() {
		assertEquals("2", DICOMType.getBySOP(crImage(), Tag.PatientName));
	}

	@Test
	void returns_null_for_a_tag_absent_from_the_sop_definition() {
		Attributes dcm = crImage();
		dcm.setString(0x00090010, VR.LO, "private");

		assertNull(DICOMType.getBySOP(dcm, 0x00090010));
	}

}