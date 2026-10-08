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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.karnak.backend.data.entity.TransferSeriesReasonEntity;
import org.karnak.backend.data.entity.TransferSeriesStatusEntity;
import org.karnak.backend.data.repo.TransferSeriesInstanceRepo;
import org.karnak.backend.data.repo.TransferSeriesReasonRepo;
import org.karnak.backend.data.repo.TransferSeriesStatusRepo;
import org.karnak.backend.data.repo.specification.TransferSeriesSpecifications;
import org.karnak.backend.model.event.TransferMonitoringEvent;
import org.karnak.backend.model.monitoring.MonitoringEntry;
import org.karnak.backend.model.monitoring.MonitoringSearchCriteria;
import org.karnak.backend.model.monitoring.TransferSeriesStatusModel;
import org.karnak.frontend.monitoring.component.ExportSettings;
import org.karnak.frontend.monitoring.component.MonitoringCsvMappingStrategy;
import org.karnak.frontend.monitoring.component.TransferStatusFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
	 * Outcomes not yet written, per series, in arrival order. Filled by the forwarding
	 * threads, drained by {@link #flushPendingEntries()}. Per-key {@code compute} on the
	 * writers and {@code remove} on the drain are both atomic, so a batch is either still
	 * here or exclusively held by the flush, never both.
	 */
	private final ConcurrentHashMap<SeriesKey, List<MonitoringEntry>> pending = new ConcurrentHashMap<>();

	private record SeriesKey(Long forwardNodeId, Long destinationId, String serieUidOriginal) {
	}

	/**
	 * Listener on TransferMonitoringEvent: queues the outcome for the next flush. Runs on
	 * the forwarding thread and only touches memory: the database write is batched per
	 * series by {@link #flushPendingEntries()}, because one transaction per object made
	 * the database commit rate the ceiling of the whole ingest.
	 */
	@EventListener
	public void onTransferMonitoringEvent(TransferMonitoringEvent transferMonitoringEvent) {
		MonitoringEntry entry = transferMonitoringEvent.getEntry();
		SeriesKey key = new SeriesKey(entry.forwardNodeId(), entry.destinationId(), entry.serieUidOriginal());
		pending.compute(key, (k, batch) -> {
			List<MonitoringEntry> list = batch != null ? batch : new ArrayList<>();
			list.add(entry);
			return list;
		});
	}

	/**
	 * Writes every pending batch, one transaction per series, retrying the first-insert
	 * race for a brand-new series. A failing series is logged and skipped so the others
	 * are still written and the schedule keeps running.
	 */
	@Scheduled(fixedDelayString = "${monitoring.flush-interval-ms:1000}")
	public void flushPendingEntries() {
		for (SeriesKey key : pending.keySet()) {
			List<MonitoringEntry> batch = pending.remove(key);
			if (batch != null && !batch.isEmpty()) {
				writeBatch(batch);
			}
		}
	}

	private void writeBatch(List<MonitoringEntry> batch) {
		String serieUid = batch.getFirst().serieUidOriginal();
		for (int attempt = 1; attempt <= MAX_UPSERT_ATTEMPTS; attempt++) {
			try {
				monitoringWriteService.upsertAll(batch);
				return;
			}
			catch (DataIntegrityViolationException e) {
				// Concurrent creation of the same series: retry, the row now exists
				if (attempt == MAX_UPSERT_ATTEMPTS) {
					log.warn("Could not record {} monitoring entries for series {} after {} attempts", batch.size(),
							serieUid, MAX_UPSERT_ATTEMPTS, e);
				}
			}
			catch (RuntimeException e) {
				log.warn("Could not record {} monitoring entries for series {}", batch.size(), serieUid, e);
				return;
			}
		}
	}

	/** Writes what is still pending before the context goes down. */
	@PreDestroy
	void flushOnShutdown() {
		flushPendingEntries();
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
		pending.clear();
		reasonRepo.deleteAllInBatch();
		instanceRepo.deleteAllInBatch();
		seriesRepo.deleteAllInBatch();
	}

	/** Retrieve the series rows matching the filter. */
	public List<TransferSeriesStatusEntity> retrieveSeries(TransferStatusFilter filter) {
		return filter.hasFilter() ? seriesRepo.findAll(TransferSeriesSpecifications.matchesFilter(filter))
				: seriesRepo.findAll();
	}

	/**
	 * Retrieve a page of series rows (the per-series transfer models) matching the
	 * criteria.
	 */
	@Transactional(readOnly = true)
	public Page<TransferSeriesStatusModel> retrieveSeries(MonitoringSearchCriteria criteria, Pageable pageable) {
		Page<TransferSeriesStatusEntity> entityPage = seriesRepo.findAll(specificationOf(criteria), pageable);
		return entityPage.map(this::toModel);
	}

	/** Count the series rows (the per-series transfer models) matching the criteria. */
	@Transactional(readOnly = true)
	public long countSeries(MonitoringSearchCriteria criteria) {
		return seriesRepo.count(specificationOf(criteria));
	}

	private Specification<TransferSeriesStatusEntity> specificationOf(MonitoringSearchCriteria criteria) {
		TransferStatusFilter filter = MonitoringSearchCriteria.toTransferStatusFilter(criteria);
		UUID destinationUuid = criteria == null ? null : criteria.destinationUuid();
		return TransferSeriesSpecifications.matchesFilter(filter)
			.and(TransferSeriesSpecifications.hasDestinationUuid(destinationUuid));
	}

	/**
	 * Build a per-series CSV for the rows matching the given criteria (one row per
	 * destination/study/series, with counts and the joined error reasons).
	 */
	public byte[] buildCsv(MonitoringSearchCriteria criteria, ExportSettings exportSettings)
			throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException, IOException {
		List<TransferSeriesStatusEntity> rows = seriesRepo.findAll(specificationOf(criteria));
		return buildCsv(rows, exportSettings);
	}

	/**
	 * Build a per-series CSV for the matching rows (one row per destination/study/series,
	 * with counts and the joined error reasons).
	 */
	public byte[] buildCsv(TransferStatusFilter filter, ExportSettings exportSettings)
			throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException, IOException {
		return buildCsv(retrieveSeries(filter), exportSettings);
	}

	/** Shared CSV writer for a resolved list of series rows. */
	private byte[] buildCsv(List<TransferSeriesStatusEntity> rows, ExportSettings exportSettings)
			throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException, IOException {
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

	/** Maps a {@link TransferSeriesStatusEntity} to the API model (no DB ids). */
	private TransferSeriesStatusModel toModel(TransferSeriesStatusEntity e) {
		var fn = e.getForwardNodeEntity();
		var dest = e.getDestinationEntity();
		return new TransferSeriesStatusModel(fn != null ? fn.getUuid() : null, dest != null ? dest.getUuid() : null,
				fn != null ? fn.getFwdAeTitle() : null, fn != null ? fn.getFwdDescription() : null,
				dest != null ? dest.getDescription() : null, e.getPatientIdOriginal(), e.getPatientIdToSend(),
				e.getAccessionNumberOriginal(), e.getAccessionNumberToSend(), e.getStudyDescriptionOriginal(),
				e.getStudyDescriptionToSend(), e.getStudyDateOriginal(), e.getStudyDateToSend(),
				e.getStudyUidOriginal(), e.getStudyUidToSend(), e.getSerieDescriptionOriginal(),
				e.getSerieDescriptionToSend(), e.getSerieDateOriginal(), e.getSerieDateToSend(),
				e.getSerieUidOriginal(), e.getSerieUidToSend(), e.getModality(), e.getSopClassUids(), e.getInstances(),
				e.getRetries(), e.getSent(), e.getErrors(), e.getExcluded(), e.getFirstSeen(), e.getLastSeen());
	}

}
