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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ModuleTest {

	@Test
	void exposes_its_identifying_fields() {
		Module module = new Module("patient", "M", "Patient");

		assertEquals("patient", module.id());
		assertEquals("M", module.usage());
		assertEquals("Patient", module.informationEntity());
	}

	@Test
	void is_mandatory_when_usage_is_M() {
		assertTrue(Module.moduleIsMandatory(new Module("patient", "M", "Patient")));
	}

	@Test
	void is_not_mandatory_for_other_usages() {
		assertFalse(Module.moduleIsMandatory(new Module("optional", "U", "Patient")));
		assertFalse(Module.moduleIsMandatory(new Module("conditional", "C", "Patient")));
	}

}
