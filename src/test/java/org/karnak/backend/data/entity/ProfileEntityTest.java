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

import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ProfileEntityTest {

	@Test
	void four_argument_constructor_defaults_by_default_to_false() {
		ProfileEntity entity = new ProfileEntity("Name", "1.0", "1.2", "issuer");

		assertEquals("Name", entity.getName());
		assertEquals("1.0", entity.getVersion());
		assertEquals("1.2", entity.getMinimumKarnakVersion());
		assertEquals("issuer", entity.getDefaultIssuerOfPatientId());
		assertFalse(entity.getByDefault());
	}

	@Test
	void five_argument_constructor_keeps_the_by_default_flag() {
		ProfileEntity entity = new ProfileEntity("Name", "1.0", "1.2", "issuer", true);

		assertTrue(entity.getByDefault());
	}

	@Test
	void exposes_its_properties_through_setters() {
		ProfileEntity entity = new ProfileEntity();
		entity.setId(1L);
		entity.setName("N");
		entity.setVersion("v");
		entity.setMinimumKarnakVersion("mkv");
		entity.setDefaultIssuerOfPatientId("issuer");
		entity.setByDefault(true);
		entity.setProjectEntities(List.of(new ProjectEntity()));

		assertEquals(1L, entity.getId());
		assertEquals("N", entity.getName());
		assertEquals("v", entity.getVersion());
		assertEquals("mkv", entity.getMinimumKarnakVersion());
		assertEquals("issuer", entity.getDefaultIssuerOfPatientId());
		assertTrue(entity.getByDefault());
		assertEquals(1, entity.getProjectEntities().size());
	}

	@Test
	void add_profile_pipe_and_mask_append_to_their_collections() {
		ProfileEntity entity = new ProfileEntity();

		entity.addProfilePipe(new ProfileElementEntity());
		entity.addMask(new MaskEntity());

		assertEquals(1, entity.getProfileElementEntities().size());
		assertEquals(1, entity.getMaskEntities().size());
	}

	@Test
	void collections_can_be_replaced() {
		ProfileEntity entity = new ProfileEntity();
		entity.setProfileElementEntities(java.util.Set.of(new ProfileElementEntity()));
		entity.setMaskEntities(java.util.Set.of(new MaskEntity()));

		assertEquals(1, entity.getProfileElementEntities().size());
		assertEquals(1, entity.getMaskEntities().size());
	}

	@Test
	void equal_instances_match_and_share_a_hash() {
		ProfileEntity a = profile(1L, "N");
		ProfileEntity b = profile(1L, "N");

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(a, a);
	}

	@Test
	void differs_by_field_type_or_null() {
		ProfileEntity base = profile(1L, "N");

		assertNotEquals(base, profile(2L, "N"));
		assertNotEquals(base, profile(1L, "Other"));
		// Declared as Object: equals(Object) must reject a foreign type at runtime,
		// which is what this asserts.
		Object foreign = "not-a-profile";
		assertNotEquals(foreign, base);
		assertFalse(base.equals(null));
	}

	private static ProfileEntity profile(Long id, String name) {
		ProfileEntity entity = new ProfileEntity(name, "1.0", "1.2", "issuer", false);
		entity.setId(id);
		return entity;
	}

}