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

import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ModuleAttributeTest {

	@Nested
	class TagPathGeneration {

		@Test
		void strips_the_module_id_segment_from_the_module_tag_path() {
			ModuleAttribute attribute = new ModuleAttribute("patient:00100010:00100020", "1", "patient");

			assertEquals("00100010:00100020", attribute.getTagPath());
			assertEquals("patient:00100010:00100020", attribute.getModuleTagPath());
		}

		@Test
		void keeps_the_path_unchanged_when_module_id_is_absent() {
			ModuleAttribute attribute = new ModuleAttribute("00100010:00100020", "2", "patient");

			assertEquals("00100010:00100020", attribute.getTagPath());
		}

		@Test
		void exposes_type_and_module_id() {
			ModuleAttribute attribute = new ModuleAttribute("patient:00100010", "1C", "patient");

			assertEquals("1C", attribute.getType());
			assertEquals("patient", attribute.getModuleId());
		}

	}

	@Nested
	class GetStrictedType {

		@Test
		void returns_the_strictest_type_present_following_priority_order() {
			List<ModuleAttribute> attributes = List.of(new ModuleAttribute("m:t1", "3", "m"),
					new ModuleAttribute("m:t2", "2", "m"), new ModuleAttribute("m:t3", "1C", "m"));

			// Priority order is 1, 1C, 2, 2C, 3 -> 1C is the strictest present.
			assertEquals("1C", ModuleAttribute.getStricterType(attributes));
		}

		@Test
		void prefers_type_1_over_everything_else() {
			List<ModuleAttribute> attributes = List.of(new ModuleAttribute("m:t1", "3", "m"),
					new ModuleAttribute("m:t2", "1", "m"));

			assertEquals("1", ModuleAttribute.getStricterType(attributes));
		}

		@Test
		void returns_the_only_type_when_a_single_attribute_is_given() {
			assertEquals("3", ModuleAttribute.getStricterType(List.of(new ModuleAttribute("m:t1", "3", "m"))));
		}

		@Test
		void returns_null_for_an_empty_list() {
			assertNull(ModuleAttribute.getStricterType(List.of()));
		}

	}

}
