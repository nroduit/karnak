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

import java.util.Arrays;
import java.util.LinkedHashSet;
import org.apache.commons.lang3.StringUtils;
import org.karnak.backend.data.entity.TransferSeriesReasonEntity;
import org.karnak.backend.data.entity.TransferSeriesStatusEntity;
import org.karnak.backend.data.repo.TransferSeriesReasonRepo;
import org.karnak.backend.data.repo.TransferSeriesStatusRepo;
import org.karnak.backend.model.monitoring.MonitoringEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Folds a single transfer outcome into the aggregated {@code transfer_series_status} row
 * (one per forward node × destination × series) and, on failure, into the per-reason
 * breakdown. The series row is taken with a pessimistic lock so concurrent increments for
 * the same series serialize and cannot lose updates; the only contended insert is the
 * very first event of a series, whose unique-constraint race is retried by the caller.
 */
@Service
public class MonitoringWriteService {

	private static final int MAX_REASON_LENGTH = 1024;

	private static final int MAX_SOP_CLASS_UIDS_LENGTH = 1024;

	private final TransferSeriesStatusRepo seriesRepo;

	private final TransferSeriesReasonRepo reasonRepo;

	@Autowired
	public MonitoringWriteService(final TransferSeriesStatusRepo seriesRepo,
			final TransferSeriesReasonRepo reasonRepo) {
		this.seriesRepo = seriesRepo;
		this.reasonRepo = reasonRepo;
	}

	/**
	 * Upsert the series aggregate for one transfer outcome. May throw
	 * {@code DataIntegrityViolationException} when two threads create the same series
	 * concurrently — the caller retries.
	 */
	@Transactional
	public void upsert(MonitoringEntry entry) {
		String serieKey = StringUtils.defaultString(entry.serieUidOriginal());
		TransferSeriesStatusEntity series = seriesRepo
			.findWithLockByForwardNodeIdAndDestinationIdAndSerieUidOriginal(entry.forwardNodeId(),
					entry.destinationId(), serieKey)
			.orElse(null);

		if (series == null) {
			series = newSeries(entry, serieKey);
			apply(series, entry);
			series = seriesRepo.saveAndFlush(series);
		}
		else {
			apply(series, entry);
			seriesRepo.saveAndFlush(series);
		}

		if (entry.error() && StringUtils.isNotBlank(entry.reason())) {
			incrementReason(series.getId(), truncate(entry.reason(), MAX_REASON_LENGTH));
		}
	}

	private TransferSeriesStatusEntity newSeries(MonitoringEntry entry, String serieKey) {
		TransferSeriesStatusEntity series = new TransferSeriesStatusEntity();
		series.setForwardNodeId(entry.forwardNodeId());
		series.setDestinationId(entry.destinationId());
		series.setPatientIdOriginal(entry.patientIdOriginal());
		series.setPatientIdToSend(entry.patientIdToSend());
		series.setAccessionNumberOriginal(entry.accessionNumberOriginal());
		series.setAccessionNumberToSend(entry.accessionNumberToSend());
		series.setStudyDescriptionOriginal(entry.studyDescriptionOriginal());
		series.setStudyDescriptionToSend(entry.studyDescriptionToSend());
		series.setStudyDateOriginal(entry.studyDateOriginal());
		series.setStudyDateToSend(entry.studyDateToSend());
		series.setStudyUidOriginal(entry.studyUidOriginal());
		series.setStudyUidToSend(entry.studyUidToSend());
		series.setSerieDescriptionOriginal(entry.serieDescriptionOriginal());
		series.setSerieDescriptionToSend(entry.serieDescriptionToSend());
		series.setSerieDateOriginal(entry.serieDateOriginal());
		series.setSerieDateToSend(entry.serieDateToSend());
		series.setSerieUidOriginal(serieKey);
		series.setSerieUidToSend(entry.serieUidToSend());
		series.setModality(entry.modality());
		series.setFirstSeen(entry.timestamp());
		series.setLastSeen(entry.timestamp());
		return series;
	}

	private void apply(TransferSeriesStatusEntity series, MonitoringEntry entry) {
		series.setInstances(series.getInstances() + 1);
		if (entry.sent()) {
			series.setSent(series.getSent() + 1);
		}
		if (entry.error()) {
			series.setErrors(series.getErrors() + 1);
		}
		if (series.getLastSeen() == null || series.getLastSeen().isBefore(entry.timestamp())) {
			series.setLastSeen(entry.timestamp());
		}
		series.setSopClassUids(mergeSopClassUids(series.getSopClassUids(), entry.sopClassUid()));
	}

	/** Increment (or create) the per-reason counter; serialized by the series lock. */
	private void incrementReason(Long seriesStatusId, String reason) {
		reasonRepo.findBySeriesStatusIdAndReason(seriesStatusId, reason).ifPresentOrElse(existing -> {
			existing.setCount(existing.getCount() + 1);
			reasonRepo.saveAndFlush(existing);
		}, () -> reasonRepo.saveAndFlush(new TransferSeriesReasonEntity(seriesStatusId, reason, 1)));
	}

	/** Distinct, comma-joined SOP class UIDs, bounded to the column length. */
	private String mergeSopClassUids(String existing, String sopClassUid) {
		if (StringUtils.isBlank(sopClassUid)) {
			return existing;
		}
		if (StringUtils.isBlank(existing)) {
			return sopClassUid;
		}
		LinkedHashSet<String> set = new LinkedHashSet<>(Arrays.asList(existing.split(",")));
		if (!set.add(sopClassUid)) {
			return existing;
		}
		String joined = String.join(",", set);
		return joined.length() > MAX_SOP_CLASS_UIDS_LENGTH ? existing : joined;
	}

	private String truncate(String value, int max) {
		return value.length() <= max ? value : value.substring(0, max);
	}

}
