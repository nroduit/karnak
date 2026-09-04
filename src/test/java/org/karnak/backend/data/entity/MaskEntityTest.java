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

import java.awt.Rectangle;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class MaskEntityTest {

	@Test
	void full_constructor_sets_all_fields() {
		ProfileEntity profile = new ProfileEntity();

		MaskEntity entity = new MaskEntity("STATION", 100L, 200L, "FFFFFF", profile);

		assertEquals("STATION", entity.getStationName());
		assertEquals(100L, entity.getImageWidth());
		assertEquals(200L, entity.getImageHeight());
		assertEquals("FFFFFF", entity.getColor());
		assertSame(profile, entity.getProfileEntity());
	}

	@Test
	void short_constructor_leaves_dimensions_null() {
		MaskEntity entity = new MaskEntity("STATION", "FFFFFF", new ProfileEntity());

		assertEquals("STATION", entity.getStationName());
		assertEquals("FFFFFF", entity.getColor());
		assertEquals(null, entity.getImageWidth());
		assertEquals(null, entity.getImageHeight());
	}

	@Test
	void add_rectangle_from_a_valid_string_appends_it() {
		MaskEntity entity = new MaskEntity();

		entity.addRectangle("10 20 30 40");

		assertEquals(1, entity.getRectangles().size());
		assertEquals(new Rectangle(10, 20, 30, 40), entity.getRectangles().getFirst());
	}

	@Test
	void add_rectangle_from_an_invalid_string_is_ignored() {
		MaskEntity entity = new MaskEntity();

		entity.addRectangle("not-a-rectangle");

		assertEquals(0, entity.getRectangles().size());
	}

	@Test
	void add_rectangle_object_appends_it() {
		MaskEntity entity = new MaskEntity();

		entity.addRectangle(new Rectangle(1, 2, 3, 4));

		assertEquals(1, entity.getRectangles().size());
	}

	@Test
	void exposes_its_properties_through_setters() {
		MaskEntity entity = new MaskEntity();
		entity.setId(1L);
		entity.setStationName("S");
		entity.setImageWidth(10L);
		entity.setImageHeight(20L);
		entity.setColor("000000");
		entity.setRectangles(List.of(new Rectangle(0, 0, 1, 1)));

		assertEquals(1L, entity.getId());
		assertEquals("S", entity.getStationName());
		assertEquals(10L, entity.getImageWidth());
		assertEquals(20L, entity.getImageHeight());
		assertEquals("000000", entity.getColor());
		assertEquals(1, entity.getRectangles().size());
	}

	@Test
	void equal_instances_match_and_share_a_hash() {
		MaskEntity a = mask(1L, "S");
		MaskEntity b = mask(1L, "S");

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(a, a);
	}

	@Test
	void differs_by_field_type_or_null() {
		MaskEntity base = mask(1L, "S");

		assertNotEquals(base, mask(2L, "S"));
		assertNotEquals(base, mask(1L, "OTHER"));
		assertFalse(base.equals(null));
		// Declared as Object: equals(Object) must reject a foreign type at runtime,
		// which is what this asserts.
		Object foreign = "not-a-mask";
		assertNotEquals(base, foreign);
	}

	private static MaskEntity mask(Long id, String station) {
		MaskEntity entity = new MaskEntity(station, "FFFFFF", null);
		entity.setId(id);
		return entity;
	}

}