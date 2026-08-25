/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.validation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.dcm4che3.data.Tag;
import org.karnak.backend.model.validation.ConformanceReport.FindingSummary;
import org.karnak.backend.model.validation.ConformanceReport.SeriesSummary;

/**
 * Accumulates the conformance data of one study transfer batch. Memory stays bounded:
 * identical findings across instances of the same SOP Class are deduplicated with an
 * occurrence count, and only lightweight per-series tuples are kept — never datasets.
 *
 * <p>
 * Thread-safe: instances are mutated by async collectors while a scheduled flusher may
 * concurrently {@link #close()} them. After close, {@link #add} returns {@code false} and
 * the caller must start a fresh accumulator.
 */
public class StudyConformanceAccumulator {

	private static final int MAX_FAILURE_REASONS = 10;

	private final StudyKey key;

	private final String sourceAet;

	// When false, the destination does not de-identify: the Patient Name is real PHI and
	// is
	// not collected, so it appears neither in the header nor in any consistency finding
	private final boolean deidentified;

	private final CuratedValidationRules rules;

	private final Instant createdAt;

	private Instant lastUpdatedAt;

	private boolean closed;

	// First non-empty de-identified study-level values, for the report header
	private String patientId = "";

	private String patientName = "";

	private String studyDate = "";

	private String studyDescription = "";

	private String accessionNumber = "";

	private final Map<String, SeriesData> seriesByUid = new LinkedHashMap<>();

	private final Set<String> studyUidsSeen = new LinkedHashSet<>();

	private final Set<String> patientIds = new LinkedHashSet<>();

	private final Set<String> patientNames = new LinkedHashSet<>();

	private final Set<String> modalities = new LinkedHashSet<>();

	private final Set<String> sopClassUids = new LinkedHashSet<>();

	private final Set<String> transferSyntaxUids = new LinkedHashSet<>();

	private final Map<String, Map<ConformanceFinding, FindingStats>> findingsBySopClass = new LinkedHashMap<>();

	private int failedInstanceCount;

	private final Set<String> failureReasons = new LinkedHashSet<>();

	// Burned-in identity check: tag name -> number of instances it was detected in
	private final Map<String, Integer> detectedIdentityTags = new LinkedHashMap<>();

	private int imageIdentityCheckedInstances;

	private int imageIdentityCheckErrors;

	public StudyConformanceAccumulator(StudyKey key, String sourceAet, boolean deidentified,
			CuratedValidationRules rules, Instant now) {
		this.key = key;
		this.sourceAet = sourceAet;
		this.deidentified = deidentified;
		this.rules = rules;
		this.createdAt = now;
		this.lastUpdatedAt = now;
	}

	/**
	 * Adds one forwarded instance and its validation result (null when the instance was
	 * not validated, e.g. a failed transfer).
	 * @return false when this accumulator is already closed — the instance was not added
	 */
	public synchronized boolean add(InstanceConformanceData data, InstanceValidationResult result, Instant now) {
		return add(data, result, null, now);
	}

	/**
	 * Adds one forwarded instance, its validation result and the outcome of the burned-in
	 * identity OCR check (null when the check was not run for this instance).
	 * @return false when this accumulator is already closed - the instance was not added
	 */
	public synchronized boolean add(InstanceConformanceData data, InstanceValidationResult result,
			ImageIdentityCheckOutcome identityOutcome, Instant now) {
		if (closed) {
			return false;
		}
		lastUpdatedAt = now;
		var metadata = data.snapshot().metadata();
		patientId = firstNonEmpty(patientId, data.snapshot().metadata().getString(Tag.PatientID, ""));
		studyDate = firstNonEmpty(studyDate, metadata.getString(Tag.StudyDate, ""));
		studyDescription = firstNonEmpty(studyDescription, metadata.getString(Tag.StudyDescription, ""));
		accessionNumber = firstNonEmpty(accessionNumber, metadata.getString(Tag.AccessionNumber, ""));

		studyUidsSeen.add(data.studyUid());
		patientIds.add(metadata.getString(Tag.PatientID, ""));
		// Patient Name is real PHI when the destination does not de-identify: do not
		// collect
		// it, so it leaks neither into the header nor into the identity-consistency check
		if (deidentified) {
			patientName = firstNonEmpty(patientName, metadata.getString(Tag.PatientName, ""));
			patientNames.add(metadata.getString(Tag.PatientName, ""));
		}
		if (!data.modality().isEmpty()) {
			modalities.add(data.modality());
		}
		if (!data.sopClassUid().isEmpty()) {
			sopClassUids.add(data.sopClassUid());
		}
		if (data.transferSyntaxUid() != null && !data.transferSyntaxUid().isEmpty()) {
			transferSyntaxUids.add(data.transferSyntaxUid());
		}

		SeriesData series = seriesByUid.computeIfAbsent(data.seriesUid(), uid -> new SeriesData());
		series.modality = firstNonEmpty(series.modality, data.modality());
		if (!data.sopClassUid().isEmpty()) {
			series.sopClassUids.add(data.sopClassUid());
		}
		series.sopInstanceUids.add(data.sopInstanceUid());
		String frameOfReferenceUid = metadata.getString(Tag.FrameOfReferenceUID, "");
		if (!frameOfReferenceUid.isEmpty()) {
			series.frameOfReferenceUids.add(frameOfReferenceUid);
		}

		recordFailure(data);
		recordFindings(data, result);
		recordIdentityOutcome(identityOutcome);
		return true;
	}

	// Called only from the synchronized add(...) overload, so no extra locking is needed.
	private void recordFailure(InstanceConformanceData data) {
		if (data.sent()) {
			return;
		}
		failedInstanceCount++;
		if (data.failureReason() != null && failureReasons.size() < MAX_FAILURE_REASONS) {
			failureReasons.add(data.failureReason());
		}
	}

	private void recordFindings(InstanceConformanceData data, InstanceValidationResult result) {
		if (result == null) {
			return;
		}
		Map<ConformanceFinding, FindingStats> findings = findingsBySopClass.computeIfAbsent(data.sopClassUid(),
				uid -> new LinkedHashMap<>());
		for (ConformanceFinding finding : result.findings()) {
			findings.computeIfAbsent(finding, f -> new FindingStats(data.sopInstanceUid())).count++;
		}
	}

	private void recordIdentityOutcome(ImageIdentityCheckOutcome identityOutcome) {
		if (identityOutcome == null) {
			return;
		}
		if (identityOutcome.failed()) {
			imageIdentityCheckErrors++;
			return;
		}
		imageIdentityCheckedInstances++;
		for (String tag : identityOutcome.detectedTags()) {
			detectedIdentityTags.merge(tag, 1, Integer::sum);
		}
	}

	/**
	 * Closes the accumulator, runs the study-level consistency checks and builds the
	 * immutable report. Subsequent {@link #add} calls are rejected.
	 */
	public synchronized ConformanceReport close() {
		closed = true;
		List<ConformanceFinding> consistencyFindings = StudyConsistencyChecker.check(this);

		Map<String, List<FindingSummary>> summariesBySopClass = new LinkedHashMap<>();
		findingsBySopClass.forEach((sopClassUid, findings) -> {
			List<FindingSummary> summaries = new ArrayList<>(findings.size());
			findings.forEach((finding, stats) -> summaries
				.add(new FindingSummary(finding, stats.count, stats.exampleSopInstanceUid)));
			summaries.sort((a, b) -> a.finding().severity().compareTo(b.finding().severity()));
			summariesBySopClass.put(sopClassUid, List.copyOf(summaries));
		});

		int errorCount = countBySeverity(summariesBySopClass, consistencyFindings, Severity.ERROR);
		int warningCount = countBySeverity(summariesBySopClass, consistencyFindings, Severity.WARNING);
		int infoCount = countBySeverity(summariesBySopClass, consistencyFindings, Severity.INFO);
		int instanceCount = seriesByUid.values().stream().mapToInt(series -> series.sopInstanceUids.size()).sum();

		List<SeriesSummary> series = seriesByUid.entrySet()
			.stream()
			.map(entry -> new SeriesSummary(entry.getKey(), entry.getValue().modality,
					Set.copyOf(entry.getValue().sopClassUids), entry.getValue().sopInstanceUids.size()))
			.toList();

		return new ConformanceReport(key, sourceAet, deidentified, patientId, patientName, studyDate, studyDescription,
			accessionNumber, seriesByUid.size(), instanceCount, failedInstanceCount, List.copyOf(failureReasons),
				Set.copyOf(modalities), Set.copyOf(sopClassUids), Set.copyOf(transferSyntaxUids), series,
				summariesBySopClass, consistencyFindings, errorCount, warningCount, infoCount, errorCount == 0,
			createdAt, lastUpdatedAt, Map.copyOf(detectedIdentityTags), imageIdentityCheckedInstances,
			imageIdentityCheckErrors);
	}

	public synchronized Instant getLastUpdatedAt() {
		return lastUpdatedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public StudyKey getKey() {
		return key;
	}

	/** Counts finding occurrences (a finding hitting N instances counts N times). */
	private static int countBySeverity(Map<String, List<FindingSummary>> summariesBySopClass,
			List<ConformanceFinding> consistencyFindings, Severity severity) {
		long instanceFindings = summariesBySopClass.values()
			.stream()
			.flatMap(List::stream)
			.filter(summary -> summary.finding().severity() == severity)
			.mapToLong(FindingSummary::count)
			.sum();
		long studyFindings = consistencyFindings.stream().filter(finding -> finding.severity() == severity).count();
		return (int) (instanceFindings + studyFindings);
	}

	private static String firstNonEmpty(String current, String candidate) {
		return current.isEmpty() ? candidate : current;
	}

	// Accessors for the consistency checker (same package)
	CuratedValidationRules rules() {
		return rules;
	}

	Set<String> studyUidsSeen() {
		return studyUidsSeen;
	}

	Set<String> patientIds() {
		return patientIds;
	}

	Set<String> patientNames() {
		return patientNames;
	}

	Set<String> transferSyntaxUids() {
		return transferSyntaxUids;
	}

	Map<String, SeriesData> seriesByUid() {
		return seriesByUid;
	}

	/** Lightweight per-series accumulation. */
	static class SeriesData {

		String modality = "";

		final Set<String> sopClassUids = new LinkedHashSet<>();

		final Set<String> sopInstanceUids = new LinkedHashSet<>();

		final Set<String> frameOfReferenceUids = new LinkedHashSet<>();

	}

	private static class FindingStats {

		int count;

		final String exampleSopInstanceUid;

		FindingStats(String exampleSopInstanceUid) {
			this.exampleSopInstanceUid = exampleSopInstanceUid;
		}

	}

}
