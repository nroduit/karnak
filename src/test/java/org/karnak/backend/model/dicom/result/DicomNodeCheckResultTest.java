/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.dicom.result;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.model.dicom.ConfigNode;
import org.weasis.dicom.param.DicomNode;

@DisplayNameGeneration(ReplaceUnderscores.class)
class DicomNodeCheckResultTest {

	@Test
	void exposes_node_description_and_network_details_from_called_node() {
		ConfigNode node = new ConfigNode("Main PACS", new DicomNode("PACS_AET", "pacs.local", 11112));
		DicomEchoResult echo = DicomEchoResult.builder().build();
		NetworkCheckResult network = NetworkCheckResult.builder()
			.hostname("pacs.local")
			.port(11112)
			.hostnameReachable(true)
			.portOpen(true)
			.build();

		DicomNodeCheckResult result = new DicomNodeCheckResult("CALLER", node, echo, network);

		assertEquals("Main PACS", result.getCalledNodeDescription());
		assertEquals("PACS_AET pacs.local 11112", result.getCalledNodeNetworkDetails());
	}

}