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

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.enums.AuthConfigType;

@DisplayNameGeneration(ReplaceUnderscores.class)
class AuthConfigEntityTest {

	private static AuthConfigEntity config(Long id, String code) {
		AuthConfigEntity entity = new AuthConfigEntity();
		entity.setId(id);
		entity.setCode(code);
		entity.setClientSecret("secret");
		entity.setClientId("client");
		entity.setAccessTokenUrl("http://token");
		entity.setScope("scope");
		entity.setAuthConfigType(AuthConfigType.values()[0]);
		return entity;
	}

	@Test
	void exposes_all_properties() {
		AuthConfigEntity entity = config(1L, "my-code");

		assertEquals(1L, entity.getId());
		assertEquals("my-code", entity.getCode());
		assertEquals("secret", entity.getClientSecret());
		assertEquals("client", entity.getClientId());
		assertEquals("http://token", entity.getAccessTokenUrl());
		assertEquals("scope", entity.getScope());
		assertEquals(AuthConfigType.values()[0], entity.getAuthConfigType());
	}

	@Test
	void to_string_is_the_code() {
		assertEquals("my-code", config(1L, "my-code").toString());
	}

	@Test
	void equal_instances_match_and_share_a_hash() {
		AuthConfigEntity a = config(1L, "code");
		AuthConfigEntity b = config(1L, "code");

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	void differs_by_field_type_or_null() {
		AuthConfigEntity base = config(1L, "code");

		assertNotEquals(base, config(2L, "code"));
		assertNotEquals(base, config(1L, "other"));
		assertFalse(base.equals(null));
		// Declared as Object: equals(Object) must reject a foreign type at runtime,
		// which is what this asserts.
		Object foreign = "not-a-config";
		assertNotEquals(base, foreign);
	}

}