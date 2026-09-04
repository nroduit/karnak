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
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ProfileElementEntityTest {

	@Test
	void seven_argument_constructor_sets_the_core_fields() {
		ProfileEntity profile = new ProfileEntity();

		ProfileElementEntity entity = new ProfileElementEntity("Name", "codename", "cond", "D", "opt", 1, profile);

		assertEquals("Name", entity.getName());
		assertEquals("codename", entity.getCodename());
		assertEquals("cond", entity.getCondition());
		assertEquals("D", entity.getAction());
		assertEquals("opt", entity.getOption());
		assertEquals(1, entity.getPosition());
		assertSame(profile, entity.getProfileEntity());
	}

	@Test
	void eight_argument_constructor_keeps_the_arguments() {
		List<ArgumentEntity> args = List.of(new ArgumentEntity("k", "v"));

		ProfileElementEntity entity = new ProfileElementEntity("Name", "codename", null, "D", "opt", args, 2, null);

		assertEquals(args, entity.getArgumentEntities());
		assertEquals(2, entity.getPosition());
	}

	@Test
	void add_helpers_append_to_their_collections() {
		ProfileElementEntity entity = new ProfileElementEntity();

		entity.addIncludedTag(new IncludedTagEntity("(0010,0010)", entity));
		entity.addExceptedtags(new ExcludedTagEntity("(0008,0020)", entity));
		entity.addArgument(new ArgumentEntity("k", "v", entity));

		assertEquals(1, entity.getIncludedTagEntities().size());
		assertEquals(1, entity.getExcludedTagEntities().size());
		assertEquals(1, entity.getArgumentEntities().size());
	}

	@Test
	void exposes_its_properties_through_setters() {
		ProfileElementEntity entity = new ProfileElementEntity();
		entity.setId(1L);
		entity.setName("N");
		entity.setCodename("c");
		entity.setCondition("cond");
		entity.setAction("X");
		entity.setOption("o");
		entity.setPosition(3);
		entity.setIncludedTagEntities(List.of(new IncludedTagEntity("(0010,0010)", entity)));
		entity.setExcludedTagEntities(List.of(new ExcludedTagEntity("(0008,0020)", entity)));
		entity.setArgumentEntities(List.of(new ArgumentEntity("k", "v")));

		assertEquals(1L, entity.getId());
		assertEquals("N", entity.getName());
		assertEquals("c", entity.getCodename());
		assertEquals("cond", entity.getCondition());
		assertEquals("X", entity.getAction());
		assertEquals("o", entity.getOption());
		assertEquals(3, entity.getPosition());
		assertEquals(1, entity.getIncludedTagEntities().size());
		assertEquals(1, entity.getExcludedTagEntities().size());
		assertEquals(1, entity.getArgumentEntities().size());
	}

	@Test
	void equal_instances_match_and_share_a_hash() {
		ProfileElementEntity a = element(1L, "N");
		ProfileElementEntity b = element(1L, "N");

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(a, a);
	}

	@Test
	void differs_by_field_type_or_null() {
		ProfileElementEntity base = element(1L, "N");

		assertNotEquals(base, element(2L, "N"));
		assertNotEquals(base, element(1L, "Other"));
		// Declared as Object: equals(Object) must reject a foreign type at runtime,
		// which is what this asserts.
		Object foreign = "not-an-element";
		assertNotEquals(foreign, base);
		assertFalse(base.equals(null));
	}

	private static ProfileElementEntity element(Long id, String name) {
		ProfileElementEntity entity = new ProfileElementEntity(name, "codename", "cond", "D", "opt", 1, null);
		entity.setId(id);
		return entity;
	}

}