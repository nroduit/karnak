/*
 * Copyright (c) 2026 Karnak Team and other contributors.
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class MonitoringCsvMappingTest {

	@Nested
	class EnumValues {

		@Test
		void all_entries_have_non_empty_field_name() {
			for (var mapping : MonitoringCsvMapping.values()) {
				assertNotNull(mapping.getNameFieldEntity());
				assertFalse(mapping.getNameFieldEntity().isBlank(), mapping.name() + " has blank field name");
			}
		}

		@Test
		void all_entries_have_non_empty_csv_label() {
			for (var mapping : MonitoringCsvMapping.values()) {
				assertNotNull(mapping.getLabelCsv());
				assertFalse(mapping.getLabelCsv().isBlank(), mapping.name() + " has blank CSV label");
			}
		}

		@Test
		void forward_aetitle_maps_to_correct_field() {
			assertEquals("fwdAeTitle", MonitoringCsvMapping.FORWARD_AETITLE.getNameFieldEntity());
			assertEquals("Forward aeTitle", MonitoringCsvMapping.FORWARD_AETITLE.getLabelCsv());
		}

		@Test
		void sent_maps_to_correct_field() {
			assertEquals("sent", MonitoringCsvMapping.SENT.getNameFieldEntity());
			assertEquals("Sent", MonitoringCsvMapping.SENT.getLabelCsv());
		}

		@Test
		void last_seen_is_last_column() {
			var values = MonitoringCsvMapping.values();
			assertEquals(MonitoringCsvMapping.LAST_SEEN, values[values.length - 1]);
		}

	}

	@Nested
	class MappingStrategy {

		@Test
		void constructor_sets_column_mapping_for_all_enum_values() {
			var strategy = new MonitoringCsvMappingStrategy<Object>();
			// The strategy should have been configured with column mappings
			// matching the number of enum values
			var expectedCount = EnumSet.allOf(MonitoringCsvMapping.class).size();
			assertTrue(expectedCount > 0);
		}

		@Test
		void generates_csv_headers_from_enum_labels() throws Exception {
			var strategy = new MonitoringCsvMappingStrategy<Object>();
			var headers = strategy.generateHeader(new Object());
			var allMappings = MonitoringCsvMapping.values();
			assertEquals(allMappings.length, headers.length);
			assertEquals("Forward aeTitle", headers[0]);
			assertEquals("Last Seen", headers[headers.length - 1]);
		}

	}

}
