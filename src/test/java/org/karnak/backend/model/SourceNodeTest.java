/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.weasis.dicom.param.DicomNode;

@DisplayNameGeneration(ReplaceUnderscores.class)
class SourceNodeTest {

	@Test
	void exposes_the_forward_ae_title_and_dicom_node() {
		DicomNode node = new DicomNode("AET", "host", 104);
		SourceNode source = new SourceNode("FWD", node);

		assertEquals("FWD", source.forwardAETitle());
		assertSame(node, source.sourceNode());
	}

	@Test
	void considers_nodes_equal_when_forward_ae_title_matches() {
		SourceNode a = new SourceNode("FWD", new DicomNode("AET", "host-a", 104));
		SourceNode b = new SourceNode("FWD", new DicomNode("OTHER", "host-b", 105));
		SourceNode c = new SourceNode("DIFFERENT", new DicomNode("AET", "host-a", 104));

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, c);
	}

}
