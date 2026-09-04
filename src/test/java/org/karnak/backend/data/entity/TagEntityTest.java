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

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

/**
 * Exercises the shared {@link TagEntity} behaviour through its concrete subclasses
 * {@link IncludedTagEntity} and {@link ExcludedTagEntity}.
 */
@DisplayNameGeneration(ReplaceUnderscores.class)
class TagEntityTest {

	@Test
	void constructor_sets_value_and_profile_element() {
		ProfileElementEntity element = new ProfileElementEntity();

		IncludedTagEntity tag = new IncludedTagEntity("(0010,0010)", element);

		assertEquals("(0010,0010)", tag.getTagValue());
		assertSame(element, tag.getProfileElementEntity());
	}

	@Test
	void exposes_its_properties_through_setters() {
		ExcludedTagEntity tag = new ExcludedTagEntity();
		ProfileElementEntity element = new ProfileElementEntity();
		tag.setId(2L);
		tag.setTagValue("(0008,0020)");
		tag.setProfileElementEntity(element);

		assertEquals(2L, tag.getId());
		assertEquals("(0008,0020)", tag.getTagValue());
		assertSame(element, tag.getProfileElementEntity());
	}

	@Test
	void equal_instances_match_and_share_a_hash() {
		IncludedTagEntity a = included(1L, "(0010,0010)");
		IncludedTagEntity b = included(1L, "(0010,0010)");

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(a, a);
	}

	@Test
	void differs_by_field_subtype_or_null() {
		IncludedTagEntity base = included(1L, "(0010,0010)");

		assertNotEquals(base, included(2L, "(0010,0010)"));
		assertNotEquals(base, included(1L, "(0008,0020)"));
		assertFalse(base.equals(null));
		// Different concrete subclass -> not equal (getClass() check).
		TagEntity excluded = new ExcludedTagEntity("(0010,0010)", null);
		excluded.setId(1L);
		assertNotEquals(base, excluded);
	}

	private static IncludedTagEntity included(Long id, String value) {
		IncludedTagEntity tag = new IncludedTagEntity(value, null);
		tag.setId(id);
		return tag;
	}

}