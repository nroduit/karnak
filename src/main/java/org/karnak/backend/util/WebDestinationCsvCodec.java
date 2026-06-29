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

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.karnak.backend.data.entity.WebDestinationConfigEntity;
import org.karnak.backend.enums.DicomWebServiceType;

/**
 * Reads and writes DICOMweb endpoint configurations as CSV:
 * {@code description,url,services,group} with a header row.
 *
 * <p>
 * The {@code services} cell lists the {@link DicomWebServiceType} to probe, separated by
 * {@code ;} (so the cell never clashes with the CSV comma); an empty cell means "all
 * services". Both enum names ({@code STOW_RS}) and display names ({@code STOW-RS}) are
 * accepted on import and unknown tokens are ignored. The {@code group} column is the
 * optional organizational group. Header rows, blank lines and {@code #} comments are
 * skipped. The parser returns detached entities (no id, no de-duplication); persistence
 * and de-duplication are the caller's responsibility.
 */
public final class WebDestinationCsvCodec {

	static final String[] CSV_HEADER = { "description", "url", "services", "group" };

	private static final int MIN_COLUMNS = 2;

	private static final String SERVICE_DELIMITER = ";";

	private WebDestinationCsvCodec() {
	}

	public static CsvParseResult<WebDestinationConfigEntity> parse(InputStream inputStream, char separator) {
		List<WebDestinationConfigEntity> destinations = new ArrayList<>();
		List<String> errors = new ArrayList<>();

		var parser = new CSVParserBuilder().withSeparator(separator).build();
		try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
			.withCSVParser(parser)
			.build()) {

			String[] row;
			int line = 0;
			while ((row = reader.readNext()) != null) {
				line++;
				RowOutcome outcome = toDestination(row, line);
				if (outcome.entity() != null) {
					destinations.add(outcome.entity());
				}
				errors.addAll(outcome.errors());
			}
		}
		catch (IOException | CsvValidationException ex) {
			throw new IllegalStateException("Cannot import DICOMweb endpoints from CSV: " + ex.getMessage(), ex);
		}

		return new CsvParseResult<>(destinations, errors);
	}

	private static RowOutcome toDestination(String[] row, int line) {
		if (isBlankRow(row)) {
			return RowOutcome.skip();
		}

		String description = trim(row[0]);
		if (description.startsWith("#") || description.equalsIgnoreCase("description")
				|| description.equalsIgnoreCase("name")) {
			return RowOutcome.skip();
		}

		if (row.length < MIN_COLUMNS) {
			return RowOutcome.error(line, "expected at least " + MIN_COLUMNS + " columns (description, url)");
		}

		String url = trim(row[1]);
		if (url.equalsIgnoreCase("url")) {
			return RowOutcome.skip();
		}
		if (url.isEmpty()) {
			return RowOutcome.error(line, "URL is required");
		}
		if (!isValidUrl(url)) {
			return RowOutcome.error(line, "invalid URL '" + url + "' (expected http(s)://host[:port]/path)");
		}

		List<String> unknown = new ArrayList<>();
		String services = (row.length >= 3) ? encodeServices(trim(row[2]), unknown) : "";
		String group = (row.length >= 4) ? trim(row[3]) : "";

		var entity = new WebDestinationConfigEntity(emptyToNull(description), url, services, emptyToNull(group));
		if (unknown.isEmpty()) {
			return RowOutcome.of(entity);
		}
		return RowOutcome.of(entity, "row " + line + ": ignored unknown service(s): " + String.join(", ", unknown));
	}

	private static boolean isValidUrl(String url) {
		try {
			URI uri = URI.create(url);
			String scheme = uri.getScheme();
			return uri.getHost() != null && scheme != null
					&& (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));
		}
		catch (IllegalArgumentException ex) {
			return false;
		}
	}

	private static boolean isBlankRow(String[] row) {
		for (String cell : row) {
			if (cell != null && !cell.trim().isEmpty()) {
				return false;
			}
		}
		return true;
	}

	public static byte[] export(List<WebDestinationConfigEntity> destinations) {
		var stringWriter = new StringWriter();
		try (var csvWriter = new CSVWriter(stringWriter)) {
			csvWriter.writeNext(CSV_HEADER);
			for (WebDestinationConfigEntity destination : destinations) {
				csvWriter.writeNext(new String[] { emptyIfNull(destination.getDescription()), destination.getUrl(),
						formatServices(destination.getServices()), emptyIfNull(destination.getGroupName()) });
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException("Cannot export DICOMweb endpoints to CSV: " + ex.getMessage(), ex);
		}

		return stringWriter.toString().getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Parses a {@code ;}/{@code ,}-separated service cell into the comma-joined DB form,
	 * collecting any unrecognized tokens into {@code unknown}.
	 */
	private static String encodeServices(String cell, List<String> unknown) {
		if (cell.isBlank()) {
			return "";
		}
		Set<DicomWebServiceType> services = EnumSet.noneOf(DicomWebServiceType.class);
		for (String token : cell.split("[;,]")) {
			String trimmed = token.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			DicomWebServiceType service = toService(trimmed);
			if (service != null) {
				services.add(service);
			}
			else {
				unknown.add(trimmed);
			}
		}
		return services.stream().map(Enum::name).collect(Collectors.joining(","));
	}

	/** Renders the comma-joined DB form as a {@code ;}-separated cell for the CSV. */
	private static String formatServices(@Nullable String dbServices) {
		return (dbServices == null || dbServices.isBlank()) ? "" : dbServices.replace(",", SERVICE_DELIMITER);
	}

	private static @Nullable DicomWebServiceType toService(String token) {
		String name = token.trim().toUpperCase(Locale.ROOT).replace('-', '_');
		if (name.isEmpty()) {
			return null;
		}
		try {
			return DicomWebServiceType.valueOf(name);
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
	}

	private static String trim(@Nullable String value) {
		return (value != null) ? value.trim() : "";
	}

	private static @Nullable String emptyToNull(String value) {
		return value.isBlank() ? null : value;
	}

	private static String emptyIfNull(@Nullable String value) {
		return (value != null) ? value : "";
	}

	/**
	 * A single parsed row: the entity (null when skipped or invalid) and any messages.
	 */
	private record RowOutcome(@Nullable WebDestinationConfigEntity entity, List<String> errors) {

		static RowOutcome skip() {
			return new RowOutcome(null, List.of());
		}

		static RowOutcome of(WebDestinationConfigEntity entity) {
			return new RowOutcome(entity, List.of());
		}

		static RowOutcome of(WebDestinationConfigEntity entity, String note) {
			return new RowOutcome(entity, List.of(note));
		}

		static RowOutcome error(int line, String message) {
			return new RowOutcome(null, List.of("row " + line + ": " + message));
		}

	}

}
