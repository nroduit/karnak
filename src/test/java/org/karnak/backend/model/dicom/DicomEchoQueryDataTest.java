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

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class DicomEchoQueryDataTest {

	@Test
	void initialises_the_calling_aet_with_the_default_value() {
		DicomEchoQueryData data = new DicomEchoQueryData();

		assertEquals("DCM-TOOLS", data.getCallingAet());
	}

	@Test
	void stores_called_node_details() {
		DicomEchoQueryData data = new DicomEchoQueryData();
		data.setCalledAet("CALLED");
		data.setCalledHostname("host");
		data.setCalledPort(11112);
		data.setCallingAet("CALLER");

		assertEquals("CALLED", data.getCalledAet());
		assertEquals("host", data.getCalledHostname());
		assertEquals(11112, data.getCalledPort());
		assertEquals("CALLER", data.getCallingAet());
	}

	@Test
	void reset_restores_the_default_calling_aet() {
		DicomEchoQueryData data = new DicomEchoQueryData();
		data.setCallingAet("CALLER");

		data.reset();

		assertEquals("DCM-TOOLS", data.getCallingAet());
	}

}