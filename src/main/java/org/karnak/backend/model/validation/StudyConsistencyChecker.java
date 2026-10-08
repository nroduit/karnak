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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.dcm4che3.data.Tag;
import org.dcm4che3.util.TagUtils;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.model.validation.StudyConformanceAccumulator.SeriesData;

/**
 * Study-level consistency checks run when a study transfer batch is closed: identifier
 * uniformity across the instances/series, SOP Class / Modality coherence and transfer
 * syntax acceptability.
 */
@NullUnmarked
final class StudyConsistencyChecker {

	private StudyConsistencyChecker() {
	}

	static List<ConformanceFinding> check(StudyConformanceAccumulator accumulator) {
		List<ConformanceFinding> findings = new ArrayList<>();
		CuratedValidationRules rules = accumulator.rules();

		if (accumulator.studyUidsSeen().size() > 1) {
			findings.add(new ConformanceFinding(TagUtils.toString(Tag.StudyInstanceUID), "Study Instance UID", null,
					Severity.ERROR, CheckKind.STUDY_UID_MISMATCH, "A single Study Instance UID across the batch",
					join(accumulator.studyUidsSeen())));
		}
		if (accumulator.patientIds().size() > 1) {
			findings.add(new ConformanceFinding(TagUtils.toString(Tag.PatientID), "Patient ID", null, Severity.ERROR,
					CheckKind.PATIENT_IDENTITY_MISMATCH, "A single Patient ID across all series",
					join(accumulator.patientIds())));
		}
		if (accumulator.patientNames().size() > 1) {
			findings.add(new ConformanceFinding(TagUtils.toString(Tag.PatientName), "Patient's Name", null,
					Severity.ERROR, CheckKind.PATIENT_IDENTITY_MISMATCH, "A single Patient's Name across all series",
					join(accumulator.patientNames())));
		}

		for (Map.Entry<String, SeriesData> entry : accumulator.seriesByUid().entrySet()) {
			SeriesData series = entry.getValue();
			if (series.frameOfReferenceUids.size() > 1) {
				findings.add(new ConformanceFinding(TagUtils.toString(Tag.FrameOfReferenceUID),
						"Frame of Reference UID", null, Severity.WARNING, CheckKind.FRAME_OF_REFERENCE_MISMATCH,
						"A single Frame of Reference UID within series " + entry.getKey(),
						series.frameOfReferenceUids.size() + " different UIDs"));
			}
			for (String sopClassUid : series.sopClassUids) {
				List<String> expectedModalities = rules.getSopClassToModalities().get(sopClassUid);
				if (expectedModalities != null && !series.modality.isEmpty()
						&& !expectedModalities.contains(series.modality)) {
					findings.add(new ConformanceFinding(TagUtils.toString(Tag.Modality), "Modality", null,
							Severity.WARNING, CheckKind.MODALITY_SOP_MISMATCH,
							"Modality " + join(expectedModalities) + " for SOP Class " + sopClassUid,
							"Modality " + series.modality + " in series " + entry.getKey()));
				}
			}
		}

		accumulator.transferSyntaxUids()
			.stream()
			.filter(rules.getRetiredTransferSyntaxes()::contains)
			.forEach(transferSyntaxUid -> findings.add(new ConformanceFinding(null, "Transfer Syntax", null,
					Severity.WARNING, CheckKind.RETIRED_TRANSFER_SYNTAX, "A non-retired transfer syntax",
					transferSyntaxUid + " is retired")));

		return List.copyOf(findings);
	}

	private static String join(Iterable<String> values) {
		List<String> display = new ArrayList<>();
		values.forEach(value -> display.add(value == null || value.isEmpty() ? "(empty)" : value));
		return String.join(", ", display);
	}

}
