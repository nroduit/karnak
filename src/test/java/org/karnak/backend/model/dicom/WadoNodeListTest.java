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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URL;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class WadoNodeListTest {

	private static WadoNode node(String name) {
		try {
			return new WadoNode(name, URI.create("http://localhost/" + name).toURL());
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Test
	void empty_list_keeps_its_name() {
		WadoNodeList list = new WadoNodeList("wados");

		assertTrue(list.isEmpty());
		assertEquals("wados", list.toString());
	}

	@Test
	void rejects_a_null_name_on_the_single_argument_constructor() {
		assertThrows(NullPointerException.class, () -> new WadoNodeList((String) null));
	}

	@Test
	void list_built_from_a_collection_retains_its_elements() {
		WadoNodeList list = new WadoNodeList("wados", List.of(node("a"), node("b")));

		assertEquals(2, list.size());
		assertEquals("wados", list.toString());
	}

	@Test
	void list_built_with_an_initial_capacity_is_usable() {
		WadoNodeList list = new WadoNodeList("wados", 8);
		list.add(node("a"));

		assertEquals(1, list.size());
		assertEquals("wados", list.toString());
	}

}