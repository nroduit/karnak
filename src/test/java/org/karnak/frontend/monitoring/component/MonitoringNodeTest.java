/*
 * Copyright (c) 2022-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.monitoring.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.karnak.frontend.monitoring.component.MonitoringNode.DestinationNode;
import org.karnak.frontend.monitoring.component.MonitoringNode.ErrorNode;
import org.karnak.frontend.monitoring.component.MonitoringNode.SeriesNode;
import org.karnak.frontend.monitoring.component.MonitoringNode.StudyNode;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MonitoringNodeTest {

	@Test
	void destination_key_and_errors_and_display_name() {
		DestinationNode withForward = new DestinationNode(5L, "GATEWAY", "AET_DEST", 3, 12, 100, 98, 2);
		assertEquals("d:5", withForward.key());
		assertTrue(withForward.hasErrors());
		assertEquals("GATEWAY → AET_DEST", withForward.displayName());

		DestinationNode noErrors = new DestinationNode(6L, "", "AET_DEST", 1, 1, 1, 1, 0);
		assertFalse(noErrors.hasErrors());
		assertEquals("AET_DEST", noErrors.displayName());
	}

	@Test
	void study_key_uses_destination_and_study_uid() {
		StudyNode study = new StudyNode(5L, "1.2.3", null, "CT chest", null, null, null, null, null, null, null, 4, 40,
				0, 0, null, null);
		assertEquals("st:5:1.2.3", study.key());
		assertFalse(study.hasErrors());
	}

	@Test
	void series_key_uses_destination_and_serie_uid_and_flags_errors() {
		SeriesNode series = new SeriesNode(5L, "1.2.3", null, null, null, null, null, null, null, null, null, "1.2.3.4",
				null, "axial", null, "CT", null, null, null, 40, 39, 1, null, null);
		assertEquals("se:5:1.2.3.4", series.key());
		assertTrue(series.hasErrors());
	}

	@Test
	void error_node_is_unique_per_parent_and_always_an_error() {
		SeriesNode series = new SeriesNode(5L, "1.2.3", null, null, null, null, null, null, null, null, null, "1.2.3.4",
				null, "axial", null, "CT", null, null, null, 40, 39, 1, null, null);
		ErrorNode error = new ErrorNode(series.key(), "timeout", 3);
		assertEquals("se:5:1.2.3.4|err:timeout", error.key());
		assertTrue(error.hasErrors());
		assertEquals(3, error.instances());
	}

}
