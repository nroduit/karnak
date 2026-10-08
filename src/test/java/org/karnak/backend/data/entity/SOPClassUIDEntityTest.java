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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class SOPClassUIDEntityTest {

	@Test
	void full_constructor_sets_all_fields() {
		SOPClassUIDEntity entity = new SOPClassUIDEntity("ciod", "1.2.3", "name");

		assertEquals("ciod", entity.getCiod());
		assertEquals("1.2.3", entity.getUid());
		assertEquals("name", entity.getName());
	}

	@Test
	void ciod_constructor_only_sets_the_ciod() {
		SOPClassUIDEntity entity = new SOPClassUIDEntity("ciod");

		assertEquals("ciod", entity.getCiod());
		assertNull(entity.getUid());
		assertNull(entity.getName());
	}

	@Test
	void exposes_its_properties_through_setters() {
		SOPClassUIDEntity entity = new SOPClassUIDEntity();
		entity.setId(4L);
		entity.setCiod("c");
		entity.setUid("u");
		entity.setName("n");

		assertEquals(4L, entity.getId());
		assertEquals("c", entity.getCiod());
		assertEquals("u", entity.getUid());
		assertEquals("n", entity.getName());
	}

	@Test
	void equal_instances_match_and_share_a_hash() {
		SOPClassUIDEntity a = sop(1L, "c", "u", "n");
		SOPClassUIDEntity b = sop(1L, "c", "u", "n");

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(a, a);
	}

	@Test
	void differs_by_field_type_or_null() {
		SOPClassUIDEntity base = sop(1L, "c", "u", "n");

		assertNotEquals(base, sop(2L, "c", "u", "n"));
		assertNotEquals(base, sop(1L, "x", "u", "n"));
		assertNotEquals(base, sop(1L, "c", "x", "n"));
		assertNotEquals(base, sop(1L, "c", "u", "x"));
		assertFalse(base.equals(null));
		// Declared as Object: equals(Object) must reject a foreign type at runtime,
		// which is what this asserts.
		Object foreign = "not-a-sop";
		assertNotEquals(base, foreign);
	}

	private static SOPClassUIDEntity sop(Long id, String ciod, String uid, String name) {
		SOPClassUIDEntity entity = new SOPClassUIDEntity(ciod, uid, name);
		entity.setId(id);
		return entity;
	}

}