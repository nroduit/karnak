/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class SerieSummaryNotificationTest {

	private static SerieSummaryNotification sample() {
		SerieSummaryNotification serie = new SerieSummaryNotification();
		serie.setSerieUid("1.2.3");
		serie.setSerieDescription("CT");
		serie.setSerieDate(LocalDateTime.of(2026, 1, 1, 0, 0));
		serie.setNbTransferSent(5);
		serie.setNbTransferNotSent(1);
		serie.setContainsError(true);
		serie.setUnTransferedReasons(new LinkedHashSet<>(Set.of("reason")));
		serie.setTransferredModalities(new LinkedHashSet<>(Set.of("CT")));
		serie.setTransferredSopClassUid(new LinkedHashSet<>(Set.of("1.2.840")));
		return serie;
	}

	@Test
	void stores_and_exposes_all_fields() {
		SerieSummaryNotification serie = sample();

		assertEquals("1.2.3", serie.getSerieUid());
		assertEquals("CT", serie.getSerieDescription());
		assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), serie.getSerieDate());
		assertEquals(5, serie.getNbTransferSent());
		assertEquals(1, serie.getNbTransferNotSent());
		assertTrue(serie.isContainsError());
	}

	@Test
	void joins_collections_with_a_comma_separator() {
		SerieSummaryNotification serie = new SerieSummaryNotification();
		serie.setUnTransferedReasons(new LinkedHashSet<>(java.util.List.of("a", "b")));
		serie.setTransferredModalities(new LinkedHashSet<>(java.util.List.of("CT", "MR")));
		serie.setTransferredSopClassUid(new LinkedHashSet<>(java.util.List.of("1", "2")));

		assertEquals("a,b", serie.toStringUnTransferredReasons());
		assertEquals("CT,MR", serie.toStringTransferredModalities());
		assertEquals("1,2", serie.toStringTransferredSopClassUid());
	}

	@Test
	void considers_two_instances_with_the_same_content_equal() {
		assertEquals(sample(), sample());
		assertEquals(sample().hashCode(), sample().hashCode());
	}

	@Test
	void differs_when_a_field_changes() {
		SerieSummaryNotification other = sample();
		other.setSerieUid("9.9.9");

		assertNotEquals(sample(), other);
	}

}