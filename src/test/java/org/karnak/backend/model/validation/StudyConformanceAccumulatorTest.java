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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class StudyConformanceAccumulatorTest {

	private static final Instant NOW = Instant.parse("2026-06-12T10:00:00Z");

	private static final StudyKey KEY = new StudyKey(1L, 2L, "1.2.3.4");

	private static CuratedValidationRules rules;

	@BeforeAll
	static void loadRules() {
		rules = CuratedValidationRules.load();
	}

	private static InstanceConformanceData instance(String seriesUid, String sopInstanceUid, String patientId,
			String modality, String transferSyntaxUid, String frameOfReferenceUid) {
		var dcm = new Attributes();
		dcm.setString(Tag.SOPClassUID, VR.UI, UID.CTImageStorage);
		dcm.setString(Tag.SOPInstanceUID, VR.UI, sopInstanceUid);
		dcm.setString(Tag.StudyInstanceUID, VR.UI, KEY.studyInstanceUid());
		dcm.setString(Tag.SeriesInstanceUID, VR.UI, seriesUid);
		dcm.setString(Tag.PatientID, VR.LO, patientId);
		dcm.setString(Tag.PatientName, VR.PN, "PSEUDO^A");
		dcm.setString(Tag.Modality, VR.CS, modality);
		dcm.setString(Tag.FrameOfReferenceUID, VR.UI, frameOfReferenceUid);
		return InstanceConformanceData.of(KEY.forwardNodeId(), KEY.destinationId(), "SRC_AET", transferSyntaxUid, true,
				null, false, false, true, MetadataSnapshot.of(dcm));
	}

	private static InstanceConformanceData ctInstance(String sopInstanceUid) {
		return instance("1.2.3.4.5", sopInstanceUid, "PSEUDO-1", "CT", UID.ExplicitVRLittleEndian, "1.2.3.4.9");
	}

	private static InstanceValidationResult resultWith(ConformanceFinding... findings) {
		return new InstanceValidationResult(UID.CTImageStorage, "1.2.3.4.5.1", List.of(findings));
	}

	private static ConformanceFinding type2Warning() {
		return new ConformanceFinding("(0010,0030)", "Patient's Birth Date", "patient", Severity.WARNING,
				CheckKind.TYPE2_MISSING, "Type 2: present, possibly empty", "Attribute is missing");
	}

	@Test
	void identical_findings_across_instances_are_deduplicated_with_a_count() {
		var accumulator = new StudyConformanceAccumulator(KEY, "SRC_AET", true, rules, NOW);
		for (int i = 1; i <= 3; i++) {
			accumulator.add(ctInstance("1.2.3.4.5." + i), resultWith(type2Warning()), NOW);
		}

		var report = accumulator.close();

		var summaries = report.findingsBySopClass().get(UID.CTImageStorage);
		assertEquals(1, summaries.size());
		assertEquals(3, summaries.get(0).count());
		assertEquals(type2Warning(), summaries.get(0).finding());
		assertEquals(3, report.warningCount());
		assertEquals(0, report.errorCount());
		assertTrue(report.passed());
	}

	@Test
	void retried_instance_is_counted_once() {
		var accumulator = new StudyConformanceAccumulator(KEY, "SRC_AET", true, rules, NOW);
		accumulator.add(ctInstance("1.2.3.4.5.1"), resultWith(), NOW);
		accumulator.add(ctInstance("1.2.3.4.5.1"), resultWith(), NOW);

		var report = accumulator.close();

		assertEquals(1, report.instanceCount());
		assertEquals(1, report.seriesCount());
	}

	@Test
	void failed_instances_are_counted_with_their_reasons() {
		var accumulator = new StudyConformanceAccumulator(KEY, "SRC_AET", true, rules, NOW);
		accumulator.add(ctInstance("1.2.3.4.5.1"), resultWith(), NOW);
		var dcm = new Attributes();
		dcm.setString(Tag.SOPClassUID, VR.UI, UID.CTImageStorage);
		dcm.setString(Tag.StudyInstanceUID, VR.UI, KEY.studyInstanceUid());
		dcm.setString(Tag.SeriesInstanceUID, VR.UI, "1.2.3.4.5");
		dcm.setString(Tag.SOPInstanceUID, VR.UI, "1.2.3.4.5.2");
		var failed = InstanceConformanceData.of(KEY.forwardNodeId(), KEY.destinationId(), "SRC_AET",
				UID.ExplicitVRLittleEndian, false, "Connection refused", false, false, true, MetadataSnapshot.of(dcm));
		accumulator.add(failed, null, NOW);

		var report = accumulator.close();

		assertEquals(1, report.failedInstanceCount());
		assertEquals(List.of("Connection refused"), report.failureReasons());
	}

	@Test
	void add_is_rejected_after_close() {
		var accumulator = new StudyConformanceAccumulator(KEY, "SRC_AET", true, rules, NOW);
		accumulator.add(ctInstance("1.2.3.4.5.1"), resultWith(), NOW);
		accumulator.close();

		assertFalse(accumulator.add(ctInstance("1.2.3.4.5.2"), resultWith(), NOW));
	}

	@Test
	void report_header_uses_first_non_empty_study_values() {
		var accumulator = new StudyConformanceAccumulator(KEY, "SRC_AET", true, rules, NOW);
		accumulator.add(ctInstance("1.2.3.4.5.1"), resultWith(), NOW);

		var report = accumulator.close();

		assertEquals("PSEUDO-1", report.patientId());
		assertEquals("PSEUDO^A", report.patientName());
		assertEquals("SRC_AET", report.sourceAet());
		assertEquals(KEY, report.key());
	}

	@Test
	void patient_name_is_redacted_when_destination_is_not_de_identified() {
		var accumulator = new StudyConformanceAccumulator(KEY, "SRC_AET", false, rules, NOW);
		var dcm = new Attributes();
		dcm.setString(Tag.SOPClassUID, VR.UI, UID.CTImageStorage);
		dcm.setString(Tag.SOPInstanceUID, VR.UI, "1.2.3.4.5.1");
		dcm.setString(Tag.StudyInstanceUID, VR.UI, KEY.studyInstanceUid());
		dcm.setString(Tag.SeriesInstanceUID, VR.UI, "1.2.3.4.5");
		dcm.setString(Tag.PatientID, VR.LO, "REAL-ID");
		dcm.setString(Tag.PatientName, VR.PN, "DOE^JOHN");
		var data = InstanceConformanceData.of(KEY.forwardNodeId(), KEY.destinationId(), "SRC_AET",
				UID.ExplicitVRLittleEndian, true, null, false, false, false, MetadataSnapshot.of(dcm));
		accumulator.add(data, resultWith(), NOW);

		var report = accumulator.close();

		assertFalse(report.deidentified());
		// Patient Name is dropped, other identifiers (Patient ID) are kept
		assertEquals("", report.patientName());
		assertEquals("REAL-ID", report.patientId());
		// The real name leaks into no finding either
		assertTrue(report.consistencyFindings().stream().noneMatch(finding -> "DOE^JOHN".equals(finding.found())));
	}

	@Test
	void inconsistent_patient_id_across_series_is_an_error() {
		var accumulator = new StudyConformanceAccumulator(KEY, "SRC_AET", true, rules, NOW);
		accumulator.add(instance("1.2.3.4.5", "1.2.3.4.5.1", "PSEUDO-1", "CT", UID.ExplicitVRLittleEndian, "1.2.3.4.9"),
				resultWith(), NOW);
		accumulator.add(instance("1.2.3.4.6", "1.2.3.4.6.1", "PSEUDO-2", "CT", UID.ExplicitVRLittleEndian, "1.2.3.4.9"),
				resultWith(), NOW);

		var report = accumulator.close();

		assertTrue(report.consistencyFindings()
			.stream()
			.anyMatch(finding -> finding.kind() == CheckKind.PATIENT_IDENTITY_MISMATCH
					&& finding.severity() == Severity.ERROR));
		assertFalse(report.passed());
	}

	@Test
	void multiple_frame_of_reference_uids_in_one_series_is_a_warning() {
		var accumulator = new StudyConformanceAccumulator(KEY, "SRC_AET", true, rules, NOW);
		accumulator.add(instance("1.2.3.4.5", "1.2.3.4.5.1", "PSEUDO-1", "CT", UID.ExplicitVRLittleEndian, "1.2.3.4.9"),
				resultWith(), NOW);
		accumulator.add(instance("1.2.3.4.5", "1.2.3.4.5.2", "PSEUDO-1", "CT", UID.ExplicitVRLittleEndian, "1.2.3.4.8"),
				resultWith(), NOW);

		var report = accumulator.close();

		assertTrue(report.consistencyFindings()
			.stream()
			.anyMatch(finding -> finding.kind() == CheckKind.FRAME_OF_REFERENCE_MISMATCH));
	}

	@Test
	void modality_inconsistent_with_sop_class_is_a_warning() {
		var accumulator = new StudyConformanceAccumulator(KEY, "SRC_AET", true, rules, NOW);
		accumulator.add(instance("1.2.3.4.5", "1.2.3.4.5.1", "PSEUDO-1", "MR", UID.ExplicitVRLittleEndian, "1.2.3.4.9"),
				resultWith(), NOW);

		var report = accumulator.close();

		assertTrue(report.consistencyFindings()
			.stream()
			.anyMatch(finding -> finding.kind() == CheckKind.MODALITY_SOP_MISMATCH));
	}

	@Test
	void retired_transfer_syntax_is_a_warning() {
		var accumulator = new StudyConformanceAccumulator(KEY, "SRC_AET", true, rules, NOW);
		accumulator.add(instance("1.2.3.4.5", "1.2.3.4.5.1", "PSEUDO-1", "CT", UID.ExplicitVRBigEndian, "1.2.3.4.9"),
				resultWith(), NOW);

		var report = accumulator.close();

		assertTrue(report.consistencyFindings()
			.stream()
			.anyMatch(finding -> finding.kind() == CheckKind.RETIRED_TRANSFER_SYNTAX));
	}

	@Test
	void consistent_study_has_no_consistency_finding() {
		var accumulator = new StudyConformanceAccumulator(KEY, "SRC_AET", true, rules, NOW);
		accumulator.add(ctInstance("1.2.3.4.5.1"), resultWith(), NOW);
		accumulator.add(ctInstance("1.2.3.4.5.2"), resultWith(), NOW);

		var report = accumulator.close();

		assertEquals(List.of(), report.consistencyFindings());
		assertTrue(report.passed());
	}

}
