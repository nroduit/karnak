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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
		upsertAll(List.of(entry));
	}

	/**
	 * Folds a batch of outcomes of <em>one</em> series (same forward node, destination
	 * and original series UID, in arrival order) into its aggregate row in a single
	 * transaction: one row lock, one lookup of the instances already recorded, one insert
	 * per new instance, one write per distinct reason and one update of the row. Per
	 * object, monitoring used to cost a transaction of its own, and at high object rates
	 * the commit rate of the database - not the gateway - became the throughput ceiling
	 * of the whole ingest. May throw {@code DataIntegrityViolationException} when two
	 * threads create the same series concurrently — the caller retries.
	 */
	@Transactional
	public void upsertAll(List<MonitoringEntry> entries) {
		if (entries == null || entries.isEmpty()) {
			return;
		}
		MonitoringEntry first = entries.getFirst();
		String serieKey = StringUtils.defaultString(first.serieUidOriginal());
		TransferSeriesStatusEntity series = seriesRepo
			.findWithLockByForwardNodeIdAndDestinationIdAndSerieUidOriginal(first.forwardNodeId(),
					first.destinationId(), serieKey)
			.orElse(null);

		if (series == null) {
			series = seriesRepo.saveAndFlush(newSeries(first, serieKey));
		}

		Set<String> known = knownInstances(series.getId(), entries);
		List<TransferSeriesInstanceEntity> newInstances = new ArrayList<>();
		// reason -> {errors, excluded, retries}, in first-seen order
		Map<String, int[]> reasons = new LinkedHashMap<>();
		for (MonitoringEntry entry : entries) {
			InstanceNovelty novelty = classify(series.getId(), entry, known, newInstances);
			apply(series, entry, novelty);

			boolean excluded = !entry.sent() && !entry.error() && !entry.duplicate();
			if ((entry.error() || excluded) && StringUtils.isNotBlank(entry.reason())) {
				int[] counts = reasons.computeIfAbsent(truncate(entry.reason(), MAX_REASON_LENGTH), r -> new int[3]);
				counts[entry.error() ? 0 : 1]++;
				if (novelty == InstanceNovelty.KNOWN) {
					counts[2]++;
				}
			}
		}
		if (!newInstances.isEmpty()) {
			instanceRepo.saveAll(newInstances);
		}
		seriesRepo.saveAndFlush(series);
		Long seriesId = series.getId();
		reasons.forEach((reason, counts) -> incrementReason(seriesId, reason, counts[0], counts[1], counts[2]));
	}

	/**
	 * The original SOP Instance UIDs of the batch that the series has already recorded,
	 * fetched in one query. Safe without an upsert because the caller holds the
	 * pessimistic series lock, so writes for the same series are serialized.
	 */
	private Set<String> knownInstances(Long seriesStatusId, List<MonitoringEntry> entries) {
		Set<String> uids = new HashSet<>();
		for (MonitoringEntry entry : entries) {
			if (StringUtils.isNotBlank(entry.sopInstanceUidOriginal())) {
				uids.add(entry.sopInstanceUidOriginal());
			}
		}
		Set<String> known = new HashSet<>();
		if (!uids.isEmpty()) {
			instanceRepo.findBySeriesStatusIdAndSopInstanceUidIn(seriesStatusId, uids)
				.forEach(instance -> known.add(instance.getSopInstanceUid()));
		}
		return known;
	}

	/**
	 * Classifies one outcome against the instances already recorded for the series
	 * (including the ones seen earlier in the same batch) and, for a newly seen instance,
	 * queues its original SOP Instance UID for insertion.
	 *
	 * <p>
	 * A blank UID cannot be de-duplicated: such an event is reported as
	 * {@link InstanceNovelty#UNIDENTIFIED} so it is kept out of the distinct-instance
	 * count (its delivery outcome is still recorded). Counting it as a new instance would
	 * let repeated identity-less failures — e.g. an error raised before the original
	 * attributes are read — inflate the {@code instances} counter without bound.
	 */
	private static InstanceNovelty classify(Long seriesStatusId, MonitoringEntry entry, Set<String> known,
			List<TransferSeriesInstanceEntity> newInstances) {
		String uid = entry.sopInstanceUidOriginal();
		if (StringUtils.isBlank(uid)) {
			return InstanceNovelty.UNIDENTIFIED;
		}
		if (!known.add(uid)) {
			return InstanceNovelty.KNOWN;
		}
		newInstances.add(new TransferSeriesInstanceEntity(seriesStatusId, uid));
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
	 * Adds the batch's counts to the per-reason counter (created if missing) in the
	 * matching delivery buckets (error vs excluded) and, for the outcomes that hit an
	 * already-seen instance, to its retry counter; serialized by the series lock.
	 */
	private void incrementReason(Long seriesStatusId, String reason, int errors, int excluded, int retries) {
		reasonRepo.findBySeriesStatusIdAndReason(seriesStatusId, reason).ifPresentOrElse(existing -> {
			existing.setErrorCount(existing.getErrorCount() + errors);
			existing.setExcludedCount(existing.getExcludedCount() + excluded);
			existing.setRetryCount(existing.getRetryCount() + retries);
			reasonRepo.saveAndFlush(existing);
		}, () -> reasonRepo
			.saveAndFlush(new TransferSeriesReasonEntity(seriesStatusId, reason, errors, excluded, retries)));
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
