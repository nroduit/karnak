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

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.img.util.DateTimeUtils;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.ArgumentEntity;

@Slf4j
@NullUnmarked
public class DateFormat {

	// Date formats
	public static final String FORMAT_DDMMYYYY_SLASH = "dd/MM/yyyy";

	public static final String FORMAT_DDMMYYYY_SLASH_HHMMSS_2POINTS = "dd/MM/yyyy HH:mm:ss";

	public static final String FORMAT_DDMMYYYY_SLASH_HHMMSS_2POINTS_SSS_POINT = "dd/MM/yyyy HH:mm:ss.SSS";

	public static final String FORMAT_DDMMYYYY_SLASH_HHMMSS_2POINTS_SSSSSS_POINT = "dd/MM/yyyy HH:mm:ss.SSSSSS";

	private static final String DAY = "day";

	private static final String MONTH_DAY = "month_day";

	private DateFormat() {
	}

	/**
	 * Build DateTimeFormatter
	 * @param format Date format
	 * @return DateTimeFormatter
	 */
	public static DateTimeFormatter dateTimeFormatter(final String format) {
		return new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(format).toFormatter(Locale.ENGLISH);
	}

	/**
	 * Format a LocalDate to a specific format
	 * @param date Date to format
	 * @param format Format to apply
	 * @return Formatted date String
	 */
	public static String format(final LocalDate date, final String format) {
		return date == null ? null : date.format(dateTimeFormatter(format));
	}

	/**
	 * Format a LocalDateTime to a specific format
	 * @param dateTime Date to format
	 * @param format Format to apply
	 * @return Formatted date String
	 */
	public static String format(final LocalDateTime dateTime, final String format) {
		return dateTime == null ? null : dateTime.format(dateTimeFormatter(format));
	}

	public static String formatDA(String date, String option) {
		LocalDate localDate = DateTimeUtils.parseDA(date);
		localDate = switch (option) {
			case DAY -> localDate.withDayOfMonth(1);
			case MONTH_DAY -> localDate.withDayOfMonth(1).withMonth(1);
			default -> localDate;
		};
		return localDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
	}

	public static String formatDT(String dateTime, String option) {
		Temporal localDateTime = DateTimeUtils.parseDT(dateTime);
		localDateTime = switch (option) {
			case DAY -> localDateTime.with(ChronoField.DAY_OF_MONTH, 1);
			case MONTH_DAY -> localDateTime.with(ChronoField.DAY_OF_MONTH, 1).with(ChronoField.MONTH_OF_YEAR, 1);
			default -> localDateTime;
		};
		return DateTimeUtils.formatDT(localDateTime);
	}

	public static String format(Attributes dcm, int tag, List<ArgumentEntity> argumentEntities)
			throws DateTimeException {
		verifyPatternArguments(argumentEntities);

		String dcmElValue = dcm.getString(tag);
		if (dcmElValue == null) {
			return null;
		}
		String format = ArgumentUtil.stringValue(argumentEntities, "remove", "");
		return switch (dcm.getVR(tag)) {
			case DA -> formatDA(dcmElValue, format);
			case DT -> formatDT(dcmElValue, format);
			default -> null;
		};
	}

	public static void verifyPatternArguments(List<ArgumentEntity> argumentEntities) {
		List<String> allowedValues = List.of(DAY, MONTH_DAY);
		if (argumentEntities.stream()
			.noneMatch(argument -> argument.getArgumentKey().equals("remove")
					&& allowedValues.contains(argument.getArgumentValue()))) {
			IllegalArgumentException missingParameters = new IllegalArgumentException(
					"Cannot build the option date_format, arguments are not correct");
			log.error("Missing argument, the class need pattern as parameters", missingParameters);
			throw missingParameters;
		}
	}

}
