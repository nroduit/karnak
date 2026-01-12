/*
 * Copyright (c) 2025 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.dicominnolitics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Checks that the files can be opened and read correctly. When updating the json files,
 * this class must be updated. It also allows to check the differences in size between the
 * previous and updated version, especially if something was deleted.
 */
public class JsonDicomFilesTest {

	@Test
	public void loadAttributes() {
		jsonAttributes[] attributes = StandardAttributes.readJsonAttributes();
		assertEquals(attributes.length, 5129);
	}

	@Test
	public void loadCiodsToModules() {
		jsonCIODtoModule[] ciodsToModules = StandardCIODtoModules.readJsonCIODToModules();
		assertEquals(ciodsToModules.length, 3258);
	}

	@Test
	public void loadCiods() {
		jsonCIOD[] ciods = StandardCIODS.readJsonCIODS();
		assertEquals(ciods.length, 171);
	}

	@Test
	public void loadConfidentialityProfiles() {
		StandardConfidentialityProfiles standardConfidentialityProfiles = new StandardConfidentialityProfiles();
		jsonConfidentialityProfiles[] confidentialityProfiles = standardConfidentialityProfiles
			.getConfidentialityProfiles();
		assertEquals(confidentialityProfiles.length, 621);
	}

	@Test
	public void loadModulesToAttributes() {
		jsonModuleToAttribute[] moduleToAttributes = StandardModuleToAttributes.readJsonModuleToAttributes();
		assertEquals(moduleToAttributes.length, 93241);
	}

	@Test
	public void loadSops() {
		jsonSOP[] sops = StandardSOPS.readJsonSOPS();
		assertEquals(sops.length, 175);
	}

}
