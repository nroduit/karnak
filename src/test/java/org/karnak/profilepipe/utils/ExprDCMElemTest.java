/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.profilepipe.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.karnak.backend.model.expression.ExprAction;

class ExprDCMElemTest {

	@Test
	void isHexTag() {
		assertEquals(true, ExprAction.isHexTag("(0221,0000)"));
		assertEquals(true, ExprAction.isHexTag("0xx1,0000)"));
		assertEquals(true, ExprAction.isHexTag("0xx1,0000"));
		assertEquals(true, ExprAction.isHexTag("(0xx1,0000"));

		assertEquals(true, ExprAction.isHexTag("(02210000)"));
		assertEquals(true, ExprAction.isHexTag("0xx10000)"));
		assertEquals(true, ExprAction.isHexTag("0xx10000"));
		assertEquals(true, ExprAction.isHexTag("(0xx10000"));

		assertEquals(true, ExprAction.isHexTag("02210000)"));
		assertEquals(true, ExprAction.isHexTag("0x010000)"));
		assertEquals(true, ExprAction.isHexTag("00010000"));
		assertEquals(true, ExprAction.isHexTag("(00010000"));

		assertEquals(true, ExprAction.isHexTag("0201,0000"));

		assertEquals(false, ExprAction.isHexTag("ef00)"));
		assertEquals(false, ExprAction.isHexTag("(e,0000)"));
		assertEquals(false, ExprAction.isHexTag("xx1,0000)"));
		assertEquals(false, ExprAction.isHexTag("xx1,00 00)"));
	}

}
