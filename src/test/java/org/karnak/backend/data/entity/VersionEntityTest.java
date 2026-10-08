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

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class VersionEntityTest {

	private static VersionEntity version(Long id, long setup) {
		VersionEntity entity = new VersionEntity();
		entity.setId(id);
		entity.setGatewaySetup(setup);
		return entity;
	}

	@Test
	void exposes_its_properties() {
		VersionEntity entity = version(1L, 42L);

		assertEquals(1L, entity.getId());
		assertEquals(42L, entity.getGatewaySetup());
	}

	@Test
	void equal_instances_match_and_share_a_hash() {
		VersionEntity a = version(1L, 42L);
		VersionEntity b = version(1L, 42L);

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(a, a);
	}

	@Test
	void differs_by_field_or_type_or_null() {
		VersionEntity base = version(1L, 42L);

		assertNotEquals(base, version(2L, 42L));
		assertNotEquals(base, version(1L, 99L));
		assertNotEquals(base, null);
		// Declared as Object: equals(Object) must reject a foreign type at runtime,
		// which is what this asserts.
		Object foreign = "not-a-version";
		assertNotEquals(base, foreign);
	}

}