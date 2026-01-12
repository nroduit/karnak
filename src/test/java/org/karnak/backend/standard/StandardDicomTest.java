/*
 * Copyright (c) 2025 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.standard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.model.standard.AttributeDetail;
import org.karnak.backend.model.standard.ModuleAttribute;
import org.karnak.backend.model.standard.StandardDICOM;

public class StandardDicomTest {

	private StandardDICOM dicom;

	@BeforeEach
	public void setUpBeforeClass() {
		this.dicom = new StandardDICOM();
	}

	@Test
	public void loadAllSopsTest() {
		assertEquals(175, this.dicom.getAllSOPuids().size());
	}

	@Test
	public void getAttributeDetailTest() {
		AttributeDetail patient = dicom.getAttributeDetail("00100010");
		assertEquals("Patient's Name", patient.getName());
		assertEquals("PN", patient.getValueRepresentation());
	}

	@Test
	public void getAttributeBySopTest() {
		List<ModuleAttribute> attr = this.dicom.getAttributesBySOP("1.2.840.10008.5.1.4.1.1.1", "(0010,0010)");
		assertEquals(1, attr.size());
		assertEquals("patient", attr.get(0).getModuleId());
		assertEquals("2", attr.get(0).getType());
	}

	@Test
	public void moduleIsPresentTest() {
		assertTrue(this.dicom.moduleIsPresent("1.2.840.10008.5.1.4.1.1.1", "patient"));
	}

	@Test
	public void cleanTagPathTest() {
		String path1 = "(0010,0010)";
		assertEquals("00100010", StandardDICOM.cleanTagPath(path1));
		String path2 = "0010,0010";
		assertEquals("00100010", StandardDICOM.cleanTagPath(path2));
		String path3 = "(0010,00XX)";
		assertEquals("001000xx", StandardDICOM.cleanTagPath(path3));
		String path4 = "XX10,00XX";
		assertEquals("xx1000xx", StandardDICOM.cleanTagPath(path4));
		String path5 = "00100010";
		assertEquals("00100010", StandardDICOM.cleanTagPath(path5));
	}

}
