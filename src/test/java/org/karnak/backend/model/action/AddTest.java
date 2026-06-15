/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.action;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.Test;

class AddTest {

	@Test
	void should_add_tag() {
		// Init data
		Attributes attributes = new Attributes();
		Add add = new Add("symbol", 524294, VR.AE, "dummyValue");

		// Add tag
		add.execute(attributes, 524291, null);

		// Test result
		assertEquals("dummyValue", attributes.getString(524294));
	}

}
