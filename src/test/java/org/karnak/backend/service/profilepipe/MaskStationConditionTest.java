/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.profilepipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class MaskStationConditionTest {

	@Nested
	class Constructors {

		@Test
		void full_constructor_sets_all_fields() {
			var condition = new MaskStationCondition("CT_SCANNER", 512L, 512L);
			assertEquals("CT_SCANNER", condition.getStationName());
			assertEquals(512L, condition.getImageWidth());
			assertEquals(512L, condition.getImageHeight());
		}

		@Test
		void string_constructor_parses_dimensions_from_strings() {
			var condition = new MaskStationCondition("MR1", "1024", "768");
			assertEquals("MR1", condition.getStationName());
			assertEquals(1024L, condition.getImageWidth());
			assertEquals(768L, condition.getImageHeight());
		}

		@Test
		void string_constructor_handles_null_dimensions() {
			var condition = new MaskStationCondition("CT1", (String) null, (String) null);
			assertEquals("CT1", condition.getStationName());
			assertNull(condition.getImageWidth());
			assertNull(condition.getImageHeight());
		}

		@Test
		void station_name_only_constructor_leaves_dimensions_null() {
			var condition = new MaskStationCondition("DX1");
			assertEquals("DX1", condition.getStationName());
			assertNull(condition.getImageWidth());
			assertNull(condition.getImageHeight());
		}

	}

	@Nested
	class Equality {

		@Test
		void equal_when_all_fields_match() {
			var a = new MaskStationCondition("CT1", 512L, 512L);
			var b = new MaskStationCondition("CT1", 512L, 512L);
			assertEquals(a, b);
			assertEquals(a.hashCode(), b.hashCode());
		}

		@Test
		void not_equal_when_station_name_differs() {
			var a = new MaskStationCondition("CT1", 512L, 512L);
			var b = new MaskStationCondition("CT2", 512L, 512L);
			assertNotEquals(a, b);
		}

		@Test
		void not_equal_when_dimensions_differ() {
			var a = new MaskStationCondition("CT1", 512L, 512L);
			var b = new MaskStationCondition("CT1", 256L, 512L);
			assertNotEquals(a, b);
		}

		@Test
		void not_equal_to_null_or_different_type() {
			var condition = new MaskStationCondition("CT1");
			assertNotEquals(null, condition);
			assertNotEquals("CT1", condition);
		}

	}

	@Nested
	class Setters {

		@Test
		void setters_update_fields() {
			var condition = new MaskStationCondition("OLD");
			condition.setStationName("NEW");
			condition.setImageWidth(100L);
			condition.setImageHeight(200L);
			assertEquals("NEW", condition.getStationName());
			assertEquals(100L, condition.getImageWidth());
			assertEquals(200L, condition.getImageHeight());
		}

	}

}
