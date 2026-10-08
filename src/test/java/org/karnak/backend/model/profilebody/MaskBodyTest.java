/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profilebody;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class MaskBodyTest {

	@Test
	void stores_and_exposes_all_fields() {
		MaskBody mask = new MaskBody();
		mask.setStationName("CT-1");
		mask.setImageWidth(512L);
		mask.setImageHeight(384L);
		mask.setColor("FF0000");
		mask.setRectangles(List.of("0,0,10,10"));

		assertEquals("CT-1", mask.getStationName());
		assertEquals(512L, mask.getImageWidth());
		assertEquals(384L, mask.getImageHeight());
		assertEquals("FF0000", mask.getColor());
		assertEquals(List.of("0,0,10,10"), mask.getRectangles());
	}

}