/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.data.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class WebDestinationConfigEntityTest {

	@Test
	void full_constructor_sets_all_fields() {
		WebDestinationConfigEntity entity = new WebDestinationConfigEntity("PACS", "https://pacs/dicomweb", "STOW-RS",
				"group-a");

		assertEquals("PACS", entity.getDescription());
		assertEquals("https://pacs/dicomweb", entity.getUrl());
		assertEquals("STOW-RS", entity.getServices());
		assertEquals("group-a", entity.getGroupName());
	}

	@Test
	void default_constructor_leaves_services_empty() {
		WebDestinationConfigEntity entity = new WebDestinationConfigEntity();

		assertEquals("", entity.getServices());
	}

	@Test
	void exposes_its_properties_through_setters() {
		WebDestinationConfigEntity entity = new WebDestinationConfigEntity();
		entity.setId(7L);
		entity.setDescription("D");
		entity.setUrl("https://host");
		entity.setServices("QIDO-RS");
		entity.setGroupName("g");

		assertEquals(7L, entity.getId());
		assertEquals("D", entity.getDescription());
		assertEquals("https://host", entity.getUrl());
		assertEquals("QIDO-RS", entity.getServices());
		assertEquals("g", entity.getGroupName());
	}

	@Test
	void equal_instances_match_and_share_a_hash() {
		WebDestinationConfigEntity a = entity(1L);
		WebDestinationConfigEntity b = entity(1L);

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(a, a);
	}

	@Test
	void differs_by_field_type_or_null() {
		WebDestinationConfigEntity base = entity(1L);

		assertNotEquals(base, entity(2L));
		assertFalse(base.equals(null));
		assertNotEquals(base, "not-a-web-destination");
	}

	@Test
	void to_string_contains_the_url() {
		assertTrue(entity(1L).toString().contains("https://pacs/dicomweb"));
	}

	private static WebDestinationConfigEntity entity(Long id) {
		WebDestinationConfigEntity entity = new WebDestinationConfigEntity("PACS", "https://pacs/dicomweb", "STOW-RS",
				"group-a");
		entity.setId(id);
		return entity;
	}

}