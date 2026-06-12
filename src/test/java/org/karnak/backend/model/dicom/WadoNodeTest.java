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
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class WadoNodeTest {

	private static URL url() {
		try {
			return URI.create("http://localhost:8080/wado").toURL();
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Test
	void exposes_name_and_url_and_a_mutable_tag_list() {
		URL url = url();
		WadoNode node = new WadoNode("wado", url);
		node.getTags().add("0010,0010");

		assertEquals("wado", node.getName());
		assertEquals(url, node.getUrl());
		assertEquals(1, node.getTags().size());
		assertEquals("wado", node.toString());
	}

	@Test
	void rejects_null_constructor_arguments() {
		assertThrows(NullPointerException.class, () -> new WadoNode(null, url()));
		assertThrows(NullPointerException.class, () -> new WadoNode("wado", null));
	}

	@Test
	void starts_with_an_empty_tag_list() {
		assertTrue(new WadoNode("wado", url()).getTags().isEmpty());
	}

}