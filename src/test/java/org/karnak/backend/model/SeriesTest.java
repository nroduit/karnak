/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class SeriesTest {

	@Test
	void rejects_a_null_uid() {
		assertThrows(NullPointerException.class, () -> new Series(null));
	}

	@Test
	void starts_empty_with_a_blank_description() {
		Series series = new Series("1.2.3");

		assertTrue(series.isEmpty());
		assertEquals("", series.getSeriesDescription());
	}

	@Test
	void stores_description_and_date() {
		Series series = new Series("1.2.3");
		LocalDateTime date = LocalDateTime.of(2026, 1, 2, 3, 4);
		series.setSeriesDescription("CT scan");
		series.setSeriesDate(date);

		assertEquals("CT scan", series.getSeriesDescription());
		assertEquals(date, series.getSeriesDate());
	}

	@Test
	void adds_gets_and_removes_sop_instances() {
		Series series = new Series("1.2.3");
		SopInstance sop = new SopInstance("4.5.6");

		series.addSopInstance(sop);

		assertFalse(series.isEmpty());
		assertSame(sop, series.getSopInstance("4.5.6"));
		assertEquals(1, series.getSopInstances().size());
		assertEquals(1, series.getEntrySet().size());

		assertSame(sop, series.removeSopInstance("4.5.6"));
		assertTrue(series.isEmpty());
	}

	@Test
	void returns_null_for_an_unknown_sop_instance() {
		Series series = new Series("1.2.3");

		assertNull(series.getSopInstance("unknown"));
	}

	@Test
	void considers_series_equal_when_uid_matches() {
		Series a = new Series("1.2.3");
		Series b = new Series("1.2.3");
		Series c = new Series("9.9.9");

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, c);
	}

}