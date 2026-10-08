/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class CounterTest {

	@Test
	void starts_at_zero() {
		assertEquals(0, new Counter(5).getCount());
	}

	@Test
	void increment_advances_by_the_interval_and_returns_the_new_value() {
		Counter counter = new Counter(5);

		assertEquals(5, counter.increment());
		assertEquals(10, counter.increment());
		assertEquals(10, counter.getCount());
	}

	@Test
	void reset_returns_the_count_to_zero() {
		Counter counter = new Counter(3);
		counter.increment();
		counter.increment();

		counter.resetCount();

		assertEquals(0, counter.getCount());
	}

}