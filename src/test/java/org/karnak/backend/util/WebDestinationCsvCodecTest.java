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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.WebDestinationConfigEntity;

@DisplayNameGeneration(ReplaceUnderscores.class)
class WebDestinationCsvCodecTest {

	private static ByteArrayInputStream stream(String csv) {
		return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void parses_a_semicolon_separated_service_cell() {
		var csv = """
				description,url,services,group
				Cloud,https://stow.example/dicom-web,STOW-RS;QIDO-RS,SiteA
				""";

		var result = WebDestinationCsvCodec.parse(stream(csv), ',');

		assertTrue(result.errors().isEmpty());
		var entity = result.entities().getFirst();
		assertEquals("https://stow.example/dicom-web", entity.getUrl());
		assertEquals("SiteA", entity.getGroupName());
		assertTrue(entity.getServices().contains("STOW_RS"));
		assertTrue(entity.getServices().contains("QIDO_RS"));
	}

	@Test
	void treats_an_empty_service_cell_as_all_services() {
		var csv = "Cloud,https://stow.example/dicom-web,,\n";

		var entity = WebDestinationCsvCodec.parse(stream(csv), ',').entities().getFirst();

		assertEquals("", entity.getServices());
		assertNull(entity.getGroupName());
	}

	@Test
	void keeps_the_row_but_reports_an_unknown_service() {
		var csv = "Cloud,https://stow.example/dicom-web,STOW-RS;BOGUS,\n";

		var result = WebDestinationCsvCodec.parse(stream(csv), ',');

		assertEquals(1, result.entities().size());
		assertEquals(1, result.errors().size());
		assertTrue(result.errors().getFirst().contains("BOGUS"));
	}

	@Test
	void reports_an_invalid_url() {
		var csv = "Bad,not-a-url,,\n";

		var result = WebDestinationCsvCodec.parse(stream(csv), ',');

		assertTrue(result.entities().isEmpty());
		assertEquals(1, result.errors().size());
		assertTrue(result.errors().getFirst().contains("URL"));
	}

	@Test
	void export_uses_a_semicolon_delimited_services_cell() {
		var entity = new WebDestinationConfigEntity("Cloud", "https://stow.example/dicom-web", "STOW_RS,QIDO_RS",
				"SiteA");

		var csv = new String(WebDestinationCsvCodec.export(List.of(entity)), StandardCharsets.UTF_8);

		assertTrue(csv.contains("\"description\",\"url\",\"services\",\"group\""));
		assertTrue(csv.contains("STOW_RS;QIDO_RS"));
	}

}
