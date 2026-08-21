/*
 * Copyright (c) 2022-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import java.io.IOException;
import java.util.List;

import org.karnak.backend.constant.EndPoint;
import org.karnak.backend.model.monitoring.DestinationActivityModel;
import org.karnak.backend.model.monitoring.ErrorBreakdownModel;
import org.karnak.backend.model.monitoring.MonitoringSearchCriteria;
import org.karnak.backend.model.monitoring.NodeActivityModel;
import org.karnak.backend.model.monitoring.SeriesActivityModel;
import org.karnak.backend.model.monitoring.StudyActivityModel;
import org.karnak.backend.model.monitoring.TransferSeriesStatusModel;
import org.karnak.backend.service.MonitoringAggregationService;
import org.karnak.backend.service.TransferMonitoringService;
import org.karnak.frontend.monitoring.component.ExportSettings;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST controller exposing monitoring aggregation data (Destination / Study / Series /
 * error breakdown hierarchy and per-forward-node dashboard activity). Each endpoint
 * accepts an optional {@link MonitoringSearchCriteria} model attribute for filtering
 * results by status, UIDs, and date range.
 */
@RestController
@RequestMapping(EndPoint.MONITORING_PATH)
@Tag(name = "Monitoring", description = "API Endpoints for Transfer Monitoring")
public class MonitoringController {

	private final MonitoringAggregationService monitoringAggregationService;

	private final TransferMonitoringService transferMonitoringService;

	@Autowired
	public MonitoringController(final MonitoringAggregationService monitoringAggregationService,
			final TransferMonitoringService transferMonitoringService) {
		this.monitoringAggregationService = monitoringAggregationService;
		this.transferMonitoringService = transferMonitoringService;
	}

	// --- Hierarchy aggregation endpoints ------

	/** List destinations with aggregated counts, ordered by errors descending. */
	@Operation(summary = "List all destinations with aggregated transfer activity")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Destinations found"),
			@ApiResponse(responseCode = "204", description = "No destinations configured", content = @Content) })
	@GetMapping(value = "/destinations", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_search')")
	public ResponseEntity<List<DestinationActivityModel>> searchDestinations(
			@ModelAttribute MonitoringSearchCriteria criteria) {
		List<DestinationActivityModel> result = monitoringAggregationService.searchDestinations(criteria);
		return result.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(result);
	}

	/**
	 * List studies under a destination, ordered by errors descending. The destination is
	 * given by {@code criteria.destinationUuid}.
	 */
	@Operation(summary = "List studies under a destination (given by criteria.destinationUuid)")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Studies found"),
			@ApiResponse(responseCode = "204", description = "No studies found", content = @Content),
			@ApiResponse(responseCode = "400", description = "destinationUuid is missing", content = @Content) })
	@GetMapping(value = "/studies", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_search')")
	public ResponseEntity<?> searchStudies(@ModelAttribute MonitoringSearchCriteria criteria) {
		ResponseEntity<?> missing = requireDestinationUuid(criteria);
		if (missing != null) {
			return missing;
		}
		List<StudyActivityModel> result = monitoringAggregationService.searchStudies(criteria);
		return result.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(result);
	}

	/**
	 * List series under a study of a destination, ordered by errors descending. The
	 * destination and study are given by {@code criteria.destinationUuid} and
	 * {@code criteria.studyUid}.
	 */
	@Operation(summary = "List series under a study of a destination (given by criteria.destinationUuid / studyUid)")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Series found"),
			@ApiResponse(responseCode = "204", description = "No series found", content = @Content),
			@ApiResponse(responseCode = "400", description = "destinationUuid or studyUid is missing",
					content = @Content) })
	@GetMapping(value = "/series", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_search')")
	public ResponseEntity<?> searchSeries(@ModelAttribute MonitoringSearchCriteria criteria) {
		ResponseEntity<?> missing = requireDestinationUuid(criteria);
		if (missing != null) {
			return missing;
		}
		missing = requireStudyUid(criteria);
		if (missing != null) {
			return missing;
		}
		List<SeriesActivityModel> result = monitoringAggregationService.searchSeries(criteria);
		return result.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(result);
	}

	/**
	 * List error reasons for a series, given by {@code criteria.destinationUuid} and
	 * {@code criteria.serieUid}.
	 */
	@Operation(summary = "List error reasons for a series (given by criteria.destinationUuid / serieUid)")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Error reasons found"),
			@ApiResponse(responseCode = "204", description = "No errors found", content = @Content),
			@ApiResponse(responseCode = "400", description = "destinationUuid or serieUid is missing",
					content = @Content) })
	@GetMapping(value = "/errors", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_search')")
	public ResponseEntity<?> searchErrors(@ModelAttribute MonitoringSearchCriteria criteria) {
		ResponseEntity<?> missing = requireDestinationUuid(criteria);
		if (missing != null) {
			return missing;
		}
		missing = requireSerieUid(criteria);
		if (missing != null) {
			return missing;
		}
		List<ErrorBreakdownModel> result = monitoringAggregationService.searchErrors(criteria);
		return result.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(result);
	}

	// --- Forward node dashboard endpoint ------

	/** List per-forward-node activity for the dashboard. */
	@Operation(summary = "List per-forward-node activity for the dashboard")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Node activity found"),
			@ApiResponse(responseCode = "204", description = "No node activity found", content = @Content) })
	@GetMapping(value = "/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_search')")
	public ResponseEntity<List<NodeActivityModel>> searchNodeActivity(
			@ModelAttribute MonitoringSearchCriteria criteria) {
		List<NodeActivityModel> result = monitoringAggregationService.searchNodeActivity(criteria);
		return result.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(result);
	}

	// --- Per-series transfer models (paginated list / count / CSV export) ------

	/**
	 * Paginated list of the per-series transfer models matching the criteria (one entry
	 * per forward node / destination / series).
	 */
	@Operation(summary = "List transfer models matching the criteria, paginated")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Transfers found"),
			@ApiResponse(responseCode = "204", description = "No transfer found", content = @Content) })
	@GetMapping(value = "/transfers", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_search')")
	public ResponseEntity<Page<TransferSeriesStatusModel>> searchTransfers(
			@ModelAttribute MonitoringSearchCriteria criteria, Pageable pageable) {
		Page<TransferSeriesStatusModel> page = transferMonitoringService.retrieveSeries(criteria, pageable);
		return page.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(page);
	}

	/** Count of the per-series transfer models matching the criteria. */
	@Operation(summary = "Count transfer models matching the criteria")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Count computed") })
	@GetMapping(value = "/transfers/count", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_search')")
	public ResponseEntity<Long> countTransfers(@ModelAttribute MonitoringSearchCriteria criteria) {
		return ResponseEntity.ok(transferMonitoringService.countSeries(criteria));
	}

	/** Export the per-series transfer models matching the criteria as a CSV file. */
	@Operation(summary = "Export transfer models matching the criteria as CSV")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "CSV export built"),
			@ApiResponse(responseCode = "500", description = "Error building the CSV export", content = @Content) })
	@GetMapping(value = "/transfers/export", produces = "text/csv")
	@PreAuthorize("hasAuthority('karnak_search')")
	public ResponseEntity<byte[]> exportTransfers(@ModelAttribute MonitoringSearchCriteria criteria,
			@ModelAttribute ExportSettings exportSettings) {
		try {
			byte[] csv = transferMonitoringService.buildCsv(criteria, exportSettings);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentDisposition(ContentDisposition.attachment().filename("monitoring-export.csv").build());
			return ResponseEntity.ok().headers(headers).contentType(MediaType.parseMediaType("text/csv")).body(csv);
		}
		catch (CsvRequiredFieldEmptyException | CsvDataTypeMismatchException | IOException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error building the CSV export", e);
		}
	}

	// --- Maintenance ------

	/** Delete all monitoring transfer status records. */
	@Operation(summary = "Delete all monitoring transfer status records")
	@ApiResponses(value = { @ApiResponse(responseCode = "204", description = "All monitoring records deleted"),
			@ApiResponse(responseCode = "404", description = "No records to delete", content = @Content) })
	@DeleteMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_delete')")
	public ResponseEntity<Void> deleteAllMonitoring() {
		transferMonitoringService.deleteAllTransferStatus();
		return ResponseEntity.noContent().build();
	}

	// --- Search criteria validation ------

	/**
	 * Ensure the destination to search into is provided.
	 * @return a 400 Bad Request response when {@code criteria} or its
	 * {@code destinationUuid} is missing, otherwise {@code null}
	 */
	private static @Nullable ResponseEntity<?> requireDestinationUuid(@Nullable MonitoringSearchCriteria criteria) {
		if (criteria == null || criteria.destinationUuid() == null) {
			return ResponseEntity.badRequest().body("The destination (destinationUuid) must be provided.");
		}
		return null;
	}

	/**
	 * Ensure the study to search into is provided.
	 * @return a 400 Bad Request response when {@code criteria} or its {@code studyUid} is
	 * missing or blank, otherwise {@code null}
	 */
	private static @Nullable ResponseEntity<?> requireStudyUid(@Nullable MonitoringSearchCriteria criteria) {
		if (criteria == null || criteria.studyUid() == null || criteria.studyUid().isBlank()) {
			return ResponseEntity.badRequest().body("The study (studyUid) must be provided.");
		}
		return null;
	}

	/**
	 * Ensure the series to search into is provided.
	 * @return a 400 Bad Request response when {@code criteria} or its {@code serieUid} is
	 * missing or blank, otherwise {@code null}
	 */
	private static @Nullable ResponseEntity<?> requireSerieUid(@Nullable MonitoringSearchCriteria criteria) {
		if (criteria == null || criteria.serieUid() == null || criteria.serieUid().isBlank()) {
			return ResponseEntity.badRequest().body("The series (serieUid) must be provided.");
		}
		return null;
	}

}
