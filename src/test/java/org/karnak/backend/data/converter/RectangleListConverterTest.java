/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.data.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Rectangle;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class RectangleListConverterTest {

	private final RectangleListConverter converter = new RectangleListConverter();

	@Test
	void rectangle_to_string_uses_space_separated_coordinates() {
		assertEquals("10 20 30 40", RectangleListConverter.rectangleToString(new Rectangle(10, 20, 30, 40)));
	}

	@Test
	void string_to_rectangle_parses_four_coordinates() {
		assertEquals(new Rectangle(10, 20, 30, 40), RectangleListConverter.stringToRectangle("10 20 30 40"));
	}

	@Test
	void string_to_rectangle_returns_null_for_the_wrong_arity() {
		assertNull(RectangleListConverter.stringToRectangle("10 20 30"));
	}

	@Test
	void to_database_column_joins_rectangles_with_a_semicolon() {
		String db = converter.convertToDatabaseColumn(List.of(new Rectangle(1, 2, 3, 4), new Rectangle(5, 6, 7, 8)));

		assertEquals("1 2 3 4;5 6 7 8", db);
	}

	@Test
	void to_entity_attribute_round_trips_the_database_value() {
		List<Rectangle> rectangles = converter.convertToEntityAttribute("1 2 3 4;5 6 7 8");

		assertEquals(2, rectangles.size());
		assertEquals(new Rectangle(1, 2, 3, 4), rectangles.getFirst());
		assertEquals(new Rectangle(5, 6, 7, 8), rectangles.get(1));
	}

}