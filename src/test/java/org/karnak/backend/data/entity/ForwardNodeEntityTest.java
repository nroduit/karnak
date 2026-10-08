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

@DisplayNameGeneration(ReplaceUnderscores.class)
class ForwardNodeEntityTest {

	@Test
	void default_and_factory_start_empty() {
		ForwardNodeEntity entity = ForwardNodeEntity.ofEmpty();

		assertEquals("", entity.getFwdAeTitle());
		assertEquals("", entity.getFwdDescription());
		assertTrue(entity.getSourceNodes().isEmpty());
		assertTrue(entity.getDestinationEntities().isEmpty());
	}

	@Test
	void aetitle_constructor_sets_the_title() {
		assertEquals("FWD", new ForwardNodeEntity("FWD").getFwdAeTitle());
	}

	@Test
	void exposes_its_properties_through_setters() {
		ForwardNodeEntity entity = new ForwardNodeEntity();
		entity.setId(1L);
		entity.setFwdAeTitle("FWD");
		entity.setFwdDescription("desc");

		assertEquals(1L, entity.getId());
		assertEquals("FWD", entity.getFwdAeTitle());
		assertEquals("desc", entity.getFwdDescription());
	}

	@Test
	void add_and_remove_source_node_maintains_the_back_reference() {
		ForwardNodeEntity entity = new ForwardNodeEntity("FWD");
		DicomSourceNodeEntity source = new DicomSourceNodeEntity();

		entity.addSourceNode(source);

		assertEquals(1, entity.getSourceNodes().size());
		assertSame(entity, source.getForwardNodeEntity());

		entity.removeSourceNode(source);
		assertTrue(entity.getSourceNodes().isEmpty());
	}

	@Test
	void add_and_remove_destination_maintains_the_back_reference() {
		ForwardNodeEntity entity = new ForwardNodeEntity("FWD");
		DestinationEntity destination = new DestinationEntity();

		entity.addDestination(destination);

		assertEquals(1, entity.getDestinationEntities().size());
		assertSame(entity, destination.getForwardNodeEntity());

		entity.removeDestination(destination);
		assertTrue(entity.getDestinationEntities().isEmpty());
	}

	@Test
	void matches_filter_on_own_fields_and_child_nodes() {
		ForwardNodeEntity entity = new ForwardNodeEntity("PACS");
		entity.setFwdDescription("main gateway");

		DicomSourceNodeEntity source = new DicomSourceNodeEntity();
		source.setAeTitle("SCANNER");
		entity.addSourceNode(source);

		assertTrue(entity.matchesFilter("PACS"));
		assertTrue(entity.matchesFilter("gateway"));
		assertTrue(entity.matchesFilter("SCANNER"));
		assertFalse(entity.matchesFilter("absent"));
	}

	@Test
	void to_string_contains_the_aetitle() {
		assertTrue(new ForwardNodeEntity("FWD").toString().contains("FWD"));
	}

	@Test
	void equality_is_based_on_the_id() {
		ForwardNodeEntity a = withId(1L);
		ForwardNodeEntity b = withId(1L);

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(a, a);
		assertNotEquals(a, withId(2L));
		assertFalse(a.equals(null));
		assertNotEquals(a, "not-a-node");
	}

	@Test
	void nodes_without_an_id_are_equal_only_to_other_id_less_nodes() {
		ForwardNodeEntity a = new ForwardNodeEntity("A");
		ForwardNodeEntity b = new ForwardNodeEntity("B");

		// equals() relies on the id only, which is null for both.
		assertEquals(a, b);
		assertNotEquals(a, withId(1L));
	}

	private static ForwardNodeEntity withId(Long id) {
		ForwardNodeEntity entity = new ForwardNodeEntity("FWD");
		entity.setId(id);
		return entity;
	}

}