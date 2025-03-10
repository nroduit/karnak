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

import java.time.DateTimeException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.model.profilepipe.HMAC;

@Slf4j
public class ShiftRangeDate {

	private ShiftRangeDate() {
	}

	public static void verifyShiftArguments(List<ArgumentEntity> argumentEntities) throws IllegalArgumentException {
		if (argumentEntities.stream().noneMatch(argument -> argument.getArgumentKey().equals("max_seconds"))
				|| argumentEntities.stream().noneMatch(argument -> argument.getArgumentKey().equals("max_days"))) {
			List<String> args = argumentEntities.stream().map(ArgumentEntity::getArgumentKey).toList();
			String text = "Cannot build the option ShiftRangeDate: Missing argument, the class minimum need [max_seconds, max_days] as parameters. Parameters given "
					+ args;

			IllegalArgumentException missingParameters = new IllegalArgumentException(text);
			log.error(text, missingParameters);
			throw missingParameters;
		}
	}

	public static String shift(Attributes dcm, int tag, List<ArgumentEntity> argumentEntities, HMAC hmac)
			throws DateTimeException {
		try {
			verifyShiftArguments(argumentEntities);
		}
		catch (IllegalArgumentException e) {
			throw e;
		}
		int shiftMaxDays = -1;
		int shiftMaxSeconds = -1;
		int shiftMinDays = 0;
		int shiftMinSeconds = 0;
		for (ArgumentEntity argumentEntity : argumentEntities) {
			final String key = argumentEntity.getArgumentKey();
			final String value = argumentEntity.getArgumentValue();

			try {
				if (key.equals("max_seconds")) {
					shiftMaxSeconds = Integer.parseInt(value);
				}
				if (key.equals("max_days")) {
					shiftMaxDays = Integer.parseInt(value);
				}
				if (key.equals("min_seconds")) {
					shiftMinSeconds = Integer.parseInt(value);
				}
				if (key.equals("min_days")) {
					shiftMinDays = Integer.parseInt(value);
				}
			}
			catch (Exception e) {
				log.error("args {} is not correct", value, e);
			}
		}
		String dcmElValue = dcm.getString(tag);
		String patientID = hmac.getHashContext().getPatientID();
		int shiftDays = (int) hmac.scaleHash(patientID, shiftMinDays, shiftMaxDays);
		int shiftSeconds = (int) hmac.scaleHash(patientID, shiftMinSeconds, shiftMaxSeconds);

		return ShiftDate.shiftValue(dcm, tag, dcmElValue, shiftDays, shiftSeconds);
	}

}
