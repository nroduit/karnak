/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.dicom;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.OFFSET_SECONDS;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Apr 2019
 */
@Slf4j
public class DateTimeUtils {

	private DateTimeUtils() {
	}

	private static final DateTimeFormatter DA_PARSER = new DateTimeFormatterBuilder().appendValue(YEAR, 4)
		.optionalStart()
		.appendLiteral('.')
		.optionalEnd()
		.appendValue(MONTH_OF_YEAR, 2)
		.optionalStart()
		.appendLiteral('.')
		.optionalEnd()
		.appendValue(DAY_OF_MONTH, 2)
		.toFormatter();

	private static final DateTimeFormatter DA_FORMATTER = new DateTimeFormatterBuilder().appendValue(YEAR, 4)
		.appendValue(MONTH_OF_YEAR, 2)
		.appendValue(DAY_OF_MONTH, 2)
		.toFormatter();

	private static final DateTimeFormatter TM_PARSER = new DateTimeFormatterBuilder().appendValue(HOUR_OF_DAY, 2)
		.optionalStart()
		.optionalStart()
		.appendLiteral(':')
		.optionalEnd()
		.appendValue(MINUTE_OF_HOUR, 2)
		.optionalStart()
		.optionalStart()
		.appendLiteral(':')
		.optionalEnd()
		.appendValue(SECOND_OF_MINUTE, 2)
		.optionalStart()
		.appendFraction(NANO_OF_SECOND, 0, 6, true)
		.toFormatter();

	private static final DateTimeFormatter TM_FORMATTER = new DateTimeFormatterBuilder().appendValue(HOUR_OF_DAY, 2)
		.appendValue(MINUTE_OF_HOUR, 2)
		.appendValue(SECOND_OF_MINUTE, 2)
		.appendFraction(NANO_OF_SECOND, 6, 6, true)
		.toFormatter();

	private static final DateTimeFormatter DT_PARSER = new DateTimeFormatterBuilder().appendValue(YEAR, 4)
		.optionalStart()
		.appendValue(MONTH_OF_YEAR, 2)
		.optionalStart()
		.appendValue(DAY_OF_MONTH, 2)
		.optionalStart()
		.appendValue(HOUR_OF_DAY, 2)
		.optionalStart()
		.appendValue(MINUTE_OF_HOUR, 2)
		.optionalStart()
		.appendValue(SECOND_OF_MINUTE, 2)
		.optionalStart()
		.appendFraction(NANO_OF_SECOND, 0, 6, true)
		.optionalEnd()
		.optionalEnd()
		.optionalEnd()
		.optionalEnd()
		.optionalEnd()
		.optionalEnd()
		.optionalStart()
		.appendOffset("+HHMM", "+0000")
		.toFormatter();

	private static final DateTimeFormatter DT_FORMATTER = new DateTimeFormatterBuilder().appendValue(YEAR, 4)
		.appendValue(MONTH_OF_YEAR, 2)
		.appendValue(DAY_OF_MONTH, 2)
		.appendValue(HOUR_OF_DAY, 2)
		.appendValue(MINUTE_OF_HOUR, 2)
		.appendValue(SECOND_OF_MINUTE, 2)
		.appendFraction(NANO_OF_SECOND, 6, 6, true)
		.optionalStart()
		.appendOffset("+HHMM", "+0000")
		.toFormatter();

	public static LocalDate parseDA(String value) {
		return LocalDate.from(DA_PARSER.parse(value));
	}

	public static String formatDA(Temporal value) {
		return DA_FORMATTER.format(value);
	}

	public static LocalTime parseTM(String value) {
		return LocalTime.from(TM_PARSER.parse(value));
	}

	public static LocalTime parseTMMax(String value) {
		return parseTM(value).plusNanos(nanosToAdd(value));
	}

	public static String formatTM(Temporal value) {
		return TM_FORMATTER.format(value);
	}

	public static Temporal parseDT(String value) {
		TemporalAccessor temporal = DT_PARSER.parse(value);
		LocalDate date = temporal.isSupported(DAY_OF_MONTH) ? LocalDate.from(temporal)
				: LocalDate.of(temporal.get(YEAR), getMonth(temporal), 1);
		LocalTime time = temporal.isSupported(HOUR_OF_DAY) ? LocalTime.from(temporal) : LocalTime.MIN;
		LocalDateTime dateTime = LocalDateTime.of(date, time);
		return temporal.isSupported(OFFSET_SECONDS)
				? ZonedDateTime.of(dateTime, ZoneOffset.ofTotalSeconds(temporal.get(OFFSET_SECONDS))) : dateTime;
	}

	public static LocalDateTime dateTime(LocalDate date, LocalTime time) {
		if (date == null) {
			return null;
		}
		if (time == null) {
			return date.atStartOfDay();
		}
		return LocalDateTime.of(date, time);
	}

	private static int getMonth(TemporalAccessor temporal) {
		return temporal.isSupported(MONTH_OF_YEAR) ? temporal.get(MONTH_OF_YEAR) : 1;
	}

	public static Temporal parseDTMax(String value) {
		int length = lengthWithoutZone(value);
		return length > 8 ? parseDT(value).plus(nanosToAdd(length - 8), ChronoUnit.NANOS)
				: parseDT(value).plus(1, yearsMonthsDays(length)).minus(1, ChronoUnit.NANOS);
	}

	public static String formatDT(Temporal value) {
		return DT_FORMATTER.format(value);
	}

	public static String truncateTM(String value, int maxLength) {
		if (maxLength < 2) {
			throw new IllegalArgumentException("maxLength %d < 2" + maxLength);
		}

		return truncate(value, value.length(), maxLength, 8);
	}

	public static String truncateDT(String value, int maxLength) {
		if (maxLength < 4) {
			throw new IllegalArgumentException("maxLength %d < 4" + maxLength);
		}

		int index = indexOfZone(value);
		return index < 0 ? truncate(value, value.length(), maxLength, 16)
				: truncate(value, index, maxLength, 16) + value.substring(index);
	}

	private static long nanosToAdd(String tm) {
		int length = tm.length();
		int index = tm.lastIndexOf(':');
		if (index > 0) {
			length--;
			if (index > 4) {
				length--;
			}
		}
		return nanosToAdd(length);
	}

	private static long nanosToAdd(int length) {
		switch (length) {
			case 2:
				return 3599999999999L;
			case 4:
				return 59999999999L;
			case 6:
			case 7:
				return 999999999L;
			case 8:
				return 99999999L;
			case 9:
				return 9999999L;
			case 10:
				return 999999L;
			case 11:
				return 99999L;
			case 12:
				return 9999L;
			case 13:
				return 999L;
		}
		throw new IllegalArgumentException("length: " + length);
	}

	private static ChronoUnit yearsMonthsDays(int length) {
		switch (length) {
			case 4:
				return ChronoUnit.YEARS;
			case 6:
				return ChronoUnit.MONTHS;
			case 8:
				return ChronoUnit.DAYS;
		}
		throw new IllegalArgumentException("length: " + length);
	}

	private static int lengthWithoutZone(String value) {
		int index = indexOfZone(value);
		return index < 0 ? value.length() : index;
	}

	private static int indexOfZone(String value) {
		int index = value.length() - 5;
		return index >= 4 && isSign(value.charAt(index)) ? index : -1;
	}

	private static boolean isSign(char ch) {
		return ch == '+' || ch == '-';
	}

	private static String truncate(String value, int length, int maxLength, int fractionPos) {
		return value.substring(0, adjustMaxLength(Math.min(length, maxLength), fractionPos));
	}

	private static int adjustMaxLength(int maxLength, int fractionPos) {
		return maxLength < fractionPos ? maxLength & ~1 : maxLength;
	}

	/**
	 * Conversion from old to new Time API
	 *
	 * @author Nicolas Roduit
	 */
	private static final DateTimeFormatter defaultDateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

	private static final DateTimeFormatter defaultTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);

	private static final DateTimeFormatter defaultDateTimeFormatter = DateTimeFormatter
		.ofLocalizedDateTime(FormatStyle.MEDIUM);

	/**
	 * Convert date or time object to display date in String with FormatStyle.MEDIUM
	 * @param date
	 * @return the time to display with FormatStyle.MEDIUM
	 */
	public static String formatDateTime(TemporalAccessor date) {
		if (date instanceof LocalDate) {
			return defaultDateFormatter.format(date);
		}
		else if (date instanceof LocalTime) {
			return defaultTimeFormatter.format(date);
		}
		else if (date instanceof LocalDateTime || date instanceof ZonedDateTime) {
			return defaultDateTimeFormatter.format(date);
		}
		else if (date instanceof Instant) {
			return defaultDateTimeFormatter.format(((Instant) date).atZone(ZoneId.systemDefault()));
		}
		return "";
	}

	public static LocalDate toLocalDate(Date date) {
		if (date != null) {
			try {
				LocalDateTime datetime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
				return datetime.toLocalDate();
			}
			catch (Exception e) {
				log.error("Date conversion", e);
			}
		}
		return null;
	}

	public static LocalTime toLocalTime(Date date) {
		if (date != null) {
			try {
				LocalDateTime datetime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
				return datetime.toLocalTime();
			}
			catch (Exception e) {
				log.error("Time conversion", e);
			}
		}
		return null;
	}

	public static LocalDateTime toLocalDateTime(Date date) {
		if (date != null) {
			try {
				return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
			}
			catch (Exception e) {
				log.error("DateTime conversion", e);
			}
		}
		return null;
	}

	public static Date dateTime(Date date, Date time) {
		if (time == null) {
			return date;
		}
		else if (date == null) {
			return time;
		}
		Calendar calendarA = Calendar.getInstance();
		calendarA.setTime(date);

		Calendar calendarB = Calendar.getInstance();
		calendarB.setTime(time);

		calendarA.set(Calendar.HOUR_OF_DAY, calendarB.get(Calendar.HOUR_OF_DAY));
		calendarA.set(Calendar.MINUTE, calendarB.get(Calendar.MINUTE));
		calendarA.set(Calendar.SECOND, calendarB.get(Calendar.SECOND));
		calendarA.set(Calendar.MILLISECOND, calendarB.get(Calendar.MILLISECOND));

		return calendarA.getTime();
	}

}
