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
import org.karnak.backend.data.entity.DicomNodeConfigEntity;

@DisplayNameGeneration(ReplaceUnderscores.class)
class DicomNodeCsvCodecTest {

	private static ByteArrayInputStream stream(String csv) {
		return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void parses_six_column_rows_with_type_and_group() {
		var csv = """
				description,aetitle,hostname,port,nodeType,nodeGroup
				Public,DICOMSERVER,dicomserver.co.uk,11112,PACS,SiteA
				""";

		var result = DicomNodeCsvCodec.parse(stream(csv), ',', null, false);

		assertTrue(result.errors().isEmpty());
		assertEquals(1, result.entities().size());
		var entity = result.entities().getFirst();
		assertEquals("PACS", entity.getNodeType());
		assertEquals("SiteA", entity.getNodeGroup());
	}

	@Test
	void maps_legacy_fifth_column_to_the_group() {
		var csv = "Public,DICOMSERVER,dicomserver.co.uk,11112,PACS_WEB\n";

		var entity = DicomNodeCsvCodec.parse(stream(csv), ',', null, false).entities().getFirst();

		assertEquals("WORKSTATION", entity.getNodeType());
		assertEquals("PACS_WEB", entity.getNodeGroup());
	}

	@Test
	void leaves_four_column_rows_ungrouped() {
		var csv = "Legacy,LEG,leg.host,104\n";

		var entity = DicomNodeCsvCodec.parse(stream(csv), ',', null, false).entities().getFirst();

		assertEquals("WORKSTATION", entity.getNodeType());
		assertNull(entity.getNodeGroup());
	}

	@Test
	void keeps_worklist_rows_as_the_reserved_type_without_a_group() {
		var csv = "WL,ADVT,dicomserver.co.uk,104,WORKLIST\n";

		var entity = DicomNodeCsvCodec.parse(stream(csv), ',', null, false).entities().getFirst();

		assertEquals("WORKLIST", entity.getNodeType());
		assertNull(entity.getNodeGroup());
	}

	@Test
	void forces_every_non_worklist_row_into_the_default_group() {
		var csv = """
				A,A_AE,a.host,104,WORKSTATION,Ignored
				B,B_AE,b.host,104,PACS,AlsoIgnored
				""";

		var result = DicomNodeCsvCodec.parse(stream(csv), ',', "Forced", true);

		assertEquals(2, result.entities().size());
		assertTrue(result.entities().stream().allMatch(entity -> "Forced".equals(entity.getNodeGroup())));
	}

	@Test
	void reports_rows_with_an_invalid_port() {
		var csv = "Bad,BAD,bad.host,notaport\n";

		var result = DicomNodeCsvCodec.parse(stream(csv), ',', null, false);

		assertTrue(result.entities().isEmpty());
		assertEquals(1, result.errors().size());
		assertTrue(result.errors().getFirst().contains("port"));
	}

	@Test
	void export_then_parse_round_trips() {
		var node = new DicomNodeConfigEntity("Public", "DICOMSERVER", "dicomserver.co.uk", 11112, "PACS", "SiteA");

		byte[] csv = DicomNodeCsvCodec.export(List.of(node));
		var parsed = DicomNodeCsvCodec.parse(new ByteArrayInputStream(csv), ',', null, false).entities().getFirst();

		assertEquals("DICOMSERVER", parsed.getAeTitle());
		assertEquals("PACS", parsed.getNodeType());
		assertEquals("SiteA", parsed.getNodeGroup());
	}

}
