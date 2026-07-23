/*
 * Copyright (c) 2024-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.karnak.backend.constant.EndPoint;
import org.karnak.backend.data.entity.TransferSeriesStatusEntity;
import org.karnak.backend.enums.TransferStatusType;
import org.karnak.backend.service.TransferMonitoringService;
import org.karnak.frontend.monitoring.component.ExportSettings;
import org.karnak.frontend.monitoring.component.TransferStatusFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest controller for querying and exporting DICOM transfer monitoring data
 */
@RestController
@RequestMapping(EndPoint.MONITORING_PATH)
@Tag(name = "Monitoring", description = "API Endpoints for Transfer Monitoring")
@Slf4j
public class MonitoringController {

	private static final int MAX_PAGE_SIZE = 1000;

	private final TransferMonitoringService transferMonitoringService;

	@Autowired
	public MonitoringController(final TransferMonitoringService transferMonitoringService) {
		this.transferMonitoringService = transferMonitoringService;
	}

	@Operation(summary = "Query transfer status records",
			description = "Returns a paginated list of aggregated per-series transfer records (one row per "
					+ "forward node/destination/series). All filter parameters are optional. "
					+ "status values: ALL, SENT, NOT_SENT, EXCLUDED, ERROR")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Transfer records returned") })
	@GetMapping(value = "/transfers", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getTransfers(
			@Parameter(description = "Filter by Study UID (partial match)") @RequestParam(
					required = false) String studyUid,
			@Parameter(description = "Filter by Series UID (partial match)") @RequestParam(
					required = false) String serieUid,
			@Parameter(description = "Filter by status: ALL, SENT, NOT_SENT, EXCLUDED, ERROR") @RequestParam(
					required = false, defaultValue = "ALL") String status,
			@Parameter(
					description = "Filter from date-time (ISO format: 2024-01-01T00:00:00), applies to the series' last activity") @RequestParam(
							required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
			@Parameter(
					description = "Filter to date-time (ISO format: 2024-12-31T23:59:59), applies to the series' last activity") @RequestParam(
							required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
			@Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Page size") @RequestParam(defaultValue = "50") int size) {

		int safePage = Math.max(0, page);
		int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
		TransferStatusFilter filter = buildFilter(studyUid, serieUid, status, start, end);
		Page<TransferSeriesStatusEntity> result = transferMonitoringService.retrieveSeriesPageable(filter,
				PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "lastSeen")));
		return ResponseEntity.ok(Map.of("content", result.getContent(), "totalElements", result.getTotalElements(),
				"totalPages", result.getTotalPages(), "page", safePage, "size", safeSize));
	}

	@Operation(summary = "Export transfer status records as CSV",
			description = "Downloads a per-series CSV file of transfer records matching the filter. "
					+ "Optional ?delimiter=; param.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "CSV file returned"),
			@ApiResponse(responseCode = "500", description = "Export error", content = @Content) })
	@GetMapping(value = "/transfers/export", produces = "text/csv")
	public ResponseEntity<byte[]> exportTransfers(@RequestParam(required = false) String studyUid,
			@RequestParam(required = false) String serieUid,
			@RequestParam(required = false, defaultValue = "ALL") String status,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
			@Parameter(description = "CSV delimiter character (default: ,)") @RequestParam(
					required = false) String delimiter) {
		try {
			TransferStatusFilter filter = buildFilter(studyUid, serieUid, status, start, end);
			ExportSettings exportSettings = new ExportSettings();
			if (delimiter != null && !delimiter.isEmpty()) {
				exportSettings.setDelimiter(delimiter);
			}
			byte[] csv = transferMonitoringService.buildCsv(filter, exportSettings);
			return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"monitoring-export.csv\"")
				.contentType(MediaType.parseMediaType("text/csv"))
				.body(csv);
		}
		catch (Exception e) {
			log.error("Failed to export transfer status records", e);
			return ResponseEntity.internalServerError().build();
		}
	}

	@Operation(summary = "Count transfer status records matching the filter")
	@GetMapping(value = "/transfers/count", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> countTransfers(@RequestParam(required = false) String studyUid,
			@RequestParam(required = false) String serieUid,
			@RequestParam(required = false, defaultValue = "ALL") String status,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
		TransferStatusFilter filter = buildFilter(studyUid, serieUid, status, start, end);
		return ResponseEntity.ok(Map.of("count", transferMonitoringService.countSeries(filter)));
	}

	private TransferStatusFilter buildFilter(String studyUid, String serieUid, String status, LocalDateTime start,
			LocalDateTime end) {
		TransferStatusFilter filter = new TransferStatusFilter();
		if (studyUid != null) {
			filter.setStudyUid(studyUid);
		}
		if (serieUid != null) {
			filter.setSerieUid(serieUid);
		}
		if (status != null) {
			try {
				filter.setTransferStatusType(TransferStatusType.valueOf(status.toUpperCase()));
			}
			catch (IllegalArgumentException ignored) {
				// keep default ALL
			}
		}
		filter.setStart(start);
		filter.setEnd(end);
		return filter;
	}

}
