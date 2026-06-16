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

class ReplaceTest {

	@Test
	void should_replace_tag_not_null() {
		// Init data
		Attributes attributes = new Attributes();
		attributes.setString(524294, VR.AE, "initialValue");
		Replace replace = new Replace("symbol", "dummyValue");

		// Add tag
		replace.execute(attributes, 524294, null);

		// Test result
		assertEquals("dummyValue", attributes.getString(524294));
	}

	@Test
	void should_replace_tag_null() {
		// Init data
		Attributes attributes = new Attributes();
		attributes.setString(524294, VR.AE, "initialValue");
		Replace replace = new Replace("symbol", null);

		// Add tag
		replace.execute(attributes, 524294, null);

		// Test result
		assertEquals(null, attributes.getString(524294));
	}

}
