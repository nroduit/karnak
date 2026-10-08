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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.weasis.dicom.param.DicomNode;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ConfigNodeTest {

	private static final DicomNode NODE = new DicomNode("AET", "host", 104);

	@Test
	void rejects_null_constructor_arguments() {
		assertThrows(NullPointerException.class, () -> new ConfigNode(null, NODE));
		assertThrows(NullPointerException.class, () -> new ConfigNode("name", null));
	}

	@Test
	void delegates_node_properties_to_the_called_node() {
		ConfigNode config = new ConfigNode("name", NODE);

		assertEquals("AET", config.getAet());
		assertEquals("host", config.getHostname());
		assertEquals(104, config.getPort());
	}

	@Test
	void uses_the_name_as_its_string_representation() {
		assertEquals("name", new ConfigNode("name", NODE).toString());
	}

	@Test
	void updates_the_name_only_when_new_value_has_text() {
		ConfigNode config = new ConfigNode("name", NODE);

		config.setName("renamed");
		assertEquals("renamed", config.getName());

		config.setName("  ");
		assertEquals("renamed", config.getName());

		config.setName(null);
		assertEquals("renamed", config.getName());
	}

	@Test
	void updates_the_called_node_only_when_non_null() {
		ConfigNode config = new ConfigNode("name", NODE);
		DicomNode other = new DicomNode("OTHER", "host2", 105);

		config.setCalledNode(other);
		assertSame(other, config.getCalledNode());

		config.setCalledNode(null);
		assertSame(other, config.getCalledNode());
	}

}