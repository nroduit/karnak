/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.component.converter;

import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class HStringToIntegerConverter extends StringToIntegerConverter {

	public HStringToIntegerConverter() {
		super(0, "Could not convert value to " + Integer.class.getName() + ".");
	}

	@Override
	public Result<Integer> convertToModel(String value, ValueContext context) {
		// Allow empty string or null to be converted to the default value (0)
		if (value == null || value.trim().isEmpty()) {
			return Result.ok(0);
		}
		return super.convertToModel(value, context);
	}

	@Override
	protected NumberFormat getFormat(Locale locale) {
		// Do not use a thousands separator, as HTML5 input type number expects a fixed
		// wire/DOM number format regardless of how the browser presents it to the user
		// (which could depend on the browser locale).
		DecimalFormat format = new DecimalFormat();
		format.setMaximumFractionDigits(0);
		format.setDecimalSeparatorAlwaysShown(false);
		format.setParseIntegerOnly(true);
		format.setGroupingUsed(false);
		return format;
	}

}
