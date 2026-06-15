/*
 * Copyright (c) 2024-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.expression;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.Test;

class ExprConditionTest {

	/**
	 * Method under test:
	 *
	 * <ul>
	 * <li>{@link ExprCondition#ExprCondition(Attributes)} ()}
	 * <li>{@link ExprCondition#tagValueBeginsWith(int, String)} ()}
	 * <li>{@link ExprCondition#tagValueContains(int, String)}
	 * <li>{@link ExprCondition#tagValueEndsWith(int, String)}
	 * <li>{@link ExprCondition#tagValueIsPresent(String, String)}
	 * <li>{@link ExprCondition#tagIsPresent(int)}
	 * </ul>
	 */
	@Test
	void testExprConditionsWithMultipleStrings() {
		Attributes attributes = new Attributes();
		attributes.setString(Tag.ImageType, VR.CS, "ORIGINAL", "PRIMARY", "LABEL", "NONE");
		ExprCondition exprCondition = new ExprCondition(attributes);

		assertTrue(exprCondition.tagIsPresent(Tag.ImageType));
		assertTrue(exprCondition.tagValueIsPresent(Tag.ImageType, "ORIGINAL\\PRIMARY\\LABEL\\NONE"));

		assertTrue(exprCondition.tagValueContains(Tag.ImageType, "LABEL"));
		assertTrue(exprCondition.tagValueContains(Tag.ImageType, "NONE"));

		assertTrue(exprCondition.tagValueBeginsWith(Tag.ImageType, "ORIGINAL\\PRIMARY"));
		assertTrue(exprCondition.tagValueEndsWith(Tag.ImageType, "NONE"));
	}

}
