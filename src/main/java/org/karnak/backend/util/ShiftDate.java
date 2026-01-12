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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che3.data.Attributes;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.dicom.DateTimeUtils;

@Slf4j
public class ShiftDate {

	private ShiftDate() {
	}

	public static String dateByDays(String date, int shiftDays) {
		LocalDate localDate = DateTimeUtils.parseDA(date);
		LocalDate dummyLocalDate = localDate.minusDays(shiftDays);
		return DateTimeUtils.formatDA(dummyLocalDate);
	}

	public static String timeBySeconds(String time, int shiftSeconds) {
		LocalTime localTime = DateTimeUtils.parseTM(time);
		LocalTime dummyLocalTime = localTime.minusSeconds(shiftSeconds);
		return DateTimeUtils.formatTM(dummyLocalTime);
	}

	public static String datetimeByDays(Date dateTime, int shiftDays, int shiftSeconds) {
		LocalDateTime localDateTime = LocalDateTime.ofInstant(dateTime.toInstant(), ZoneId.systemDefault());
		LocalDateTime dummyLocalDateTime = localDateTime.minusDays(shiftDays);
		dummyLocalDateTime = dummyLocalDateTime.minusSeconds(shiftSeconds);
		return DateTimeUtils.formatDT(dummyLocalDateTime);
	}

	private static String addMissingZero(String age, int nMissingValue) {
		int n = nMissingValue - age.length();
		return StringUtils.repeat('0', n) + age;
	}

	public static String ageByDays(String age, int shiftDays) {
		String valueAge = age.substring(0, 3);
		int intAge = Integer.parseInt(valueAge);

		int maxSubstring = age.length();
		String formatAge = age.substring(3, maxSubstring);

		int intDummyAge = switch (formatAge) {
			case "Y" -> intAge + shiftDays / 365;
			case "M" -> intAge + shiftDays / 30;
			case "W" -> intAge + shiftDays / 7;
			default -> intAge + shiftDays;
		};

		return addMissingZero(String.valueOf(intDummyAge), 3) + formatAge;
	}

	public static String shift(Attributes dcm, int tag, List<ArgumentEntity> argumentEntities)
			throws DateTimeException {
		verifyShiftArguments(argumentEntities);

		String dcmElValue = dcm.getString(tag);
		int shiftDays = -1;
		int shiftSeconds = -1;

		for (ArgumentEntity argumentEntity : argumentEntities) {
			final String key = argumentEntity.getArgumentKey();
			final String value = argumentEntity.getArgumentValue();

			try {
				if (key.equals("seconds")) {
					shiftSeconds = Integer.parseInt(value);
				}
				if (key.equals("days")) {
					shiftDays = Integer.parseInt(value);
				}
			}
			catch (Exception e) {
				log.error("args {} is not correct", value, e);
			}
		}
		return shiftValue(dcm, tag, dcmElValue, shiftDays, shiftSeconds);
	}

	public static String shiftValue(Attributes dcm, int tag, String dcmElValue, int shiftDays, int shiftSeconds) {
		if (dcmElValue != null) {
			return switch (dcm.getVR(tag)) {
				case AS -> ShiftDate.ageByDays(dcmElValue, shiftDays);
				case DA -> ShiftDate.dateByDays(dcmElValue, shiftDays);
				case DT -> ShiftDate.datetimeByDays(dcm.getDate(tag), shiftDays, shiftSeconds);
				case TM -> ShiftDate.timeBySeconds(dcmElValue, shiftSeconds);
				default -> null;
			};
		}
		else {
			return null;
		}
	}

	public static void verifyShiftArguments(List<ArgumentEntity> argumentEntities) throws IllegalArgumentException {
		if (argumentEntities.stream().noneMatch(argument -> argument.getArgumentKey().equals("seconds"))
				|| argumentEntities.stream().noneMatch(argument -> argument.getArgumentKey().equals("days"))) {
			List<String> args = argumentEntities.stream().map(ArgumentEntity::getArgumentKey).toList();
			IllegalArgumentException missingParameters = new IllegalArgumentException(
					"Cannot build the option ShiftDate: Missing argument, the class need [seconds, days] as parameters. Parameters given "
							+ args);
			log.error("Missing argument, the class need seconds and days as parameters", missingParameters);
			throw missingParameters;
		}
	}

}
