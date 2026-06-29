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

import java.util.List;
import java.util.Set;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.model.standard.StandardDICOM;

/**
 * Covers the cross-attribute semantic checks added on top of the IOD-driven validation:
 * pixel-geometry coherence ({@link CheckKind#PIXEL_GEOMETRY}) and residual direct
 * identifiers ({@link CheckKind#PRIVACY_RISK}).
 */
@DisplayNameGeneration(ReplaceUnderscores.class)
class SemanticConformanceChecksTest {

	private static DicomConformanceValidator validator;

	@BeforeAll
	static void loadStandard() {
		validator = new DicomConformanceValidator(new StandardDICOM(), CuratedValidationRules.load());
	}

	private static Attributes minimalCt() {
		var dcm = new Attributes();
		dcm.setString(Tag.SOPClassUID, VR.UI, UID.CTImageStorage);
		dcm.setString(Tag.SOPInstanceUID, VR.UI, "1.2.3.4.5.1");
		dcm.setString(Tag.Modality, VR.CS, "CT");
		return dcm;
	}

	private static boolean hasKind(List<ConformanceFinding> findings, CheckKind kind) {
		return findings.stream().anyMatch(f -> f.kind() == kind);
	}

	private static ConformanceFinding firstOf(List<ConformanceFinding> findings, CheckKind kind) {
		return findings.stream().filter(f -> f.kind() == kind).findFirst().orElseThrow();
	}

	private List<ConformanceFinding> validate(Attributes dcm) {
		return validator.validate(dcm, Set.of(), UID.ExplicitVRLittleEndian).findings();
	}

	/**
	 * Validates with deep-sequence recursion enabled (mirrors the deepSequenceValidation
	 * option).
	 */
	private List<ConformanceFinding> validateDeep(Attributes dcm) {
		return validator.validate(dcm, Set.of(), UID.ExplicitVRLittleEndian, false, 8).findings();
	}

	@Test
	void one_to_one_pixel_aspect_ratio_is_an_error() {
		var dcm = minimalCt();
		dcm.setInt(Tag.PixelAspectRatio, VR.IS, 1, 1);

		assertEquals(Severity.ERROR, firstOf(validate(dcm), CheckKind.PIXEL_GEOMETRY).severity());
	}

	@Test
	void pixel_spacing_differing_from_imager_spacing_without_calibration_type_is_flagged() {
		var dcm = minimalCt();
		dcm.setDouble(Tag.PixelSpacing, VR.DS, 0.4, 0.4);
		dcm.setDouble(Tag.ImagerPixelSpacing, VR.DS, 0.5, 0.5);

		assertTrue(hasKind(validate(dcm), CheckKind.PIXEL_GEOMETRY));
	}

	@Test
	void pixel_spacing_differing_from_imager_spacing_with_calibration_type_is_clean() {
		var dcm = minimalCt();
		dcm.setDouble(Tag.PixelSpacing, VR.DS, 0.4, 0.4);
		dcm.setDouble(Tag.ImagerPixelSpacing, VR.DS, 0.5, 0.5);
		dcm.setString(Tag.PixelSpacingCalibrationType, VR.CS, "GEOMETRY");

		assertFalse(hasKind(validate(dcm), CheckKind.PIXEL_GEOMETRY));
	}

	@Test
	void plain_pixel_spacing_matching_imager_spacing_is_clean() {
		var dcm = minimalCt();
		dcm.setDouble(Tag.PixelSpacing, VR.DS, 0.5, 0.5);
		dcm.setDouble(Tag.ImagerPixelSpacing, VR.DS, 0.5, 0.5);

		assertFalse(hasKind(validate(dcm), CheckKind.PIXEL_GEOMETRY));
	}

	@Test
	void zero_pixel_spacing_is_an_error() {
		var dcm = minimalCt();
		dcm.setDouble(Tag.PixelSpacing, VR.DS, 0.0, 0.5);

		assertEquals(Severity.ERROR, firstOf(validate(dcm), CheckKind.IMPLAUSIBLE_VALUE).severity());
	}

	@Test
	void residual_patient_telephone_number_is_flagged() {
		var dcm = minimalCt();
		dcm.setString(Tag.PatientTelephoneNumbers, VR.SH, "555-0100");

		assertTrue(hasKind(validate(dcm), CheckKind.PRIVACY_RISK));
	}

	@Test
	void instance_without_direct_identifiers_has_no_privacy_finding() {
		assertFalse(hasKind(validate(minimalCt()), CheckKind.PRIVACY_RISK));
	}

	@Test
	void paired_body_part_without_laterality_is_flagged() {
		var dcm = minimalCt();
		dcm.setString(Tag.BodyPartExamined, VR.CS, "BREAST");

		assertTrue(hasKind(validate(dcm), CheckKind.LATERALITY));
	}

	@Test
	void paired_body_part_with_laterality_is_clean() {
		var dcm = minimalCt();
		dcm.setString(Tag.BodyPartExamined, VR.CS, "BREAST");
		dcm.setString(Tag.ImageLaterality, VR.CS, "L");

		assertFalse(hasKind(validate(dcm), CheckKind.LATERALITY));
	}

	@Test
	void unpaired_body_part_without_laterality_is_not_flagged() {
		var dcm = minimalCt();
		dcm.setString(Tag.BodyPartExamined, VR.CS, "CHEST");

		assertFalse(hasKind(validate(dcm), CheckKind.LATERALITY));
	}

	@Test
	void absent_body_part_is_not_treated_as_paired() {
		// Deliberate divergence from dciodvfy: no Body Part Examined means no laterality
		// expectation, to avoid flagging the many images that carry neither
		assertFalse(hasKind(validate(minimalCt()), CheckKind.LATERALITY));
	}

	@Test
	void zero_kvp_is_an_implausible_warning() {
		var dcm = minimalCt();
		dcm.setString(Tag.KVP, VR.DS, "0");

		assertEquals(Severity.WARNING, firstOf(validate(dcm), CheckKind.IMPLAUSIBLE_VALUE).severity());
	}

	@Test
	void positive_kvp_is_clean() {
		var dcm = minimalCt();
		dcm.setString(Tag.KVP, VR.DS, "120");

		assertFalse(hasKind(validate(dcm), CheckKind.IMPLAUSIBLE_VALUE));
	}

	@Test
	void zero_rows_is_an_error() {
		var dcm = minimalCt();
		dcm.setInt(Tag.Rows, VR.US, 0);

		assertEquals(Severity.ERROR, firstOf(validate(dcm), CheckKind.IMPLAUSIBLE_VALUE).severity());
	}

	@Test
	void invalid_characters_in_code_value_for_its_scheme_are_flagged() {
		var dcm = minimalCt();
		var item = new Attributes();
		// Lowercase / space are not permitted in a DCM code value (only A-Z, 0-9)
		item.setString(Tag.CodeValue, VR.SH, "bad value");
		item.setString(Tag.CodingSchemeDesignator, VR.SH, "DCM");
		item.setString(Tag.CodeMeaning, VR.LO, "Whatever");
		dcm.newSequence(Tag.AnatomicRegionSequence, 1).add(item);

		assertTrue(hasKind(validate(dcm), CheckKind.CODE_VALUE_INVALID));
	}

	@Test
	void snomed_code_value_with_hyphen_is_clean() {
		var dcm = minimalCt();
		var item = new Attributes();
		// A hyphen is permitted in a SNOMED-RT style code value
		item.setString(Tag.CodeValue, VR.SH, "T-04000");
		item.setString(Tag.CodingSchemeDesignator, VR.SH, "SRT");
		item.setString(Tag.CodeMeaning, VR.LO, "Breast");
		dcm.newSequence(Tag.AnatomicRegionSequence, 1).add(item);

		assertFalse(hasKind(validate(dcm), CheckKind.CODE_VALUE_INVALID));
	}

	@Test
	void unknown_concept_code_is_flagged() {
		var dcm = minimalCt();
		var item = new Attributes();
		item.setString(Tag.CodeValue, VR.SH, "261665006");
		item.setString(Tag.CodingSchemeDesignator, VR.SH, "SCT");
		item.setString(Tag.CodeMeaning, VR.LO, "Unknown");
		dcm.newSequence(Tag.AnatomicRegionSequence, 1).add(item);

		assertTrue(hasKind(validate(dcm), CheckKind.CODE_UNKNOWN_CONCEPT));
	}

	@Test
	void specific_concept_code_is_clean() {
		var dcm = minimalCt();
		var item = new Attributes();
		item.setString(Tag.CodeValue, VR.SH, "76752008");
		item.setString(Tag.CodingSchemeDesignator, VR.SH, "SCT");
		item.setString(Tag.CodeMeaning, VR.LO, "Breast");
		dcm.newSequence(Tag.AnatomicRegionSequence, 1).add(item);

		assertFalse(hasKind(validate(dcm), CheckKind.CODE_VALUE_INVALID));
		assertFalse(hasKind(validate(dcm), CheckKind.CODE_UNKNOWN_CONCEPT));
	}

	@Test
	void standard_attribute_outside_the_iod_is_flagged_as_non_standard() {
		var dcm = minimalCt();
		// Echo Time is an MR attribute, not part of the CT image IOD
		dcm.setString(Tag.EchoTime, VR.DS, "12.0");

		assertTrue(hasKind(validate(dcm), CheckKind.NON_STANDARD_ATTRIBUTE));
	}

	@Test
	void frame_of_reference_uid_reused_as_series_uid_is_an_error() {
		var dcm = minimalCt();
		dcm.setString(Tag.SeriesInstanceUID, VR.UI, "1.2.3.4.5");
		dcm.setString(Tag.FrameOfReferenceUID, VR.UI, "1.2.3.4.5");

		assertEquals(Severity.ERROR, firstOf(validate(dcm), CheckKind.UID_REUSE).severity());
	}

	@Test
	void distinct_entity_uids_are_clean() {
		var dcm = minimalCt();
		dcm.setString(Tag.SeriesInstanceUID, VR.UI, "1.2.3.4.5");
		dcm.setString(Tag.StudyInstanceUID, VR.UI, "1.2.3.4");
		dcm.setString(Tag.FrameOfReferenceUID, VR.UI, "1.2.3.4.9");

		assertFalse(hasKind(validate(dcm), CheckKind.UID_REUSE));
	}

	@Test
	void non_unit_image_orientation_vector_is_an_error() {
		var dcm = minimalCt();
		dcm.setDouble(Tag.ImageOrientationPatient, VR.DS, 2, 0, 0, 0, 1, 0);

		assertEquals(Severity.ERROR, firstOf(validate(dcm), CheckKind.IMAGE_ORIENTATION).severity());
	}

	@Test
	void non_orthogonal_image_orientation_is_an_error() {
		var dcm = minimalCt();
		dcm.setDouble(Tag.ImageOrientationPatient, VR.DS, 1, 0, 0, 1, 0, 0);

		assertTrue(hasKind(validate(dcm), CheckKind.IMAGE_ORIENTATION));
	}

	@Test
	void unit_orthogonal_image_orientation_is_clean() {
		var dcm = minimalCt();
		dcm.setDouble(Tag.ImageOrientationPatient, VR.DS, 1, 0, 0, 0, 1, 0);

		assertFalse(hasKind(validate(dcm), CheckKind.IMAGE_ORIENTATION));
	}

	@Test
	void negative_spacing_between_slices_is_an_error() {
		var dcm = minimalCt();
		dcm.setDouble(Tag.SpacingBetweenSlices, VR.DS, -1.0);

		assertEquals(Severity.ERROR, firstOf(validate(dcm), CheckKind.IMPLAUSIBLE_VALUE).severity());
	}

	@Test
	void negative_spacing_between_slices_is_allowed_for_nuclear_medicine() {
		var dcm = minimalCt();
		dcm.setString(Tag.SOPClassUID, VR.UI, UID.NuclearMedicineImageStorage);
		dcm.setDouble(Tag.SpacingBetweenSlices, VR.DS, -1.0);

		assertFalse(hasKind(validate(dcm), CheckKind.IMPLAUSIBLE_VALUE));
	}

	@Test
	void illegal_patient_orientation_character_is_flagged() {
		var dcm = minimalCt();
		dcm.setString(Tag.PatientOrientation, VR.CS, "X", "L");

		assertTrue(hasKind(validate(dcm), CheckKind.PATIENT_ORIENTATION));
	}

	@Test
	void conflicting_patient_orientation_directions_are_flagged() {
		var dcm = minimalCt();
		dcm.setString(Tag.PatientOrientation, VR.CS, "AP", "L");

		assertTrue(hasKind(validate(dcm), CheckKind.PATIENT_ORIENTATION));
	}

	@Test
	void identical_row_and_column_patient_orientation_is_flagged() {
		var dcm = minimalCt();
		dcm.setString(Tag.PatientOrientation, VR.CS, "L", "L");

		assertTrue(hasKind(validate(dcm), CheckKind.PATIENT_ORIENTATION));
	}

	@Test
	void valid_patient_orientation_is_clean() {
		var dcm = minimalCt();
		dcm.setString(Tag.PatientOrientation, VR.CS, "A", "L");

		assertFalse(hasKind(validate(dcm), CheckKind.PATIENT_ORIENTATION));
	}

	// --- Tier 2: enhanced multi-frame and segmentation (deep-sequence) ---

	@Test
	void per_frame_functional_group_count_mismatch_is_flagged() {
		var dcm = minimalCt();
		dcm.setInt(Tag.NumberOfFrames, VR.IS, 3);
		var perFrame = dcm.newSequence(Tag.PerFrameFunctionalGroupsSequence, 2);
		perFrame.add(new Attributes());
		perFrame.add(new Attributes());

		assertTrue(hasKind(validate(dcm), CheckKind.MULTIFRAME));
	}

	@Test
	void per_frame_functional_group_count_matching_number_of_frames_is_clean() {
		var dcm = minimalCt();
		dcm.setInt(Tag.NumberOfFrames, VR.IS, 2);
		var perFrame = dcm.newSequence(Tag.PerFrameFunctionalGroupsSequence, 2);
		perFrame.add(new Attributes());
		perFrame.add(new Attributes());

		assertFalse(hasKind(validate(dcm), CheckKind.MULTIFRAME));
	}

	@Test
	void non_monotonic_segment_numbers_are_flagged() {
		var dcm = minimalCt();
		var segments = dcm.newSequence(Tag.SegmentSequence, 2);
		var first = new Attributes();
		first.setInt(Tag.SegmentNumber, VR.US, 1);
		var second = new Attributes();
		second.setInt(Tag.SegmentNumber, VR.US, 3);
		segments.add(first);
		segments.add(second);

		assertTrue(hasKind(validate(dcm), CheckKind.SEGMENTATION));
	}

	@Test
	void monotonic_segment_numbers_are_clean() {
		var dcm = minimalCt();
		var segments = dcm.newSequence(Tag.SegmentSequence, 2);
		var first = new Attributes();
		first.setInt(Tag.SegmentNumber, VR.US, 1);
		var second = new Attributes();
		second.setInt(Tag.SegmentNumber, VR.US, 2);
		segments.add(first);
		segments.add(second);

		assertFalse(hasKind(validate(dcm), CheckKind.SEGMENTATION));
	}

	@Test
	void labelmap_segmentation_is_exempt_from_segment_numbering() {
		var dcm = minimalCt();
		dcm.setString(Tag.SegmentationType, VR.CS, "LABELMAP");
		var segments = dcm.newSequence(Tag.SegmentSequence, 1);
		var only = new Attributes();
		only.setInt(Tag.SegmentNumber, VR.US, 7);
		segments.add(only);

		assertFalse(hasKind(validate(dcm), CheckKind.SEGMENTATION));
	}

	@Test
	void dimension_index_value_count_mismatch_is_flagged_only_in_deep_mode() {
		var dcm = minimalCt();
		var dimensions = dcm.newSequence(Tag.DimensionIndexSequence, 2);
		dimensions.add(new Attributes());
		dimensions.add(new Attributes());
		var perFrame = dcm.newSequence(Tag.PerFrameFunctionalGroupsSequence, 1);
		var frame = new Attributes();
		var frameContent = frame.newSequence(Tag.FrameContentSequence, 1);
		var content = new Attributes();
		content.setInt(Tag.DimensionIndexValues, VR.UL, 1); // one value, two dimensions
															// expected
		frameContent.add(content);
		perFrame.add(frame);

		assertFalse(hasKind(validate(dcm), CheckKind.MULTIFRAME)); // skipped at default
																	// depth
		assertTrue(hasKind(validateDeep(dcm), CheckKind.MULTIFRAME));
	}

	@Test
	void functional_group_present_in_both_shared_and_per_frame_is_flagged_in_deep_mode() {
		var dcm = minimalCt();
		var shared = dcm.newSequence(Tag.SharedFunctionalGroupsSequence, 1);
		var sharedItem = new Attributes();
		sharedItem.newSequence(Tag.PixelMeasuresSequence, 1).add(new Attributes());
		shared.add(sharedItem);
		var perFrame = dcm.newSequence(Tag.PerFrameFunctionalGroupsSequence, 1);
		var frame = new Attributes();
		frame.newSequence(Tag.PixelMeasuresSequence, 1).add(new Attributes());
		perFrame.add(frame);

		assertFalse(hasKind(validate(dcm), CheckKind.MULTIFRAME)); // skipped at default
																	// depth
		assertTrue(hasKind(validateDeep(dcm), CheckKind.MULTIFRAME));
	}

}
