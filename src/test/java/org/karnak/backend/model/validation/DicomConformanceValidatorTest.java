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

@DisplayNameGeneration(ReplaceUnderscores.class)
class DicomConformanceValidatorTest {

	private static DicomConformanceValidator validator;

	@BeforeAll
	static void loadStandard() {
		validator = new DicomConformanceValidator(new StandardDICOM(), CuratedValidationRules.load());
	}

	/**
	 * A CT instance carrying every Type 1/2 attribute of the mandatory CT IOD modules
	 * (patient, general-study, general-series, frame-of-reference, general-equipment,
	 * general-image, image-plane, image-pixel, ct-image, sop-common).
	 */
	private static Attributes conformantCt() {
		var dcm = new Attributes();
		dcm.setString(Tag.SOPClassUID, VR.UI, UID.CTImageStorage);
		dcm.setString(Tag.SOPInstanceUID, VR.UI, "1.2.3.4.5.1");
		// patient
		dcm.setString(Tag.PatientName, VR.PN, "PSEUDO^A");
		dcm.setString(Tag.PatientID, VR.LO, "PSEUDO-1");
		dcm.setString(Tag.PatientBirthDate, VR.DA, "19700101");
		dcm.setString(Tag.PatientSex, VR.CS, "O");
		// general-study
		dcm.setString(Tag.StudyInstanceUID, VR.UI, "1.2.3.4");
		dcm.setString(Tag.StudyDate, VR.DA, "20260101");
		dcm.setString(Tag.StudyTime, VR.TM, "101010");
		dcm.setString(Tag.AccessionNumber, VR.SH, "ACC-1");
		dcm.setString(Tag.ReferringPhysicianName, VR.PN, "");
		dcm.setString(Tag.StudyID, VR.SH, "1");
		// general-series
		dcm.setString(Tag.Modality, VR.CS, "CT");
		dcm.setString(Tag.SeriesInstanceUID, VR.UI, "1.2.3.4.5");
		dcm.setString(Tag.SeriesNumber, VR.IS, "1");
		// frame-of-reference
		dcm.setString(Tag.FrameOfReferenceUID, VR.UI, "1.2.3.4.9");
		dcm.setString(Tag.PositionReferenceIndicator, VR.LO, "");
		// general-equipment
		dcm.setString(Tag.Manufacturer, VR.LO, "Karnak");
		// general-image
		dcm.setString(Tag.InstanceNumber, VR.IS, "1");
		// image-plane
		dcm.setString(Tag.SliceThickness, VR.DS, "1.0");
		dcm.setString(Tag.ImagePositionPatient, VR.DS, "0", "0", "0");
		dcm.setString(Tag.ImageOrientationPatient, VR.DS, "1", "0", "0", "0", "1", "0");
		dcm.setString(Tag.PixelSpacing, VR.DS, "0.5", "0.5");
		// image-pixel + ct-image
		dcm.setInt(Tag.SamplesPerPixel, VR.US, 1);
		dcm.setString(Tag.PhotometricInterpretation, VR.CS, "MONOCHROME2");
		dcm.setInt(Tag.Rows, VR.US, 4);
		dcm.setInt(Tag.Columns, VR.US, 4);
		dcm.setInt(Tag.BitsAllocated, VR.US, 16);
		dcm.setInt(Tag.BitsStored, VR.US, 12);
		dcm.setInt(Tag.HighBit, VR.US, 11);
		dcm.setInt(Tag.PixelRepresentation, VR.US, 0);
		dcm.setString(Tag.ImageType, VR.CS, "ORIGINAL", "PRIMARY", "AXIAL");
		dcm.setString(Tag.KVP, VR.DS, "120");
		dcm.setString(Tag.AcquisitionNumber, VR.IS, "1");
		dcm.setString(Tag.RescaleIntercept, VR.DS, "-1024");
		dcm.setString(Tag.RescaleSlope, VR.DS, "1");
		dcm.setBytes(Tag.PixelData, VR.OW, new byte[32]);
		return dcm;
	}

	private static InstanceValidationResult validate(Attributes dcm) {
		var snapshot = MetadataSnapshot.of(dcm);
		return validator.validate(snapshot.metadata(), snapshot.bulkPresentTags(), UID.ExplicitVRLittleEndian);
	}

	private static InstanceValidationResult validateWithValueConformity(Attributes dcm) {
		var snapshot = MetadataSnapshot.of(dcm);
		return validator.validate(snapshot.metadata(), snapshot.bulkPresentTags(), UID.ExplicitVRLittleEndian, true);
	}

	private static List<ConformanceFinding> findingsOf(InstanceValidationResult result, CheckKind kind) {
		return result.findings().stream().filter(finding -> finding.kind() == kind).toList();
	}

	private static List<ConformanceFinding> errorsOf(InstanceValidationResult result) {
		return result.findings().stream().filter(finding -> finding.severity() == Severity.ERROR).toList();
	}

	@Test
	void conformant_ct_instance_has_no_error() {
		var result = validate(conformantCt());

		assertEquals(List.of(), errorsOf(result));
	}

	@Test
	void missing_type1_attribute_is_an_error() {
		var dcm = conformantCt();
		dcm.remove(Tag.Rows);

		var result = validate(dcm);

		List<ConformanceFinding> findings = findingsOf(result, CheckKind.TYPE1_MISSING);
		assertEquals(1, findings.size());
		assertEquals("(0028,0010)", findings.get(0).tagPath());
		assertEquals(Severity.ERROR, findings.get(0).severity());
	}

	@Test
	void empty_type1_attribute_is_an_error() {
		var dcm = conformantCt();
		dcm.setNull(Tag.StudyInstanceUID, VR.UI);

		var result = validate(dcm);

		List<ConformanceFinding> findings = findingsOf(result, CheckKind.TYPE1_EMPTY);
		assertEquals(1, findings.size());
		assertEquals("(0020,000D)", findings.get(0).tagPath());
	}

	@Test
	void missing_type2_attribute_is_a_warning() {
		var dcm = conformantCt();
		dcm.remove(Tag.PatientName);

		var result = validate(dcm);

		List<ConformanceFinding> findings = findingsOf(result, CheckKind.TYPE2_MISSING);
		assertEquals(1, findings.size());
		assertEquals("(0010,0010)", findings.get(0).tagPath());
		assertEquals(Severity.WARNING, findings.get(0).severity());
	}

	@Test
	void empty_type2_attribute_is_conformant() {
		// ReferringPhysicianName and PositionReferenceIndicator are already empty in the
		// conformant dataset and must not raise any finding
		var result = validate(conformantCt());

		assertEquals(List.of(), findingsOf(result, CheckKind.TYPE2_MISSING));
	}

	@Test
	void value_multiplicity_violation_is_an_error() {
		var dcm = conformantCt();
		// ImagePositionPatient requires exactly 3 values
		dcm.setString(Tag.ImagePositionPatient, VR.DS, "0", "0");

		var result = validate(dcm);

		List<ConformanceFinding> findings = findingsOf(result, CheckKind.VM_VIOLATION);
		assertEquals(1, findings.size());
		assertEquals("(0020,0032)", findings.get(0).tagPath());
		assertTrue(findings.get(0).expected().contains("3"));
	}

	@Test
	void invalid_value_of_closed_enumerated_set_is_an_error() {
		// Patient Sex is a closed Enumerated Values set (M/F/O) -> a value outside it is
		// a
		// standard violation, reported as ERROR
		var dcm = conformantCt();
		dcm.setString(Tag.PatientSex, VR.CS, "X");

		var result = validate(dcm);

		List<ConformanceFinding> findings = findingsOf(result, CheckKind.ENUMERATED_VALUE);
		assertEquals(1, findings.size());
		assertEquals("(0010,0040)", findings.get(0).tagPath());
		assertEquals("X", findings.get(0).found());
		assertEquals(Severity.ERROR, findings.get(0).severity());
	}

	@Test
	void invalid_image_laterality_value_is_flagged() {
		var dcm = conformantCt();
		dcm.setString(Tag.ImageLaterality, VR.CS, "X");

		var result = validate(dcm);

		List<ConformanceFinding> findings = findingsOf(result, CheckKind.ENUMERATED_VALUE);
		assertTrue(findings.stream().anyMatch(finding -> "(0020,0062)".equals(finding.tagPath())));
	}

	@Test
	void wrong_vr_is_an_error() {
		var dcm = conformantCt();
		dcm.remove(Tag.Rows);
		dcm.setString(Tag.Rows, VR.SH, "4");

		var result = validate(dcm);

		List<ConformanceFinding> findings = findingsOf(result, CheckKind.VR_MISMATCH);
		assertEquals(1, findings.size());
		assertEquals("VR US", findings.get(0).expected());
		assertEquals("VR SH", findings.get(0).found());
	}

	@Test
	void vr_check_is_skipped_for_implicit_vr_transfer_syntax() {
		var dcm = conformantCt();
		dcm.remove(Tag.Rows);
		dcm.setString(Tag.Rows, VR.SH, "4");
		var snapshot = MetadataSnapshot.of(dcm);

		var result = validator.validate(snapshot.metadata(), snapshot.bulkPresentTags(), UID.ImplicitVRLittleEndian);

		assertEquals(List.of(), findingsOf(result, CheckKind.VR_MISMATCH));
	}

	@Test
	void unknown_sop_class_is_a_warning_and_skips_module_checks() {
		var dcm = conformantCt();
		dcm.setString(Tag.SOPClassUID, VR.UI, "1.2.3.999.1");

		var result = validate(dcm);

		assertEquals(1, findingsOf(result, CheckKind.UNKNOWN_SOP_CLASS).size());
		assertEquals(List.of(), findingsOf(result, CheckKind.MODULE_MISSING));
	}

	@Test
	void retired_sop_class_is_flagged_as_retired_not_unknown() {
		var dcm = conformantCt();
		// Nuclear Medicine Image Storage (Retired)
		dcm.setString(Tag.SOPClassUID, VR.UI, "1.2.840.10008.5.1.4.1.1.5");

		var result = validate(dcm);

		List<ConformanceFinding> findings = findingsOf(result, CheckKind.RETIRED_SOP_CLASS);
		assertEquals(1, findings.size());
		assertEquals(Severity.WARNING, findings.get(0).severity());
		assertEquals(List.of(), findingsOf(result, CheckKind.UNKNOWN_SOP_CLASS));
		assertEquals(List.of(), findingsOf(result, CheckKind.MODULE_MISSING));
	}

	@Test
	void missing_mandatory_module_is_an_error() {
		var dcm = conformantCt();
		// Remove the whole image-plane module
		dcm.remove(Tag.SliceThickness);
		dcm.remove(Tag.ImagePositionPatient);
		dcm.remove(Tag.ImageOrientationPatient);
		dcm.remove(Tag.PixelSpacing);

		var result = validate(dcm);

		List<ConformanceFinding> findings = findingsOf(result, CheckKind.MODULE_MISSING);
		assertEquals(1, findings.size());
		assertEquals("image-plane", findings.get(0).moduleId());
	}

	@Test
	void stripped_bulk_pixel_data_is_not_flagged_as_empty() {
		// PixelData is Type 1C in the image-pixel module: present in the original
		// dataset, stripped by the snapshot, recorded in bulkPresentTags
		var snapshot = MetadataSnapshot.of(conformantCt());
		assertTrue(snapshot.bulkPresentTags().contains(Tag.PixelData));

		var result = validator.validate(snapshot.metadata(), snapshot.bulkPresentTags(), UID.ExplicitVRLittleEndian);

		assertEquals(List.of(), errorsOf(result));
	}

	@Test
	void empty_pixel_data_without_bulk_marker_is_an_error() {
		var dcm = conformantCt();
		dcm.setNull(Tag.PixelData, VR.OW);

		var result = validator.validate(dcm, Set.of(), UID.ExplicitVRLittleEndian);

		List<ConformanceFinding> findings = findingsOf(result, CheckKind.TYPE1_EMPTY);
		assertTrue(findings.stream().anyMatch(finding -> "(7FE0,0010)".equals(finding.tagPath())));
	}

	@Test
	void satisfied_type1c_condition_makes_attribute_mandatory() {
		// ct-image: Water Equivalent Diameter Calculation Method Code Sequence
		// (0018,1272) is
		// Type 1C, "Required if Water Equivalent Diameter (0018,1271) is present"
		var dcm = conformantCt();
		dcm.setString(0x00181271, VR.DS, "120.5");

		var result = validate(dcm);

		List<ConformanceFinding> findings = findingsOf(result, CheckKind.TYPE1_MISSING);
		assertTrue(findings.stream().anyMatch(finding -> "(0018,1272)".equals(finding.tagPath())));
	}

	@Test
	void unsatisfied_type1c_condition_keeps_attribute_optional() {
		// Without Water Equivalent Diameter (0018,1271) the condition is not met, so its
		// dependent attribute (0018,1272) must not be flagged as missing
		var result = validate(conformantCt());

		List<ConformanceFinding> findings = findingsOf(result, CheckKind.TYPE1_MISSING);
		assertTrue(findings.stream().noneMatch(finding -> "(0018,1272)".equals(finding.tagPath())));
	}

	@Test
	void satisfied_type2c_condition_makes_attribute_a_warning() {
		// dermoscopic-image: Contact Area (0016,1004) is Type 2C, "Required if Contact
		// Method
		// (0016,1003) is CONTACT"
		var dcm = new Attributes();
		dcm.setString(Tag.SOPClassUID, VR.UI, "1.2.840.10008.5.1.4.1.1.77.1.7");
		dcm.setString(Tag.SOPInstanceUID, VR.UI, "1.2.3.4.5.2");
		dcm.setString(0x00161003, VR.CS, "CONTACT");

		var result = validator.validate(dcm, Set.of(), UID.ExplicitVRLittleEndian);

		List<ConformanceFinding> findings = findingsOf(result, CheckKind.TYPE2_MISSING);
		assertTrue(findings.stream().anyMatch(finding -> "(0016,1004)".equals(finding.tagPath())));
	}

	@Test
	void private_data_element_without_creator_is_a_warning() {
		var dcm = conformantCt();
		// (0009,1001) is a private data element in block 0x10 of group 0009, but no
		// Private
		// Creator (0009,0010) reserves that block
		dcm.setString(0x00091001, VR.LO, "vendor value");

		var result = validate(dcm);

		List<ConformanceFinding> findings = findingsOf(result, CheckKind.PRIVATE_CREATOR_MISSING);
		assertEquals(1, findings.size());
		assertEquals("(0009,0010)", findings.get(0).tagPath());
		assertEquals(Severity.WARNING, findings.get(0).severity());
		assertEquals(List.of(), errorsOf(result));
	}

	@Test
	void private_data_element_with_its_creator_is_conformant() {
		var dcm = conformantCt();
		dcm.setString(0x00090010, VR.LO, "ACME 1.0");
		dcm.setString(0x00091001, VR.LO, "vendor value");

		var result = validate(dcm);

		assertEquals(List.of(), findingsOf(result, CheckKind.PRIVATE_CREATOR_MISSING));
		assertEquals(List.of(), findingsOf(result, CheckKind.PRIVATE_CREATOR_INVALID));
	}

	@Test
	void empty_private_creator_is_a_warning_and_not_also_an_orphan() {
		var dcm = conformantCt();
		dcm.setNull(0x00090010, VR.LO);
		dcm.setString(0x00091001, VR.LO, "vendor value");

		var result = validate(dcm);

		List<ConformanceFinding> invalid = findingsOf(result, CheckKind.PRIVATE_CREATOR_INVALID);
		assertEquals(1, invalid.size());
		assertEquals("(0009,0010)", invalid.get(0).tagPath());
		// The present-but-empty creator must not also be reported as a missing creator
		assertEquals(List.of(), findingsOf(result, CheckKind.PRIVATE_CREATOR_MISSING));
	}

	@Test
	void value_conformity_check_is_off_by_default() {
		var dcm = conformantCt();
		dcm.setString(Tag.PatientID, VR.LO, "X".repeat(80)); // exceeds LO max of 64

		var result = validate(dcm);

		assertEquals(List.of(), findingsOf(result, CheckKind.VALUE_TOO_LONG));
	}

	@Test
	void over_long_long_text_value_is_a_warning_when_value_conformity_enabled() {
		var dcm = conformantCt();
		dcm.setString(Tag.PatientID, VR.LO, "X".repeat(80)); // LO is long free text

		var result = validateWithValueConformity(dcm);

		List<ConformanceFinding> findings = findingsOf(result, CheckKind.VALUE_TOO_LONG);
		assertEquals(1, findings.size());
		assertEquals("(0010,0020)", findings.get(0).tagPath());
		assertEquals(Severity.WARNING, findings.get(0).severity());
	}

	@Test
	void over_long_small_field_value_is_an_error_when_value_conformity_enabled() {
		var dcm = conformantCt();
		dcm.setString(Tag.AccessionNumber, VR.SH, "A".repeat(20)); // SH max is 16

		var result = validateWithValueConformity(dcm);

		List<ConformanceFinding> findings = findingsOf(result, CheckKind.VALUE_TOO_LONG);
		assertEquals(1, findings.size());
		assertEquals("(0008,0050)", findings.get(0).tagPath());
		assertEquals(Severity.ERROR, findings.get(0).severity());
	}

	@Test
	void malformed_value_is_flagged_when_value_conformity_enabled() {
		var dcm = conformantCt();
		dcm.setString(Tag.StudyDate, VR.DA, "2026-01-01"); // not YYYYMMDD

		var result = validateWithValueConformity(dcm);

		List<ConformanceFinding> findings = findingsOf(result, CheckKind.VALUE_FORMAT);
		assertTrue(findings.stream().anyMatch(finding -> "(0008,0020)".equals(finding.tagPath())));
		assertTrue(findings.stream().allMatch(finding -> finding.severity() == Severity.WARNING));
	}

	@Test
	void conformant_values_produce_no_value_findings_when_enabled() {
		var result = validateWithValueConformity(conformantCt());

		assertEquals(List.of(), findingsOf(result, CheckKind.VALUE_TOO_LONG));
		assertEquals(List.of(), findingsOf(result, CheckKind.VALUE_FORMAT));
	}

	@Test
	void retired_attribute_is_an_info() {
		var dcm = conformantCt();
		// (0008,0001) Length to End is retired
		dcm.setString(0x00080001, VR.UL, "0");

		var result = validate(dcm);

		List<ConformanceFinding> findings = findingsOf(result, CheckKind.RETIRED_ATTRIBUTE);
		assertEquals(1, findings.size());
		assertEquals(Severity.INFO, findings.get(0).severity());
	}

	/**
	 * An SR-like dataset whose content tree carries a problem two sequence levels deep:
	 * ContentSequence (0040,A730) → item → ContentSequence → item → a retired attribute.
	 */
	private static Attributes srWithDeeplyNestedRetiredAttribute() {
		var dcm = new Attributes();
		dcm.setString(Tag.SOPClassUID, VR.UI, UID.BasicTextSRStorage);
		dcm.setString(Tag.SOPInstanceUID, VR.UI, "1.2.3.4.5.1");
		var level2 = new Attributes();
		// (0008,0001) Length to End is retired — sits at depth 2
		level2.setString(0x00080001, VR.UL, "0");
		var level1 = new Attributes();
		level1.newSequence(Tag.ContentSequence, 1).add(level2);
		dcm.newSequence(Tag.ContentSequence, 1).add(level1);
		return dcm;
	}

	@Test
	void deeply_nested_finding_is_skipped_at_the_default_recursion_depth() {
		var result = validate(srWithDeeplyNestedRetiredAttribute());

		assertEquals(List.of(), findingsOf(result, CheckKind.RETIRED_ATTRIBUTE));
	}

	@Test
	void deeply_nested_finding_is_caught_when_recursion_depth_is_raised() {
		var dcm = srWithDeeplyNestedRetiredAttribute();
		var snapshot = MetadataSnapshot.of(dcm, 8);

		var result = validator.validate(snapshot.metadata(), snapshot.bulkPresentTags(), UID.ExplicitVRLittleEndian,
				false, 8);

		List<ConformanceFinding> findings = findingsOf(result, CheckKind.RETIRED_ATTRIBUTE);
		assertEquals(1, findings.size());
		assertEquals("(0040,A730) > (0040,A730) > (0008,0001)", findings.get(0).tagPath());
		assertEquals(Severity.INFO, findings.get(0).severity());
	}

}
