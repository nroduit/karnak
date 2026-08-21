/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.data.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

import java.util.UUID;

@DisplayNameGeneration(ReplaceUnderscores.class)
class DicomSourceNodeEntityTest {

	@Test
	void default_and_factory_start_with_empty_values() {
		DicomSourceNodeEntity entity = DicomSourceNodeEntity.ofEmpty();

		assertEquals("", entity.getDescription());
		assertEquals("", entity.getAeTitle());
		assertEquals("", entity.getHostname());
		assertFalse(entity.getCheckHostname());
	}

	@Test
	void exposes_its_properties_through_setters() {
		DicomSourceNodeEntity entity = new DicomSourceNodeEntity();
		ForwardNodeEntity forwardNode = new ForwardNodeEntity();
		entity.setId(1L);
		entity.setDescription("desc");
		entity.setAeTitle("AET");
		entity.setHostname("host");
		entity.setCheckHostname(Boolean.TRUE);
		entity.setForwardNodeEntity(forwardNode);

		assertEquals(1L, entity.getId());
		assertEquals("desc", entity.getDescription());
		assertEquals("AET", entity.getAeTitle());
		assertEquals("host", entity.getHostname());
		assertTrue(entity.getCheckHostname());
		assertSame(forwardNode, entity.getForwardNodeEntity());
	}

	@Test
	void matches_filter_against_description_aetitle_and_hostname() {
		DicomSourceNodeEntity entity = new DicomSourceNodeEntity();
		entity.setDescription("scanner");
		entity.setAeTitle("PACS");
		entity.setHostname("10.0.0.1");

		assertTrue(entity.matchesFilter("scan"));
		assertTrue(entity.matchesFilter("PAC"));
		assertTrue(entity.matchesFilter("10.0"));
		assertFalse(entity.matchesFilter("absent"));
	}

	@Test
	void to_string_contains_the_aetitle() {
		DicomSourceNodeEntity entity = new DicomSourceNodeEntity();
		entity.setAeTitle("PACS");

		assertTrue(entity.toString().contains("PACS"));
	}

	@Test
	void equal_instances_match_and_share_a_hash() {
		DicomSourceNodeEntity a = node(1L, "AET", "1.2.3.4");
		DicomSourceNodeEntity b = node(1L, "AET", "1.2.3.4");

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(a, a);
	}

	@Test
	void differs_by_field_type_or_null() {
		DicomSourceNodeEntity base = node(1L, "AET", "1.2.3.4");

		assertNotEquals(base, node(2L, "AET", "1.2.3.5"));
		assertNotEquals(base, node(1L, "OTHER", "1.2.3.4"));
		assertFalse(base.equals(null));
		assertNotEquals(base, "not-a-node");
	}

	private static DicomSourceNodeEntity node(Long id, String aeTitle, String hostName) {
		DicomSourceNodeEntity entity = new DicomSourceNodeEntity();
		entity.setId(id);
		entity.setAeTitle(aeTitle);
		entity.setHostname(hostName);
		return entity;
	}

}