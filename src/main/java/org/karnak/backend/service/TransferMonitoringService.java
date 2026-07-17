/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.karnak.backend.data.entity.TransferSeriesReasonEntity;
import org.karnak.backend.data.entity.TransferSeriesStatusEntity;
import org.karnak.backend.data.repo.TransferSeriesInstanceRepo;
import org.karnak.backend.data.repo.TransferSeriesReasonRepo;
import org.karnak.backend.data.repo.TransferSeriesStatusRepo;
import org.karnak.backend.data.repo.specification.TransferSeriesSpecification;
import org.karnak.backend.model.event.TransferMonitoringEvent;
import org.karnak.backend.model.monitoring.MonitoringEntry;
import org.karnak.frontend.monitoring.component.ExportSettings;
import org.karnak.frontend.monitoring.component.MonitoringCsvMappingStrategy;
import org.karnak.frontend.monitoring.component.TransferStatusFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Handle transfer monitoring: folds transfer outcomes into the aggregated
 * {@code transfer_series_status} table (one row per series), purges old rows and exports
 * a per-series CSV.
 */
@Service
@Slf4j
public class TransferMonitoringService {

	/** Retries for the first-event-of-a-series insert race on the unique key. */
	private static final int MAX_UPSERT_ATTEMPTS = 3;

	private static final String REASON_JOIN = "; ";

	@Value("${monitoring.max-history-days:30}")
	private int maxHistoryDays;

	private final TransferSeriesStatusRepo seriesRepo;

	private final TransferSeriesReasonRepo reasonRepo;

	private final TransferSeriesInstanceRepo instanceRepo;

	private final MonitoringWriteService monitoringWriteService;

	@Autowired
	public TransferMonitoringService(final TransferSeriesStatusRepo seriesRepo,
			final TransferSeriesReasonRepo reasonRepo, final TransferSeriesInstanceRepo instanceRepo,
			final MonitoringWriteService monitoringWriteService) {
		this.seriesRepo = seriesRepo;
		this.reasonRepo = reasonRepo;
		this.instanceRepo = instanceRepo;
		this.monitoringWriteService = monitoringWriteService;
	}

	/**
	 * Listener on TransferMonitoringEvent: fold the outcome into the series aggregate,
	 * retrying the first-insert race for a brand-new series.
	 */
	@Async
	@EventListener
	public void onTransferMonitoringEvent(TransferMonitoringEvent transferMonitoringEvent) {
		MonitoringEntry entry = transferMonitoringEvent.getEntry();
		for (int attempt = 1; attempt <= MAX_UPSERT_ATTEMPTS; attempt++) {
			try {
				monitoringWriteService.upsert(entry);
				return;
			}
			catch (DataIntegrityViolationException e) {
				// Concurrent creation of the same series: retry, the row now exists
				if (attempt == MAX_UPSERT_ATTEMPTS) {
					log.warn("Could not record monitoring entry for series {} after {} attempts",
							entry.serieUidOriginal(), MAX_UPSERT_ATTEMPTS, e);
				}
			}
		}
	}

	/**
	 * Occurs every hour: clean the series aggregate table over a certain number of days
	 * (reason rows cascade).
	 */
	@Scheduled(cron = "0 0 * * * *")
	public void cleanupOldRecords() {
		seriesRepo.deleteOlderThan(LocalDateTime.now(ZoneId.of("CET")).minusDays(this.maxHistoryDays));
	}

	/** Delete all monitoring records. */
	public void deleteAllTransferStatus() {
		reasonRepo.deleteAllInBatch();
		instanceRepo.deleteAllInBatch();
		seriesRepo.deleteAllInBatch();
	}

	/** Retrieve the series rows matching the filter. */
	public List<TransferSeriesStatusEntity> retrieveSeries(TransferStatusFilter filter) {
		return filter.hasFilter() ? seriesRepo.findAll(new TransferSeriesSpecification(filter)) : seriesRepo.findAll();
	}

	/**
	 * Build a per-series CSV for the matching rows (one row per destination/study/series,
	 * with counts and the joined error reasons).
	 */
	public byte[] buildCsv(TransferStatusFilter filter, ExportSettings exportSettings)
			throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException, IOException {
		List<TransferSeriesStatusEntity> rows = retrieveSeries(filter);
		populateReasons(rows);

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		OutputStreamWriter streamWriter = new OutputStreamWriter(stream);
		CSVWriter writer = new CSVWriter(streamWriter,
				exportSettings.getDelimiter() != null ? exportSettings.getDelimiter().charAt(0)
						: ExportSettings.DEFAULT_CSV_DELIMITER,
				CSVWriter.DEFAULT_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);

		StatefulBeanToCsv<TransferSeriesStatusEntity> beanToCsv = new StatefulBeanToCsvBuilder<TransferSeriesStatusEntity>(
				writer)
			.withMappingStrategy(new MonitoringCsvMappingStrategy<>())
			.build();
		beanToCsv.write(rows);

		streamWriter.flush();
		return stream.toByteArray();
	}

	/** Fills each row's transient {@code reasons} with its distinct error reasons. */
	private void populateReasons(List<TransferSeriesStatusEntity> rows) {
		if (rows.isEmpty()) {
			return;
		}
		List<Long> ids = rows.stream().map(TransferSeriesStatusEntity::getId).toList();
		Map<Long, String> reasonsById = reasonRepo.findBySeriesStatusIdIn(ids)
			.stream()
			.collect(Collectors.groupingBy(TransferSeriesReasonEntity::getSeriesStatusId,
					Collectors.mapping(TransferSeriesReasonEntity::getReason, Collectors.joining(REASON_JOIN))));
		rows.forEach(row -> row.setReasons(reasonsById.getOrDefault(row.getId(), "")));
	}

}
