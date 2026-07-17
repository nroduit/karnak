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
import org.karnak.backend.data.entity.TransferSeriesInstanceEntity;
import org.karnak.backend.data.entity.TransferSeriesReasonEntity;
import org.karnak.backend.data.entity.TransferSeriesStatusEntity;
import org.karnak.backend.data.repo.TransferSeriesInstanceRepo;
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

	/**
	 * How one outcome relates to the distinct instances already recorded for its series:
	 * {@code NEW} a first-seen SOP instance, {@code KNOWN} an already-seen one (a
	 * re-send), {@code UNIDENTIFIED} an outcome with no original SOP Instance UID that
	 * cannot be attributed to an instance.
	 */
	private enum InstanceNovelty {

		NEW, KNOWN, UNIDENTIFIED

	}

	private final TransferSeriesStatusRepo seriesRepo;

	private final TransferSeriesReasonRepo reasonRepo;

	private final TransferSeriesInstanceRepo instanceRepo;

	@Autowired
	public MonitoringWriteService(final TransferSeriesStatusRepo seriesRepo, final TransferSeriesReasonRepo reasonRepo,
			final TransferSeriesInstanceRepo instanceRepo) {
		this.seriesRepo = seriesRepo;
		this.reasonRepo = reasonRepo;
		this.instanceRepo = instanceRepo;
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
			// Persist the empty row first so the instance rows can reference its id; a
			// concurrent creation of the same series fails the unique key here and the
			// caller retries (the row then exists and is taken under the lock above).
			series = seriesRepo.saveAndFlush(newSeries(entry, serieKey));
		}

		InstanceNovelty novelty = registerInstance(series.getId(), entry);
		apply(series, entry, novelty);
		seriesRepo.saveAndFlush(series);

		// Record the reason for any non-delivered outcome: an error, or an
		// editor-requested abort (counted as excluded). A sent object or a bare 409
		// retry has no reason. The reason is booked in the same buckets as the series
		// row:
		// error vs excluded on the delivery axis, plus retry when it hit an already-seen
		// instance (novelty axis), so the leaf breakdown mirrors the series counters.
		boolean excluded = !entry.sent() && !entry.error() && !entry.duplicate();
		if ((entry.error() || excluded) && StringUtils.isNotBlank(entry.reason())) {
			incrementReason(series.getId(), truncate(entry.reason(), MAX_REASON_LENGTH), entry.error(),
					novelty == InstanceNovelty.KNOWN);
		}
	}

	/**
	 * Classifies one outcome against the instances already recorded for the series and,
	 * for a newly seen instance, persists its original SOP Instance UID. The membership
	 * check + insert are safe without an upsert because the caller holds the pessimistic
	 * series lock, so writes for the same series are serialized.
	 *
	 * <p>
	 * A blank UID cannot be de-duplicated: such an event is reported as
	 * {@link InstanceNovelty#UNIDENTIFIED} so it is kept out of the distinct-instance
	 * count (its delivery outcome is still recorded). Counting it as a new instance would
	 * let repeated identity-less failures — e.g. an error raised before the original
	 * attributes are read — inflate the {@code instances} counter without bound.
	 */
	private InstanceNovelty registerInstance(Long seriesStatusId, MonitoringEntry entry) {
		String uid = entry.sopInstanceUidOriginal();
		if (StringUtils.isBlank(uid)) {
			return InstanceNovelty.UNIDENTIFIED;
		}
		if (instanceRepo.existsBySeriesStatusIdAndSopInstanceUid(seriesStatusId, uid)) {
			return InstanceNovelty.KNOWN;
		}
		instanceRepo.saveAndFlush(new TransferSeriesInstanceEntity(seriesStatusId, uid));
		return InstanceNovelty.NEW;
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

	private void apply(TransferSeriesStatusEntity series, MonitoringEntry entry, InstanceNovelty novelty) {
		// Novelty bucket: the first send of a distinct instance bumps instances; a
		// re-send (already-seen UID, or a 409 "already present in destination") bumps
		// retries. An unidentified outcome (no original SOP Instance UID) belongs to
		// neither so it cannot inflate the distinct-instance count; its delivery outcome
		// is still recorded below.
		if (novelty == InstanceNovelty.NEW && !entry.duplicate()) {
			series.setInstances(series.getInstances() + 1);
		}
		else if (novelty != InstanceNovelty.UNIDENTIFIED) {
			series.setRetries(series.getRetries() + 1);
		}
		// Delivery bucket: exactly one of sent / error / excluded per outcome. A 409 is
		// counted only as a retry, so it is neither sent, errored nor excluded.
		if (entry.sent()) {
			series.setSent(series.getSent() + 1);
		}
		else if (entry.error()) {
			series.setErrors(series.getErrors() + 1);
		}
		else if (!entry.duplicate()) {
			series.setExcluded(series.getExcluded() + 1);
		}
		if (series.getLastSeen() == null || series.getLastSeen().isBefore(entry.timestamp())) {
			series.setLastSeen(entry.timestamp());
		}
		series.setSopClassUids(mergeSopClassUids(series.getSopClassUids(), entry.sopClassUid()));
	}

	/**
	 * Increment (or create) the per-reason counter in the matching delivery bucket (error
	 * vs excluded) and, when the outcome hit an already-seen instance, also its retry
	 * counter; serialized by the series lock.
	 */
	private void incrementReason(Long seriesStatusId, String reason, boolean error, boolean retry) {
		reasonRepo.findBySeriesStatusIdAndReason(seriesStatusId, reason).ifPresentOrElse(existing -> {
			if (error) {
				existing.setErrorCount(existing.getErrorCount() + 1);
			}
			else {
				existing.setExcludedCount(existing.getExcludedCount() + 1);
			}
			if (retry) {
				existing.setRetryCount(existing.getRetryCount() + 1);
			}
			reasonRepo.saveAndFlush(existing);
		}, () -> reasonRepo.saveAndFlush(
				new TransferSeriesReasonEntity(seriesStatusId, reason, error ? 1 : 0, error ? 0 : 1, retry ? 1 : 0)));
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
