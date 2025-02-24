/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.util;

import java.util.List;
import org.dcm4che3.data.Attributes;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.model.expression.ExprCondition;
import org.karnak.backend.model.profilepipe.HMAC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShiftByTagDate {

	private static final Logger LOGGER = LoggerFactory.getLogger(ShiftByTagDate.class);

	private ShiftByTagDate() {
	}

	public static void verifyShiftArguments(List<ArgumentEntity> argumentEntities) {
		// All arguments are optional
	}

	public static String shift(Attributes dcm, int tag, List<ArgumentEntity> argumentEntities, HMAC hmac) {
		try {
			verifyShiftArguments(argumentEntities);
		}
		catch (IllegalArgumentException e) {
			throw e;
		}

		String dcmElValue = dcm.getString(tag);
		String shiftDaysTag = "";
		String shiftSecondsTag = "";

		for (ArgumentEntity argumentEntity : argumentEntities) {
			final String key = argumentEntity.getArgumentKey();
			final String value = argumentEntity.getArgumentValue();

			try {
				if (key.equals("days_tag")) {
					shiftDaysTag = value;
				}
				if (key.equals("seconds_tag")) {
					shiftSecondsTag = value;
				}
			}
			catch (Exception e) {
				LOGGER.error("args {} is not correct", value, e);
			}
		}

		final String shiftDaysValue = dcm.getString(ExprCondition.intFromHexString(shiftDaysTag));
		final String shiftSecondsValue = dcm.getString(ExprCondition.intFromHexString(shiftSecondsTag));

		int shiftDays = 0;
		int shiftSeconds = 0;
		try {
			if (shiftDaysValue != null) {
				shiftDays = Integer.parseInt(shiftDaysValue);
			}
		}
		catch (Exception e) {
			LOGGER.error("args {} is not correct", shiftDaysValue, e);
		}
		try {
			if (shiftSecondsValue != null) {
				shiftSeconds = Integer.parseInt(shiftSecondsValue);
			}
		}
		catch (Exception e) {
			LOGGER.error("args {} is not correct", shiftSecondsValue, e);
		}

		return ShiftDate.shiftValue(dcm, tag, dcmElValue, shiftDays, shiftSeconds);
	}

}
