/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class DoubleToIntegerConverterTest {

	private final DoubleToIntegerConverter converter = new DoubleToIntegerConverter();

	private final ValueContext context = new ValueContext();

	@Test
	void converts_a_double_to_its_integer_part() {
		Result<Integer> result = converter.convertToModel(3.9, context);

		assertEquals(3, result.getOrThrow(IllegalStateException::new));
	}

	@Test
	void converts_a_null_double_to_a_null_integer() {
		Result<Integer> result = converter.convertToModel(null, context);

		assertNull(result.getOrThrow(IllegalStateException::new));
	}

	@Test
	void converts_an_integer_to_a_double_for_presentation() {
		assertEquals(5.0, converter.convertToPresentation(5, context));
	}

	@Test
	void converts_a_null_integer_to_a_null_double_for_presentation() {
		assertNull(converter.convertToPresentation(null, context));
	}

}