/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.kheops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class MetadataSwitchingTest {

	@Test
	void exposes_the_uids_and_starts_not_applied() {
		MetadataSwitching switching = new MetadataSwitching("study", "series", "sop");

		assertEquals("study", switching.getStudyInstanceUID());
		assertEquals("series", switching.getSeriesInstanceUID());
		assertEquals("sop", switching.getSopInstanceUID());
		assertFalse(switching.isApplied());
	}

	@Test
	void tracks_the_applied_flag() {
		MetadataSwitching switching = new MetadataSwitching("study", "series", "sop");

		switching.setApplied(true);

		assertTrue(switching.isApplied());
	}

}