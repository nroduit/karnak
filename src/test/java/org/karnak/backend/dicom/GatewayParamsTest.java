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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.weasis.dicom.param.AdvancedParams;

@DisplayNameGeneration(ReplaceUnderscores.class)
class GatewayParamsTest {

	private static ForwardDicomNode forwardNode(String aet, String... acceptedAaets) {
		ForwardDicomNode node = new ForwardDicomNode(aet);
		for (String accepted : acceptedAaets) {
			node.addAcceptedSourceNode(accepted);
		}
		return node;
	}

	@Test
	void can_be_built_with_the_simple_constructor() {
		assertNotNull(new GatewayParams(true));
	}

	@Test
	void can_be_built_with_advanced_params() {
		assertNotNull(new GatewayParams(new AdvancedParams(), false));
	}

	@Test
	void accepted_calling_aetitles_collects_the_distinct_source_aets() {
		Map<ForwardDicomNode, List<ForwardDestination>> destinations = Map.of(forwardNode("FWD-1", "A", "B"), List.of(),
				forwardNode("FWD-2", "B", "C"), List.of());

		String[] aets = GatewayParams.getAcceptedCallingAETitles(destinations);

		assertNotNull(aets);
		assertEquals(Set.of("A", "B", "C"), Set.of(aets));
	}

	@Test
	void accepted_calling_aetitles_is_null_when_any_node_accepts_every_source() {
		// A node with no accepted source nodes means "accept any source", so the listener
		// cannot be restricted to a fixed list.
		Map<ForwardDicomNode, List<ForwardDestination>> destinations = Map.of(forwardNode("FWD-1", "A"), List.of(),
				forwardNode("FWD-2"), List.of());

		assertNull(GatewayParams.getAcceptedCallingAETitles(destinations));
	}

	@Test
	void accepted_calling_aetitles_deduplicates_a_repeated_source() {
		Map<ForwardDicomNode, List<ForwardDestination>> destinations = Map.of(forwardNode("FWD-1", "A", "A"),
				List.of());

		String[] aets = GatewayParams.getAcceptedCallingAETitles(destinations);

		assertNotNull(aets);
		assertEquals(1, aets.length);
		assertTrue("A".equals(aets[0]));
	}

}