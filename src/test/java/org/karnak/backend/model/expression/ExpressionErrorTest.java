/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ExpressionErrorTest {

	@Test
	void exposes_constructor_values() {
		ExpressionError error = new ExpressionError(true, "ok");

		assertTrue(error.isValid());
		assertEquals("ok", error.getMsg());
	}

	@Test
	void supports_mutation_of_both_fields() {
		ExpressionError error = new ExpressionError(true, "ok");

		error.setValid(false);
		error.setMsg("boom");

		assertFalse(error.isValid());
		assertEquals("boom", error.getMsg());
	}

}