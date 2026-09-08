/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.dicom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.weasis.dicom.param.DicomNode;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ForwardDicomNodeTest {

	@Nested
	class Construction {

		@Test
		void from_an_aet_only_leaves_hostname_and_id_null() {
			ForwardDicomNode node = new ForwardDicomNode("FWD-AET");

			assertEquals("FWD-AET", node.getForwardAETitle());
			assertNull(node.getHostname());
			assertNull(node.getId());
			assertTrue(node.getAcceptedSourceNodes().isEmpty());
		}

		@Test
		void from_an_aet_and_hostname_keeps_both() {
			ForwardDicomNode node = new ForwardDicomNode("FWD-AET", "host");

			assertEquals("FWD-AET", node.getForwardAETitle());
			assertEquals("host", node.getHostname());
		}

		@Test
		void from_an_aet_hostname_and_id_keeps_the_id() {
			ForwardDicomNode node = new ForwardDicomNode("FWD-AET", "host", 7L);

			assertEquals(7L, node.getId());
		}

		@Test
		void from_a_dicom_node_copies_aet_and_hostname() {
			ForwardDicomNode node = new ForwardDicomNode(new DicomNode("FWD-AET", "host", 104));

			assertEquals("FWD-AET", node.getForwardAETitle());
			assertEquals("host", node.getHostname());
		}

	}

	@Nested
	class AcceptedSourceNodes {

		@Test
		void adds_a_source_node_by_aet() {
			ForwardDicomNode node = new ForwardDicomNode("FWD-AET");

			node.addAcceptedSourceNode("SRC-AET");

			assertEquals(1, node.getAcceptedSourceNodes().size());
			assertTrue(node.getAcceptedSourceNodes().stream().anyMatch(n -> "SRC-AET".equals(n.getAet())));
		}

		@Test
		void adds_a_source_node_by_aet_and_hostname() {
			ForwardDicomNode node = new ForwardDicomNode("FWD-AET");

			node.addAcceptedSourceNode("SRC-AET", "src-host");

			DicomNode added = node.getAcceptedSourceNodes().iterator().next();
			assertEquals("SRC-AET", added.getAet());
			assertEquals("src-host", added.getHostname());
		}

		@Test
		void adds_a_source_node_with_an_id() {
			ForwardDicomNode node = new ForwardDicomNode("FWD-AET");

			node.addAcceptedSourceNode(3L, "SRC-AET", "src-host");

			assertEquals(1, node.getAcceptedSourceNodes().size());
		}

		@Test
		void hostname_validation_follows_the_explicit_flag() {
			ForwardDicomNode node = new ForwardDicomNode("FWD-AET");

			node.addAcceptedSourceNode(3L, "SRC-AET", "src-host", false);
			node.addAcceptedSourceNode(4L, "SRC-AET2", "src-host2", true);

			DicomNode unchecked = node.getAcceptedSourceNodes()
				.stream()
				.filter(n -> "SRC-AET".equals(n.getAet()))
				.findFirst()
				.orElseThrow();
			DicomNode checked = node.getAcceptedSourceNodes()
				.stream()
				.filter(n -> "SRC-AET2".equals(n.getAet()))
				.findFirst()
				.orElseThrow();
			assertFalse(unchecked.isValidateHostname());
			assertTrue(checked.isValidateHostname());
		}

	}

	@Nested
	class Identity {

		@Test
		void to_string_is_the_forward_aet() {
			assertEquals("FWD-AET", new ForwardDicomNode("FWD-AET").toString());
		}

		@Test
		void nodes_with_the_same_aet_are_equal_regardless_of_hostname() {
			ForwardDicomNode a = new ForwardDicomNode("FWD-AET", "host-a");
			ForwardDicomNode b = new ForwardDicomNode("FWD-AET", "host-b");

			// equals() compares only the forward AE title.
			assertEquals(a, b);
		}

		@Test
		void identical_nodes_share_a_hash_code() {
			ForwardDicomNode a = new ForwardDicomNode("FWD-AET", "host");
			ForwardDicomNode b = new ForwardDicomNode("FWD-AET", "host");

			assertEquals(a.hashCode(), b.hashCode());
		}

		@Test
		void nodes_with_different_aets_are_not_equal() {
			assertNotEquals(new ForwardDicomNode("FWD-1"), new ForwardDicomNode("FWD-2"));
		}

		@Test
		void a_node_equals_itself() {
			ForwardDicomNode node = new ForwardDicomNode("FWD-AET");

			assertEquals(node, node);
		}

		@Test
		void a_node_is_not_equal_to_null_or_a_plain_dicom_node() {
			ForwardDicomNode node = new ForwardDicomNode("FWD-AET");

			assertNotEquals(null, node);
			assertNotEquals(node, new DicomNode("FWD-AET", "host", 104));
		}

	}

}