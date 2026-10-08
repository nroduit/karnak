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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ArgumentEntityTest {

	@Test
	void two_argument_constructor_sets_key_and_value() {
		ArgumentEntity entity = new ArgumentEntity("key", "value");

		assertEquals("key", entity.getArgumentKey());
		assertEquals("value", entity.getArgumentValue());
	}

	@Test
	void three_argument_constructor_links_the_profile_element() {
		ProfileElementEntity element = new ProfileElementEntity();

		ArgumentEntity entity = new ArgumentEntity("key", "value", element);

		assertSame(element, entity.getProfileElementEntity());
	}

	@Test
	void exposes_its_properties_through_setters() {
		ArgumentEntity entity = new ArgumentEntity();
		ProfileElementEntity element = new ProfileElementEntity();
		entity.setId(5L);
		entity.setArgumentKey("k");
		entity.setArgumentValue("v");
		entity.setProfileElementEntity(element);

		assertEquals(5L, entity.getId());
		assertEquals("k", entity.getArgumentKey());
		assertEquals("v", entity.getArgumentValue());
		assertSame(element, entity.getProfileElementEntity());
	}

	@Test
	void equal_instances_match_and_share_a_hash() {
		ArgumentEntity a = arg(1L, "k", "v");
		ArgumentEntity b = arg(1L, "k", "v");

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(a, a);
	}

	@Test
	void differs_by_field_type_or_null() {
		ArgumentEntity base = arg(1L, "k", "v");

		assertNotEquals(base, arg(2L, "k", "v"));
		assertNotEquals(base, arg(1L, "other", "v"));
		assertNotEquals(base, arg(1L, "k", "other"));
		assertNotNull(base);
		// Declared as Object: equals(Object) must reject a foreign type at runtime,
		// which is what this asserts.
		Object foreign = "not-an-argument";
		assertNotEquals(foreign, base);
	}

	private static ArgumentEntity arg(Long id, String key, String value) {
		ArgumentEntity entity = new ArgumentEntity(key, value);
		entity.setId(id);
		return entity;
	}

}
