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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class SecretEntityTest {

	@Test
	void key_constructor_sets_the_key_and_a_creation_date() {
		SecretEntity entity = new SecretEntity(new byte[] { 1, 2, 3 });

		assertArrayEquals(new byte[] { 1, 2, 3 }, entity.getSecretKey());
		assertNotNull(entity.getCreationDate());
	}

	@Test
	void project_constructor_links_the_project() {
		ProjectEntity project = new ProjectEntity();

		SecretEntity entity = new SecretEntity(project, new byte[] { 9 });

		assertEquals(project, entity.getProjectEntity());
		assertArrayEquals(new byte[] { 9 }, entity.getSecretKey());
	}

	@Test
	void exposes_its_properties_through_setters() {
		SecretEntity entity = new SecretEntity();
		LocalDateTime now = LocalDateTime.of(2024, 1, 1, 12, 0);
		entity.setId(3L);
		entity.setSecretKey(new byte[] { 7 });
		entity.setCreationDate(now);
		entity.setActive(true);

		assertEquals(3L, entity.getId());
		assertArrayEquals(new byte[] { 7 }, entity.getSecretKey());
		assertEquals(now, entity.getCreationDate());
		assertTrue(entity.isActive());
	}

	@Test
	void equal_instances_match_and_share_a_hash() {
		LocalDateTime date = LocalDateTime.of(2024, 1, 1, 12, 0);
		UUID uuid = UUID.fromString("11111111-2222-3333-4444-555555555555");
		SecretEntity a = secret(1L, new byte[] { 1 }, date, true);
		SecretEntity b = secret(1L, new byte[] { 1 }, date, true);
		a.setUuid(uuid);
		b.setUuid(uuid);

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertTrue(a.equals(a));
	}

	@Test
	void differs_by_field_type_or_null() {
		LocalDateTime date = LocalDateTime.of(2024, 1, 1, 12, 0);
		SecretEntity base = secret(1L, new byte[] { 1 }, date, true);

		assertNotEquals(base, secret(2L, new byte[] { 1 }, date, true));
		assertNotEquals(base, secret(1L, new byte[] { 2 }, date, true));
		assertNotEquals(base, secret(1L, new byte[] { 1 }, date, false));
		assertFalse(base.equals(null));
		assertNotEquals(base, "not-a-secret");
	}

	private static SecretEntity secret(Long id, byte[] key, LocalDateTime date, boolean active) {
		SecretEntity entity = new SecretEntity();
		entity.setId(id);
		entity.setSecretKey(key);
		entity.setCreationDate(date);
		entity.setActive(active);
		return entity;
	}

}