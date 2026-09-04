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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class KheopsAlbumsEntityTest {

	@Test
	void constructor_sets_the_album_properties() {
		KheopsAlbumsEntity entity = new KheopsAlbumsEntity("http://api", "destToken", "srcToken", "true");

		assertEquals("http://api", entity.getUrlAPI());
		assertEquals("destToken", entity.getAuthorizationDestination());
		assertEquals("srcToken", entity.getAuthorizationSource());
		assertEquals("true", entity.getCondition());
	}

	@Test
	void exposes_its_properties_through_setters() {
		KheopsAlbumsEntity entity = new KheopsAlbumsEntity();
		DestinationEntity destination = new DestinationEntity();
		entity.setId(2L);
		entity.setUrlAPI("u");
		entity.setAuthorizationDestination("d");
		entity.setAuthorizationSource("s");
		entity.setCondition("c");
		entity.setDestinationEntity(destination);

		assertEquals(2L, entity.getId());
		assertEquals("u", entity.getUrlAPI());
		assertEquals("d", entity.getAuthorizationDestination());
		assertEquals("s", entity.getAuthorizationSource());
		assertEquals("c", entity.getCondition());
		assertEquals(destination, entity.getDestinationEntity());
	}

	@Test
	void to_string_contains_the_url() {
		assertTrue(new KheopsAlbumsEntity("http://api", "d", "s", "c").toString().contains("http://api"));
	}

	@Test
	void equal_instances_match_and_share_a_hash() {
		KheopsAlbumsEntity a = new KheopsAlbumsEntity("u", "d", "s", "c");
		KheopsAlbumsEntity b = new KheopsAlbumsEntity("u", "d", "s", "c");

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(a, a);
		assertNotNull(a.getDestinationEntity());
	}

	@Test
	void differs_by_field_type_or_null() {
		KheopsAlbumsEntity base = new KheopsAlbumsEntity("u", "d", "s", "c");

		assertNotEquals(base, new KheopsAlbumsEntity("x", "d", "s", "c"));
		assertNotEquals(base, new KheopsAlbumsEntity("u", "x", "s", "c"));
		assertNotEquals(base, new KheopsAlbumsEntity("u", "d", "x", "c"));
		assertNotEquals(base, new KheopsAlbumsEntity("u", "d", "s", "x"));
		assertFalse(base.equals(null));
		// Declared as Object: equals(Object) must reject a foreign type at runtime,
		// which is what this asserts.
		Object foreign = "not-an-album";
		assertNotEquals(base, foreign);
	}

}