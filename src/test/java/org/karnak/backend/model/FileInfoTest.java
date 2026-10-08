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

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class FileInfoTest {

	@Test
	void exposes_the_values_passed_to_the_constructor() {
		FileInfo info = new FileInfo("file.dcm", "1.2.3", "1.2.840", "1.2.840.10008.1.2.1");

		assertEquals("file.dcm", info.filename());
		assertEquals("1.2.3", info.iuid());
		assertEquals("1.2.840", info.cuid());
		assertEquals("1.2.840.10008.1.2.1", info.tsuid());
	}

}
