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
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable result of a study conformance accumulation, ready to be rendered. All
 * patient/study values are the de-identified ones actually sent to the destination.
 */
public record ConformanceReport(StudyKey key, String sourceAet, boolean deidentified, String patientId,
		String patientName, String studyDate, String studyDescription, String accessionNumber, int seriesCount,
		int instanceCount, int failedInstanceCount, List<String> failureReasons, Set<String> modalities,
		Set<String> sopClassUids, Set<String> transferSyntaxUids, List<SeriesSummary> series,
		Map<String, List<FindingSummary>> findingsBySopClass, List<ConformanceFinding> consistencyFindings,
		int errorCount, int warningCount, int infoCount, boolean passed, Instant firstInstanceAt,
		Instant lastInstanceAt, Map<String, Integer> detectedIdentityTags, int imageIdentityCheckedInstances,
		int imageIdentityCheckErrors) {

	/** Per-series content summary. */
	public record SeriesSummary(String seriesInstanceUid, String modality, Set<String> sopClassUids,
			int instanceCount) {
	}

	/** A deduplicated finding with the number of instances it occurred in. */
	public record FindingSummary(ConformanceFinding finding, int count, String exampleSopInstanceUid) {
	}

}
