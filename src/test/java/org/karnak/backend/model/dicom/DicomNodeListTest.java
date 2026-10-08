/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.dicom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.weasis.dicom.param.DicomNode;

@DisplayNameGeneration(ReplaceUnderscores.class)
class DicomNodeListTest {

	private static ConfigNode node(String name) {
		return new ConfigNode(name, new DicomNode(name, "host", 104));
	}

	@Test
	void empty_list_keeps_its_name() {
		DicomNodeList list = new DicomNodeList("nodes");

		assertEquals("nodes", list.getName());
		assertTrue(list.isEmpty());
		assertEquals("nodes", list.toString());
	}

	@Test
	void list_built_from_a_collection_retains_its_elements() {
		DicomNodeList list = new DicomNodeList("nodes", List.of(node("a"), node("b")));

		assertEquals(2, list.size());
		assertEquals("nodes", list.getName());
	}

	@Test
	void list_built_with_an_initial_capacity_is_usable() {
		DicomNodeList list = new DicomNodeList("nodes", 4);
		list.add(node("a"));

		assertEquals(1, list.size());
		assertEquals("nodes", list.getName());
	}

}