/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class CollatorUtilsTest {

	@Nested
	class NullSafe {

		@Test
		void returns_empty_string_for_null() {
			assertEquals("", CollatorUtils.nullSafe(null));
		}

		@Test
		void returns_value_for_non_null() {
			assertEquals("hello", CollatorUtils.nullSafe("hello"));
		}

		@Test
		void returns_empty_string_for_empty() {
			assertEquals("", CollatorUtils.nullSafe(""));
		}

	}

	@Nested
	class Compare {

		@Test
		void returns_zero_for_equal_strings() {
			assertEquals(0, CollatorUtils.compare("abc", "abc"));
		}

		@Test
		void handles_null_values() {
			assertEquals(0, CollatorUtils.compare(null, null));
			assertTrue(CollatorUtils.compare(null, "a") < 0);
			assertTrue(CollatorUtils.compare("a", null) > 0);
		}

		@Test
		void sorts_alphabetically() {
			assertTrue(CollatorUtils.compare("apple", "banana") < 0);
			assertTrue(CollatorUtils.compare("banana", "apple") > 0);
		}

	}

	@Nested
	class StringComparator {

		@Test
		void sorts_list_of_strings() {
			var list = new java.util.ArrayList<>(List.of("cherry", "apple", "banana"));
			list.sort(CollatorUtils.stringComparator());
			assertEquals(List.of("apple", "banana", "cherry"), list);
		}

	}

	@Nested
	class Comparing {

		record Item(String name) {
		}

		@Test
		void sorts_objects_by_extracted_key() {
			var items = new java.util.ArrayList<>(List.of(new Item("cherry"), new Item("apple"), new Item("banana")));
			items.sort(CollatorUtils.comparing(Item::name));
			assertEquals("apple", items.get(0).name());
			assertEquals("banana", items.get(1).name());
			assertEquals("cherry", items.get(2).name());
		}

	}

	@Nested
	class ComparingThen {

		record Person(String lastName, String firstName) {
		}

		@Test
		void sorts_by_primary_then_secondary() {
			var people = new java.util.ArrayList<>(
					List.of(new Person("Doe", "John"), new Person("Doe", "Alice"), new Person("Abc", "Zoe")));

			Comparator<Person> comparator = CollatorUtils.comparingThen(Person::lastName, Person::firstName);
			people.sort(comparator);

			assertEquals("Abc", people.get(0).lastName());
			assertEquals("Alice", people.get(1).firstName());
			assertEquals("John", people.get(2).firstName());
		}

	}

}
