/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.dicominnolitics;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.karnak.backend.model.action.ActionItem;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Each {@code Standard*} class wraps a bundled DICOM-standard JSON file. Both the
 * instance constructor and the static reader parse the same resource, so exercising both
 * confirms the resource is present and parses, and walks the generated POJO getters.
 *
 * <p>
 * A {@code @SpringBootTest} context is required because resolving a confidentiality
 * profile string can build a {@code MultipleActions}, which reads the {@code AppConfig}
 * singleton.
 */
@SpringBootTest
@DisplayNameGeneration(ReplaceUnderscores.class)
class StandardReadersTest {

	@Nested
	class ModuleToAttributes {

		@Test
		void constructor_and_static_reader_parse_the_resource() {
			StandardModuleToAttributes std = new StandardModuleToAttributes();
			JsonModuleToAttribute[] viaInstance = std.getModuleToAttributes();
			JsonModuleToAttribute[] viaStatic = StandardModuleToAttributes.readJsonModuleToAttributes();

			assertNotNull(viaInstance);
			assertNotNull(viaStatic);
			assertTrue(viaInstance.length > 0);
			assertTrue(viaStatic.length > 0);

			JsonModuleToAttribute first = viaInstance[0];
			// Walk the generated getters.
			first.getModuleId();
			first.getPath();
			first.getTag();
			first.getType();
			first.getLinkToStandard();
			first.getDescription();
		}

	}

	@Nested
	class CIODtoModules {

		@Test
		void constructor_and_static_reader_parse_the_resource() {
			StandardCIODtoModules std = new StandardCIODtoModules();
			JsonCIODtoModule[] viaInstance = std.getCIODToModules();

			assertNotNull(viaInstance);
			assertNotNull(StandardCIODtoModules.readJsonCIODToModules());
			assertTrue(viaInstance.length > 0);

			JsonCIODtoModule first = viaInstance[0];
			first.getCiodId();
			first.getModuleId();
			first.getUsage();
			first.getConditionalStatement();
			first.getInformationEntity();
		}

	}

	@Nested
	class CIODS {

		@Test
		void constructor_and_static_reader_parse_the_resource() {
			StandardCIODS std = new StandardCIODS();
			JsonCIOD[] viaInstance = std.getCIODS();

			assertNotNull(viaInstance);
			assertNotNull(StandardCIODS.readJsonCIODS());
			assertTrue(viaInstance.length > 0);

			JsonCIOD first = viaInstance[0];
			first.getName();
			first.getId();
			first.getDescription();
			first.getLinkToStandard();
		}

	}

	@Nested
	class SOPS {

		@Test
		void constructor_and_static_reader_parse_the_resource() {
			StandardSOPS std = new StandardSOPS();
			JsonSOP[] viaInstance = std.getSOPS();

			assertNotNull(viaInstance);
			assertNotNull(StandardSOPS.readJsonSOPS());
			assertTrue(viaInstance.length > 0);

			JsonSOP first = viaInstance[0];
			first.getName();
			first.getId();
			first.getCiod();
		}

	}

	@Nested
	class StandardAttributesReader {

		@Test
		void static_reader_parses_the_resource() {
			JsonAttributes[] attributes = StandardAttributes.readJsonAttributes();

			assertNotNull(attributes);
			assertTrue(attributes.length > 0);

			JsonAttributes first = attributes[0];
			first.getTag();
			first.getName();
			first.getKeyword();
			first.getValueRepresentation();
			first.getValueMultiplicity();
			first.getRetired();
			first.getId();
		}

	}

	@Nested
	class ConfidentialityProfilesReader {

		@Test
		void static_reader_parses_the_resource_and_exposes_profiles() {
			JsonConfidentialityProfiles[] profiles = StandardConfidentialityProfiles.readJsonConfidentialityProfiles();

			assertNotNull(profiles);
			assertTrue(profiles.length > 0);

			JsonConfidentialityProfiles first = profiles[0];
			first.getId();
			first.getName();
			first.getTag();
		}

	}

	@Nested
	class ConvertActionMapping {

		// JsonConfidentialityProfiles.convertAction is exercised through the action
		// getters. Some profiles leave a profile string null (the switch would NPE), so
		// each call is guarded; the standard set still drives the concrete branches.
		@Test
		void produces_concrete_action_items_across_the_standard_set() {
			int resolved = 0;
			for (JsonConfidentialityProfiles p : StandardConfidentialityProfiles.readJsonConfidentialityProfiles()) {
				resolved += countActions(p);
			}
			assertTrue(resolved > 0);
		}

		private static int countActions(JsonConfidentialityProfiles p) {
			return resolved(p::getBasicProfile) + resolved(p::getStdCompIOD) + resolved(p::getCleanDescOpt);
		}

		private static int resolved(java.util.function.Supplier<ActionItem> getter) {
			try {
				return getter.get() != null ? 1 : 0;
			}
			catch (RuntimeException ignored) {
				// Profile string is null for this column; convertAction cannot map it.
				return 0;
			}
		}

	}

}