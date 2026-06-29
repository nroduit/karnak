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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.karnak.backend.data.entity.DicomNodeConfigEntity;
import org.weasis.core.util.StringUtil;

/**
 * Reads and writes DICOM node configurations as CSV:
 * {@code description,aetitle,hostname,port,nodeType,nodeGroup} with a header row.
 *
 * <p>
 * The format stays backward compatible with the flat files of previous versions: a fifth
 * column historically held the group, so when a sixth {@code nodeGroup} column is absent
 * the fifth column is treated as the group (unless it is the reserved {@code WORKLIST}
 * type). Four-column files ({@code description,aetitle,hostname,port}) fall back to the
 * caller-provided default group. Header rows, blank lines and {@code #} comments are
 * skipped. The parser returns detached entities (no id, no de-duplication); persistence
 * and de-duplication are the caller's responsibility.
 */
public final class DicomNodeCsvCodec {

	static final String[] CSV_HEADER = { "description", "aetitle", "hostname", "port", "nodeType", "nodeGroup" };

	private static final int MIN_COLUMNS = 4;

	private static final int MAX_AE_TITLE_LENGTH = 16;

	private static final int MIN_PORT = 1;

	private static final int MAX_PORT = 65535;

	private static final String TYPE_WORKSTATION = "WORKSTATION";

	private static final String TYPE_WORKLIST = "WORKLIST";

	private DicomNodeCsvCodec() {
	}

	/**
	 * Parse DICOM node rows from a CSV document.
	 * @param defaultGroup the group applied to rows that carry no group column, or null
	 * for the default (ungrouped) bucket
	 * @param forceGroup when true, every non-worklist row is forced into
	 * {@code defaultGroup} regardless of the group it carries
	 */
	public static CsvParseResult<DicomNodeConfigEntity> parse(InputStream inputStream, char separator,
			@Nullable String defaultGroup, boolean forceGroup) {
		List<DicomNodeConfigEntity> nodes = new ArrayList<>();
		List<String> errors = new ArrayList<>();

		var parser = new CSVParserBuilder().withSeparator(separator).build();
		try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
			.withCSVParser(parser)
			.build()) {

			String[] row;
			int line = 0;
			while ((row = reader.readNext()) != null) {
				line++;
				RowOutcome outcome = toNode(row, defaultGroup, forceGroup, line);
				if (outcome.entity() != null) {
					nodes.add(outcome.entity());
				}
				errors.addAll(outcome.errors());
			}
		}
		catch (IOException | CsvValidationException ex) {
			throw new IllegalStateException("Cannot import DICOM nodes from CSV: " + ex.getMessage(), ex);
		}

		return new CsvParseResult<>(nodes, errors);
	}

	private static RowOutcome toNode(String[] row, @Nullable String defaultGroup, boolean forceGroup, int line) {
		if (isBlankRow(row)) {
			return RowOutcome.skip();
		}

		String description = trim(row[0]);
		if (description.startsWith("#") || description.equalsIgnoreCase("description")
				|| description.equalsIgnoreCase("name")) {
			return RowOutcome.skip();
		}

		if (row.length < MIN_COLUMNS) {
			return RowOutcome.error(line,
					"expected at least " + MIN_COLUMNS + " columns (description, AE Title, hostname, port)");
		}

		String aeTitle = trim(row[1]);
		String hostname = trim(row[2]);
		Integer port = parsePort(row[3]);

		List<String> issues = new ArrayList<>();
		validateAeTitle(aeTitle, issues);
		if (hostname.isEmpty()) {
			issues.add("hostname is required");
		}
		validatePort(port, trim(row[3]), issues);
		if (!issues.isEmpty()) {
			return RowOutcome.errors(line, issues);
		}

		int portValue = Objects.requireNonNull(port);
		String nodeType = (row.length >= 5) ? trim(row[4]) : "";
		// Worklist nodes are identified by the reserved type and never carry a group.
		if (TYPE_WORKLIST.equalsIgnoreCase(nodeType)) {
			return RowOutcome.of(new DicomNodeConfigEntity(emptyToNull(description), aeTitle, hostname, portValue,
					TYPE_WORKLIST, null));
		}

		String group = resolveGroup(row, nodeType, defaultGroup, forceGroup);
		String type = (row.length >= 6 && !nodeType.isEmpty()) ? nodeType : TYPE_WORKSTATION;
		return RowOutcome
			.of(new DicomNodeConfigEntity(emptyToNull(description), aeTitle, hostname, portValue, type, group));
	}

	private static @Nullable String resolveGroup(String[] row, String nodeType, @Nullable String defaultGroup,
			boolean forceGroup) {
		if (forceGroup) {
			return defaultGroup;
		}
		// Sixth column holds the group explicitly; otherwise the legacy fifth column was
		// the group label (WORKSTATION/empty meaning "no group").
		String group;
		if (row.length >= 6) {
			group = trim(row[5]);
		}
		else if (!nodeType.isEmpty() && !TYPE_WORKSTATION.equalsIgnoreCase(nodeType)) {
			group = nodeType;
		}
		else {
			group = "";
		}
		return group.isEmpty() ? defaultGroup : group;
	}

	private static void validateAeTitle(String aeTitle, List<String> issues) {
		if (aeTitle.isEmpty()) {
			issues.add("AE Title is required");
			return;
		}
		if (aeTitle.length() > MAX_AE_TITLE_LENGTH) {
			issues.add("AE Title '" + aeTitle + "' exceeds " + MAX_AE_TITLE_LENGTH + " characters");
		}
		if (!isPrintableAscii(aeTitle)) {
			issues.add("AE Title '" + aeTitle + "' contains invalid characters");
		}
	}

	private static void validatePort(@Nullable Integer port, String raw, List<String> issues) {
		if (port == null) {
			issues.add("port '" + raw + "' is not a number");
		}
		else if (port < MIN_PORT || port > MAX_PORT) {
			issues.add("port " + port + " is out of range (" + MIN_PORT + "-" + MAX_PORT + ")");
		}
	}

	private static boolean isPrintableAscii(String value) {
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (c < 0x20 || c > 0x7E || c == '\\') {
				return false;
			}
		}
		return true;
	}

	private static boolean isBlankRow(String[] row) {
		for (String cell : row) {
			if (StringUtil.hasText(cell)) {
				return false;
			}
		}
		return true;
	}

	public static byte[] export(List<DicomNodeConfigEntity> nodes) {
		var stringWriter = new StringWriter();
		try (var csvWriter = new CSVWriter(stringWriter)) {
			csvWriter.writeNext(CSV_HEADER);
			for (DicomNodeConfigEntity node : nodes) {
				csvWriter.writeNext(new String[] { emptyIfNull(node.getDescription()), emptyIfNull(node.getAeTitle()),
						emptyIfNull(node.getHostname()), node.getPort() == null ? "" : node.getPort().toString(),
						emptyIfNull(node.getNodeType()), emptyIfNull(node.getNodeGroup()) });
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException("Cannot export DICOM nodes to CSV: " + ex.getMessage(), ex);
		}

		return stringWriter.toString().getBytes(StandardCharsets.UTF_8);
	}

	private static String trim(@Nullable String value) {
		return (value != null) ? value.trim() : "";
	}

	private static @Nullable Integer parsePort(@Nullable String value) {
		try {
			return Integer.valueOf(trim(value));
		}
		catch (NumberFormatException _) {
			return null;
		}
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
	private record RowOutcome(@Nullable DicomNodeConfigEntity entity, List<String> errors) {

		static RowOutcome skip() {
			return new RowOutcome(null, List.of());
		}

		static RowOutcome of(DicomNodeConfigEntity entity) {
			return new RowOutcome(entity, List.of());
		}

		static RowOutcome error(int line, String message) {
			return new RowOutcome(null, List.of("row " + line + ": " + message));
		}

		static RowOutcome errors(int line, List<String> messages) {
			return new RowOutcome(null, messages.stream().map((message) -> "row " + line + ": " + message).toList());
		}

	}

}
