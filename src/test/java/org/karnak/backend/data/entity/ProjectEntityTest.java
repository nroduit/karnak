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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ProjectEntityTest {

	@Test
	void default_constructor_initialises_empty_collections() {
		ProjectEntity entity = new ProjectEntity();

		assertTrue(entity.getSecretEntities().isEmpty());
		assertTrue(entity.getDestinationEntities().isEmpty());
	}

	@Test
	void exposes_its_properties_through_setters() {
		ProjectEntity entity = new ProjectEntity();
		ProfileEntity profile = new ProfileEntity();
		entity.setId(1L);
		entity.setName("Project");
		entity.setProfileEntity(profile);
		entity.setSecretEntities(List.of());
		entity.setDestinationEntities(List.of());

		assertEquals(1L, entity.getId());
		assertEquals("Project", entity.getName());
		assertSame(profile, entity.getProfileEntity());
		assertTrue(entity.getSecretEntities().isEmpty());
	}

	@Test
	void retrieve_active_secret_returns_the_active_one_or_null() {
		ProjectEntity entity = new ProjectEntity();
		assertNull(entity.retrieveActiveSecret());

		SecretEntity inactive = new SecretEntity(new byte[] { 1 });
		SecretEntity active = new SecretEntity(new byte[] { 2 });
		entity.addActiveSecretEntity(inactive);
		entity.addActiveSecretEntity(active);

		// Adding the second active secret deactivates the first.
		assertSame(active, entity.retrieveActiveSecret());
		assertTrue(active.isActive());
		assertFalse(inactive.isActive());
	}

	@Test
	void add_active_secret_entity_sets_back_reference_on_secret() {
		ProjectEntity entity = new ProjectEntity();
		entity.setId(1L);
		SecretEntity secret = new SecretEntity(new byte[16]);

		entity.addActiveSecretEntity(secret);

		// The owning side of the @ManyToOne must point back to the project so
		// Hibernate writes a non-null project_id FK on insert.
		assertSame(entity, secret.getProjectEntity());
	}

	@Test
	void apply_active_secret_activates_only_the_target() {
		ProjectEntity entity = new ProjectEntity();
		SecretEntity first = new SecretEntity(new byte[] { 1 });
		SecretEntity second = new SecretEntity(new byte[] { 2 });
		entity.getSecretEntities().add(first);
		entity.getSecretEntities().add(second);
		first.setActive(true);

		entity.applyActiveSecret(second);

		assertFalse(first.isActive());
		assertTrue(second.isActive());
	}

	@Test
	void build_label_secret_mentions_the_creation_date() {
		byte[] key = new byte[16];
		for (int i = 0; i < key.length; i++) {
			key[i] = (byte) i;
		}
		SecretEntity secret = new SecretEntity(key);
		secret.setCreationDate(LocalDateTime.of(2024, 1, 15, 10, 30, 0));

		String label = ProjectEntity.buildLabelSecret(secret);

		assertTrue(label.contains("created"), label);
	}

	@Test
	void equal_instances_match_and_share_a_hash() {
		ProjectEntity a = named("Project");
		ProjectEntity b = named("Project");

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(a, a);
	}

	@Test
	void differs_by_field_type_or_null() {
		ProjectEntity base = named("Project");

		assertNotEquals(base, named("Other"));
		assertFalse(base.equals(null));
		assertNotEquals(base, "not-a-project");
	}

	private static ProjectEntity named(String name) {
		ProjectEntity entity = new ProjectEntity();
		entity.setId(1L);
		entity.setName(name);
		return entity;
	}

}