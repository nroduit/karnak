/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class VmSpecTest {

	@Nested
	class Parse {

		@Test
		void returns_null_for_null_input() {
			assertNull(VmSpec.parse(null));
		}

		@Test
		void returns_null_for_invalid_input() {
			assertNull(VmSpec.parse(""));
			assertNull(VmSpec.parse("abc"));
			assertNull(VmSpec.parse("1-"));
			assertNull(VmSpec.parse("-1"));
		}

		@Test
		void parses_fixed_value() {
			var spec = VmSpec.parse("1");
			assertEquals(1, spec.min());
			assertEquals(1, spec.max());
			assertEquals(1, spec.factor());
		}

		@Test
		void parses_larger_fixed_value() {
			var spec = VmSpec.parse("16");
			assertEquals(16, spec.min());
			assertEquals(16, spec.max());
			assertEquals(1, spec.factor());
		}

		@Test
		void parses_fixed_range() {
			var spec = VmSpec.parse("1-2");
			assertEquals(1, spec.min());
			assertEquals(2, spec.max());
			assertEquals(1, spec.factor());
		}

		@Test
		void parses_wider_fixed_range() {
			var spec = VmSpec.parse("1-99");
			assertEquals(1, spec.min());
			assertEquals(99, spec.max());
			assertEquals(1, spec.factor());
		}

		@Test
		void parses_unbounded_range() {
			var spec = VmSpec.parse("1-n");
			assertEquals(1, spec.min());
			assertEquals(Integer.MAX_VALUE, spec.max());
			assertEquals(1, spec.factor());
		}

		@Test
		void parses_unbounded_with_factor() {
			var spec = VmSpec.parse("2-2n");
			assertEquals(2, spec.min());
			assertEquals(Integer.MAX_VALUE, spec.max());
			assertEquals(2, spec.factor());
		}

		@Test
		void parses_unbounded_with_factor_3() {
			var spec = VmSpec.parse("3-3n");
			assertEquals(3, spec.min());
			assertEquals(Integer.MAX_VALUE, spec.max());
			assertEquals(3, spec.factor());
		}

		@Test
		void trims_whitespace() {
			var spec = VmSpec.parse("  1-n  ");
			assertEquals(1, spec.min());
			assertEquals(Integer.MAX_VALUE, spec.max());
		}

	}

	@Nested
	class Matches {

		@Test
		void fixed_value_matches_exact_count() {
			var spec = VmSpec.parse("1");
			assertTrue(spec.matches(1));
			assertFalse(spec.matches(0));
			assertFalse(spec.matches(2));
		}

		@Test
		void fixed_range_matches_within_bounds() {
			var spec = VmSpec.parse("1-3");
			assertFalse(spec.matches(0));
			assertTrue(spec.matches(1));
			assertTrue(spec.matches(2));
			assertTrue(spec.matches(3));
			assertFalse(spec.matches(4));
		}

		@Test
		void unbounded_matches_any_count_above_min() {
			var spec = VmSpec.parse("1-n");
			assertFalse(spec.matches(0));
			assertTrue(spec.matches(1));
			assertTrue(spec.matches(100));
			assertTrue(spec.matches(10000));
		}

		@Test
		void factor_constraint_enforces_multiples() {
			var spec = VmSpec.parse("2-2n");
			assertFalse(spec.matches(1));
			assertTrue(spec.matches(2));
			assertFalse(spec.matches(3));
			assertTrue(spec.matches(4));
			assertFalse(spec.matches(5));
			assertTrue(spec.matches(6));
		}

		@Test
		void factor_3_constraint_enforces_multiples_of_3() {
			var spec = VmSpec.parse("3-3n");
			assertFalse(spec.matches(2));
			assertTrue(spec.matches(3));
			assertFalse(spec.matches(4));
			assertTrue(spec.matches(6));
			assertTrue(spec.matches(9));
		}

	}

}
