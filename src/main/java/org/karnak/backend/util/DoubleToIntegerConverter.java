/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.util;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

public class DoubleToIntegerConverter implements Converter<Double, Integer> {

	private static final long serialVersionUID = 1L;

	@Override
	public Result<Integer> convertToModel(Double value, ValueContext valueContext) {
		if (value != null) {
			return Result.ok(value.intValue());
		}
		else {
			return Result.ok(null);
		}
	}

	@Override
	public Double convertToPresentation(Integer value, ValueContext valueContext) {
		if (value == null) {
			return null;
		}
		else {
			return value.doubleValue();
		}
	}

}
