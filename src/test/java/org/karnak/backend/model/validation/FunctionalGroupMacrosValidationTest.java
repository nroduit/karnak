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
import org.dcm4che3.util.TagUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.model.standard.StandardDICOM;

/**
 * Functional Group Macros of enhanced multi-frame IODs: a macro is carried either in the
 * Shared or in every Per-frame Functional Groups item, and its usage (M/C/U) in the IOD
 * decides whether it is mandatory.
 */
@DisplayNameGeneration(ReplaceUnderscores.class)
class FunctionalGroupMacrosValidationTest {

	private static final int FRAMES = 2;

	private static DicomConformanceValidator validator;

	@BeforeAll
	static void loadStandard() {
		validator = new DicomConformanceValidator(new StandardDICOM(), CuratedValidationRules.load());
	}

	/**
	 * Breast Tomosynthesis laid out like the Hologic instance reported by HUG: shared
	 * Derivation Image, Frame Anatomy, Plane Orientation, Frame VOI LUT and Pixel Value
	 * Transformation; per-frame X-Ray 3D Frame Type, Plane Position and Pixel Measures;
	 * no Frame Content.
	 */
	private static Attributes hologicTomosynthesis() {
		var dcm = new Attributes();
		dcm.setString(Tag.SOPClassUID, VR.UI, UID.BreastTomosynthesisImageStorage);
		dcm.setString(Tag.SOPInstanceUID, VR.UI, "1.2.3.4.5.1");
		dcm.setString(Tag.Modality, VR.CS, "MG");
		dcm.setString(Tag.ImageType, VR.CS, "DERIVED", "PRIMARY", "TOMOSYNTHESIS", "NONE");
		dcm.setInt(Tag.NumberOfFrames, VR.IS, FRAMES);
		var shared = new Attributes();
		addItem(shared, Tag.DerivationImageSequence);
		addItem(shared, Tag.FrameAnatomySequence);
		addItem(shared, Tag.PlaneOrientationSequence);
		addItem(shared, Tag.FrameVOILUTSequence);
		addItem(shared, Tag.PixelValueTransformationSequence);
		dcm.newSequence(Tag.SharedFunctionalGroupsSequence, 1).add(shared);
		var perFrame = dcm.newSequence(Tag.PerFrameFunctionalGroupsSequence, FRAMES);
		for (int i = 0; i < FRAMES; i++) {
			var frame = new Attributes();
			addItem(frame, Tag.XRay3DFrameTypeSequence);
			addItem(frame, Tag.PlanePositionSequence);
			addItem(frame, Tag.PixelMeasuresSequence);
			perFrame.add(frame);
		}
		return dcm;
	}

	private static void addItem(Attributes functionalGroup, int sequenceTag) {
		var item = new Attributes();
		item.setString(Tag.ContentLabel, VR.CS, "ITEM");
		functionalGroup.newSequence(sequenceTag, 1).add(item);
	}

	private static Attributes shared(Attributes dcm) {
		return dcm.getNestedDataset(Tag.SharedFunctionalGroupsSequence);
	}

	private static List<Attributes> perFrame(Attributes dcm) {
		return dcm.getSequence(Tag.PerFrameFunctionalGroupsSequence);
	}

	private static void addFrameContent(Attributes dcm) {
		perFrame(dcm).forEach(frame -> addItem(frame, Tag.FrameContentSequence));
	}

	/** Findings located in the functional groups sequences. */
	private static List<ConformanceFinding> functionalGroupFindings(Attributes dcm) {
		return validator.validate(dcm, Set.of(), UID.ExplicitVRLittleEndian)
			.findings()
			.stream()
			.filter(f -> f.tagPath() != null
					&& (f.tagPath().contains(TagUtils.toString(Tag.SharedFunctionalGroupsSequence))
							|| f.tagPath().contains(TagUtils.toString(Tag.PerFrameFunctionalGroupsSequence))))
			.toList();
	}

	private static boolean hasFinding(List<ConformanceFinding> findings, CheckKind kind, String tagPath) {
		return findings.stream().anyMatch(f -> f.kind() == kind && f.tagPath().equals(tagPath));
	}

	@Test
	void hologic_layout_only_reports_the_missing_frame_content() {
		var findings = functionalGroupFindings(hologicTomosynthesis());

		assertEquals(1, findings.size(), findings::toString);
		assertTrue(hasFinding(findings, CheckKind.TYPE1_MISSING, "(5200,9230) > (0020,9111)"), findings::toString);
		assertEquals(Severity.ERROR, findings.getFirst().severity());
	}

	@Test
	void macros_split_between_shared_and_per_frame_are_conformant() {
		var dcm = hologicTomosynthesis();
		addFrameContent(dcm);

		assertTrue(functionalGroupFindings(dcm).isEmpty());
	}

	@Test
	void frame_content_in_the_shared_functional_groups_is_flagged() {
		var dcm = hologicTomosynthesis();
		addItem(shared(dcm), Tag.FrameContentSequence);

		var findings = functionalGroupFindings(dcm);
		assertTrue(hasFinding(findings, CheckKind.MULTIFRAME, "(5200,9229) > (0020,9111)"), findings::toString);
	}

	@Test
	void mandatory_macro_absent_from_both_sequences_is_reported_once() {
		var dcm = hologicTomosynthesis();
		addFrameContent(dcm);
		shared(dcm).remove(Tag.FrameAnatomySequence);

		var findings = functionalGroupFindings(dcm);
		assertEquals(1, findings.size(), findings::toString);
		assertTrue(hasFinding(findings, CheckKind.TYPE1_MISSING, "(5200,9229) / (5200,9230) > (0020,9071)"),
				findings::toString);
	}

	@Test
	void per_frame_macro_missing_in_some_frames_is_flagged() {
		var dcm = hologicTomosynthesis();
		addFrameContent(dcm);
		perFrame(dcm).getLast().remove(Tag.PixelMeasuresSequence);

		var findings = functionalGroupFindings(dcm);
		assertTrue(hasFinding(findings, CheckKind.MULTIFRAME, "(5200,9230) > (0028,9110)"), findings::toString);
	}

	@Test
	void conditional_derivation_image_follows_image_type_value_1() {
		var dcm = hologicTomosynthesis();
		addFrameContent(dcm);
		shared(dcm).remove(Tag.DerivationImageSequence);

		assertTrue(hasFinding(functionalGroupFindings(dcm), CheckKind.TYPE2_MISSING,
				"(5200,9229) / (5200,9230) > (0008,9124)"));

		dcm.setString(Tag.ImageType, VR.CS, "ORIGINAL", "PRIMARY", "TOMOSYNTHESIS", "NONE");
		assertTrue(functionalGroupFindings(dcm).isEmpty());
	}

	@Test
	void contrast_bolus_usage_is_required_only_with_a_contrast_agent() {
		var dcm = hologicTomosynthesis();
		addFrameContent(dcm);
		assertTrue(functionalGroupFindings(dcm).isEmpty());

		var agent = new Attributes();
		agent.setString(Tag.CodeValue, VR.SH, "C-B0322");
		dcm.newSequence(Tag.ContrastBolusAgentSequence, 1).add(agent);

		assertTrue(hasFinding(functionalGroupFindings(dcm), CheckKind.TYPE1_MISSING,
				"(5200,9229) / (5200,9230) > (0018,9341)"));
	}

	@Test
	void user_optional_macros_are_not_required() {
		var findings = functionalGroupFindings(hologicTomosynthesis());

		// Real World Value Mapping and Referenced Image have usage U
		assertFalse(findings.stream().anyMatch(f -> f.tagPath().endsWith("(0040,9096)")));
		assertFalse(findings.stream().anyMatch(f -> f.tagPath().endsWith("(0008,1140)")));
	}

	@Test
	void empty_functional_group_sequence_is_flagged() {
		var dcm = hologicTomosynthesis();
		addFrameContent(dcm);
		shared(dcm).newSequence(Tag.FrameAnatomySequence, 0);

		assertTrue(hasFinding(functionalGroupFindings(dcm), CheckKind.TYPE1_EMPTY, "(5200,9229) > (0020,9071)"));
	}

}
