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

import java.awt.Rectangle;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.data.entity.IncludedTagEntity;
import org.karnak.backend.data.entity.TagEntity;

@DisplayNameGeneration(ReplaceUnderscores.class)
class JsonConvertersTest {

	@Nested
	class ArgumentToMap {

		@Test
		void maps_arguments_by_key() {
			Map<String, String> map = new ArgumentToMapConverter()
				.convert(List.of(new ArgumentEntity("days", "5"), new ArgumentEntity("seconds", "30")));

			assertEquals(Map.of("days", "5", "seconds", "30"), map);
		}

	}

	@Nested
	class TagListToStringList {

		@Test
		void extracts_the_tag_values() {
			List<TagEntity> tags = List.of(new IncludedTagEntity("(0010,0010)", null),
					new IncludedTagEntity("(0008,0020)", null));

			assertEquals(List.of("(0010,0010)", "(0008,0020)"), new TagListToStringListConverter().convert(tags));
		}

	}

	@Nested
	class RectangleListToStringList {

		@Test
		void formats_each_rectangle() {
			List<String> result = new RectangleListToStringListConverter()
				.convert(List.of(new Rectangle(1, 2, 3, 4), new Rectangle(5, 6, 7, 8)));

			assertEquals(List.of("1 2 3 4", "5 6 7 8"), result);
		}

	}

}