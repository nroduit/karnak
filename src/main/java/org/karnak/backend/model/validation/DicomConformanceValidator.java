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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.ElementDictionary;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.util.TagUtils;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.exception.SOPNotFoundException;
import org.karnak.backend.model.standard.AttributeDetail;
import org.karnak.backend.model.standard.Module;
import org.karnak.backend.model.standard.ModuleAttribute;
import org.karnak.backend.model.standard.StandardDICOM;

/**
 * Validates a single DICOM instance against the IOD of its SOP Class, using the bundled
 * machine-readable DICOM standard ({@link StandardDICOM}) plus
 * {@link CuratedValidationRules} for enumerated values.
 *
 * <p>
 * Scope and known limits: conditions (Type 1C/2C, usage C) are free text in the standard
 * JSON. Where {@link CuratedValidationRules} provides a machine-evaluable predicate that
 * resolves to {@link Ternary#TRUE} for the instance, the conditional attribute is treated
 * as mandatory; otherwise (no curated rule, or a clinical/free-text condition) it is only
 * validated when actually present. Private tags are validated only for block structure
 * (PS3.5 §7.8: a private data element must have a non-empty private creator reserving its
 * block); their values are not validated against vendor dictionaries. Tag paths deeper
 * than one sequence level are skipped. VR checks are skipped for Implicit VR Little
 * Endian datasets (the VR would come from the dictionary, making the check circular).
 */
@NullUnmarked
public class DicomConformanceValidator {

	private static final String TYPE_1 = "1";

	private static final String TYPE_1C = "1C";

	private static final String TYPE_2 = "2";

	private static final String TYPE_2C = "2C";

	private static final Set<VR> VM_CHECK_EXCLUDED_VRS = EnumSet.of(VR.SQ, VR.OB, VR.OW, VR.OD, VR.OF, VR.OL, VR.OV,
			VR.UN);

	private static final int MAX_VALUE_SUMMARY_LENGTH = 64;

	private static final int MAX_LISTED_MODULE_ATTRIBUTES = 8;

	/** Recursion bound for the coding-scheme scan, deep enough for SR content trees. */
	private static final int MAX_CODING_SCHEME_DEPTH = 16;

	/**
	 * dciodvfy tolerance for unit-vector and orthogonality tests on direction cosines.
	 */
	private static final double ORIENTATION_TOLERANCE = 0.0001;

	/**
	 * Entity-identifying UIDs that must not be reused for one another (dciodvfy
	 * {@code checkUIDsAreNotReusedForDifferentEntities}), paired with their display
	 * names.
	 */
	private static final int[] ENTITY_UID_TAGS = { Tag.SOPInstanceUID, Tag.SeriesInstanceUID, Tag.StudyInstanceUID,
			Tag.FrameOfReferenceUID };

	private static final String[] ENTITY_UID_NAMES = { "SOP Instance UID", "Series Instance UID", "Study Instance UID",
			"Frame of Reference UID" };

	/** Direction codes permitted in Patient Orientation (0020,0020) for a biped. */
	private static final String PATIENT_ORIENTATION_BIPED_CODES = "APHFLR";

	/**
	 * Standard attributes permitted in any dataset regardless of the SOP Class IOD, so
	 * the non-standard-attribute check does not flag them as Standard Extended usage.
	 */
	private static final Set<Integer> ALWAYS_ALLOWED_TAGS = Set.of(Tag.SpecificCharacterSet, Tag.TimezoneOffsetFromUTC);

	/**
	 * Default sequence recursion depth of the dataset sweep: the top-level dataset plus
	 * the first level of sequence items. The deep-sequence-validation option raises this
	 * to walk the whole SR content tree, enhanced-multiframe functional groups, etc.
	 */
	public static final int DEFAULT_MAX_SEQUENCE_DEPTH = 1;

	private final StandardDICOM standard;

	private final CuratedValidationRules rules;

	public DicomConformanceValidator(StandardDICOM standard, CuratedValidationRules rules) {
		this.standard = standard;
		this.rules = rules;
	}

	/**
	 * Validates one instance, without the optional value-content conformity check.
	 */
	public InstanceValidationResult validate(Attributes attrs, Set<Integer> bulkPresentTags, String transferSyntaxUid) {
		return validate(attrs, bulkPresentTags, transferSyntaxUid, false);
	}

	/**
	 * Validates one instance with the default sequence recursion depth.
	 */
	public InstanceValidationResult validate(Attributes attrs, Set<Integer> bulkPresentTags, String transferSyntaxUid,
			boolean checkValueConformity) {
		return validate(attrs, bulkPresentTags, transferSyntaxUid, checkValueConformity, DEFAULT_MAX_SEQUENCE_DEPTH);
	}

	/**
	 * Validates one instance, treating the dataset as de-identified so the
	 * residual-identifier check runs.
	 */
	public InstanceValidationResult validate(Attributes attrs, Set<Integer> bulkPresentTags, String transferSyntaxUid,
			boolean checkValueConformity, int maxSequenceDepth) {
		return validate(attrs, bulkPresentTags, transferSyntaxUid, checkValueConformity, maxSequenceDepth, true);
	}

	/**
	 * Validates one instance.
	 * @param attrs the dataset actually sent (metadata only, bulk values stripped)
	 * @param bulkPresentTags top-level tags whose value was present but stripped — they
	 * are treated as present and non-empty
	 * @param transferSyntaxUid transfer syntax the instance was sent with
	 * @param checkValueConformity when true, also validates that string values obey their
	 * VR length and format rules (PS3.5 §6.2) — opt-in, as real-world data deviates often
	 * @param maxSequenceDepth how many sequence levels the dataset sweep recurses into
	 * ({@link #DEFAULT_MAX_SEQUENCE_DEPTH} = one level; higher walks SR content trees and
	 * functional groups). Must not exceed the depth the {@code MetadataSnapshot}
	 * retained.
	 * @param deidentified whether the destination de-identifies. The residual-identifier
	 * check (direct identifiers expected to have been removed or emptied) is meaningful
	 * only for a de-identified destination; it is skipped otherwise, so identifiers a
	 * destination forwards intact by design are not flagged as a residual privacy risk.
	 */
	public InstanceValidationResult validate(Attributes attrs, Set<Integer> bulkPresentTags, String transferSyntaxUid,
			boolean checkValueConformity, int maxSequenceDepth, boolean deidentified) {
		String sopClassUid = attrs.getString(Tag.SOPClassUID);
		String sopInstanceUid = attrs.getString(Tag.SOPInstanceUID);
		List<ConformanceFinding> findings = new ArrayList<>();

		Map<Module, Map<String, ModuleAttribute>> modules = null;
		try {
			modules = standard.getModulesBySOP(sopClassUid);
			checkModuleRequirements(attrs, bulkPresentTags, modules, findings);
		}
		catch (SOPNotFoundException e) {
			// The bundled standard has no IOD for this SOP Class. The dcm4che registry
			// distinguishes a retired class (name suffixed " (Retired)") from an unknown
			// one
			String name = sopClassUid == null ? null : UID.nameOf(sopClassUid);
			if (name != null && name.contains("(Retired)")) {
				findings.add(new ConformanceFinding(TagUtils.toString(Tag.SOPClassUID), "SOP Class UID", null,
						Severity.WARNING, CheckKind.RETIRED_SOP_CLASS, "A non-retired SOP Class",
						"%s is retired: IOD module checks skipped".formatted(name)));
			}
			else {
				findings.add(new ConformanceFinding(TagUtils.toString(Tag.SOPClassUID), "SOP Class UID", null,
						Severity.WARNING, CheckKind.UNKNOWN_SOP_CLASS, "A SOP Class known to the DICOM standard",
						"Unknown SOP Class %s: IOD module checks skipped".formatted(sopClassUid)));
			}
		}

		boolean implicitVr = UID.ImplicitVRLittleEndian.equals(transferSyntaxUid);
		sweepDataset(attrs, bulkPresentTags, "", 0, implicitVr, checkValueConformity, maxSequenceDepth, findings);

		// Cross-attribute semantic checks, evaluated once on the top-level dataset
		checkPixelGeometry(attrs, findings);
		if (deidentified) {
			checkResidualIdentifiers(attrs, findings);
		}
		checkLaterality(attrs, findings);
		checkImplausibleValues(attrs, findings);
		checkCodeSequences(attrs, findings);
		checkUidReuse(attrs, findings);
		checkImageOrientation(attrs, findings);
		checkSpacingBetweenSlices(attrs, sopClassUid, findings);
		checkPatientOrientation(attrs, findings);
		checkNonStandardAttributes(attrs, modules, findings);

		// Tier 2: object-type-specific checks. The cheap consistency checks always run;
		// the per-frame relational checks ride on the deep-sequence option (they iterate
		// per-frame functional groups, which are only retained / worth walking when deep)
		checkPerFrameFunctionalGroupCount(attrs, findings);
		checkSegmentNumbering(attrs, findings);
		if (maxSequenceDepth > DEFAULT_MAX_SEQUENCE_DEPTH) {
			checkDimensionIndexValueCount(attrs, findings);
			checkFunctionalGroupExclusivity(attrs, findings);
		}

		return new InstanceValidationResult(sopClassUid, sopInstanceUid, List.copyOf(findings));
	}

	/**
	 * Checks IOD module presence and the Type requirements of the attributes of every
	 * module that applies.
	 */
	private void checkModuleRequirements(Attributes attrs, Set<Integer> bulkPresentTags,
			Map<Module, Map<String, ModuleAttribute>> modules, List<ConformanceFinding> findings) {
		// Same attribute may belong to several applicable modules: gather them and
		// resolve the effective (stricter) type afterwards
		Map<String, List<ModuleAttribute>> requirements = new HashMap<>();
		for (Map.Entry<Module, Map<String, ModuleAttribute>> entry : modules.entrySet()) {
			Module module = entry.getKey();
			Map<String, ModuleAttribute> moduleAttributes = entry.getValue();
			if (moduleAttributes == null || moduleAttributes.isEmpty()) {
				continue;
			}
			if (isModulePresent(attrs, moduleAttributes)) {
				moduleAttributes.values()
					.forEach(attribute -> requirements
						.computeIfAbsent(attribute.getTagPath(), tagPath -> new ArrayList<>())
						.add(attribute));
			}
			else if (Module.moduleIsMandatory(module)) {
				String requiredAttributes = summarizeRequiredAttributes(moduleAttributes);
				// A mandatory module whose attributes are all optional/conditional may
				// legitimately be entirely absent
				if (!requiredAttributes.isEmpty()) {
					findings.add(new ConformanceFinding(null, module.id(), module.id(), Severity.ERROR,
							CheckKind.MODULE_MISSING, "Mandatory module (usage M)",
							"None of its attributes is present. Required attributes: " + requiredAttributes));
				}
			}
			// Usage U/C modules are validated only when present: their free-text
			// conditions cannot be evaluated automatically
		}
		requirements.forEach((tagPath, moduleAttributes) -> checkAttributeRequirement(attrs, bulkPresentTags, tagPath,
				moduleAttributes, findings));
	}

	/** A module is considered present when any of its top-level attributes exists. */
	private boolean isModulePresent(Attributes attrs, Map<String, ModuleAttribute> moduleAttributes) {
		return moduleAttributes.keySet()
			.stream()
			.filter(tagPath -> !tagPath.contains(":"))
			.map(DicomConformanceValidator::parseTag)
			.anyMatch(tag -> tag != null && attrs.contains(tag));
	}

	private String summarizeRequiredAttributes(Map<String, ModuleAttribute> moduleAttributes) {
		List<String> required = moduleAttributes.values()
			.stream()
			.filter(attribute -> TYPE_1.equals(attribute.getType()) || TYPE_2.equals(attribute.getType()))
			.map(attribute -> attributeName(parseTag(firstSegment(attribute.getTagPath()))))
			.distinct()
			.sorted()
			.toList();
		if (required.size() <= MAX_LISTED_MODULE_ATTRIBUTES) {
			return String.join(", ", required);
		}
		return String.join(", ", required.subList(0, MAX_LISTED_MODULE_ATTRIBUTES))
				+ ", … (%d more)".formatted(required.size() - MAX_LISTED_MODULE_ATTRIBUTES);
	}

	private void checkAttributeRequirement(Attributes attrs, Set<Integer> bulkPresentTags, String tagPath,
			List<ModuleAttribute> moduleAttributes, List<ConformanceFinding> findings) {
		String[] segments = tagPath.split(":");
		if (segments.length > 2) {
			// Deeper nesting is out of scope
			return;
		}
		String type = ModuleAttribute.getStricterType(moduleAttributes);
		if (type == null) {
			return;
		}
		// A Type 1C/2C attribute whose curated condition is satisfied is mandatory for
		// this
		// instance: promote it to the equivalent unconditional Type so the checks below
		// fire
		type = effectiveConditionalType(attrs, type, moduleAttributes);
		String moduleId = moduleAttributes.stream()
			.map(ModuleAttribute::getModuleId)
			.distinct()
			.sorted()
			.collect(Collectors.joining(", "));
		Integer tag = parseTag(segments[0]);
		if (tag == null) {
			// Repeating-group placeholder paths like 60xx3000 are skipped
			return;
		}
		if (segments.length == 1) {
			checkType(attrs, bulkPresentTags, tag, null, type, moduleId, findings);
			return;
		}
		// Path inside a sequence: checked in every item when the sequence is present
		Integer childTag = parseTag(segments[1]);
		Sequence sequence = attrs.getSequence(tag);
		if (childTag == null || sequence == null || sequence.isEmpty()) {
			return;
		}
		for (Attributes item : sequence) {
			if (checkType(item, Set.of(), childTag, tag, type, moduleId, findings)) {
				// One finding per instance is enough, whatever the number of items
				break;
			}
		}
	}

	/**
	 * Resolves the effective Type of a conditional (1C/2C) attribute for this instance.
	 * When a curated condition tied to a module that contributes the conditional type
	 * evaluates to {@link Ternary#TRUE}, the attribute is mandatory, so 1C is promoted to
	 * Type 1 and 2C to Type 2. Otherwise (no curated rule, or the condition is
	 * FALSE/UNKNOWN) the conditional type is kept and the attribute is only validated
	 * when present.
	 */
	private String effectiveConditionalType(Attributes attrs, String type, List<ModuleAttribute> moduleAttributes) {
		if (!TYPE_1C.equals(type) && !TYPE_2C.equals(type)) {
			return type;
		}
		for (ModuleAttribute attribute : moduleAttributes) {
			if (!type.equals(attribute.getType())) {
				continue;
			}
			ConditionalRequirement requirement = rules.getConditionalRequirements()
				.get(attribute.getModuleId() + "/" + attribute.getTagPath());
			if (requirement == null) {
				continue;
			}
			if (ConditionEvaluator.evaluate(attrs, requirement.getRequiredWhen()) == Ternary.TRUE) {
				return TYPE_1C.equals(type) ? TYPE_1 : TYPE_2;
			}
		}
		return type;
	}

	/**
	 * @return true when a finding was added
	 */
	private boolean checkType(Attributes attrs, Set<Integer> bulkPresentTags, int tag, Integer parentTag, String type,
			String moduleId, List<ConformanceFinding> findings) {
		boolean present = attrs.contains(tag);
		boolean nonEmpty = attrs.containsValue(tag) || bulkPresentTags.contains(tag);
		String display = displayPath(parentTag, tag);
		String name = attributeName(tag);
		switch (type) {
			case TYPE_1 -> {
				if (!present) {
					findings.add(new ConformanceFinding(display, name, moduleId, Severity.ERROR,
							CheckKind.TYPE1_MISSING, "Type 1: present with a value", "Attribute is missing"));
					return true;
				}
				if (!nonEmpty) {
					findings.add(new ConformanceFinding(display, name, moduleId, Severity.ERROR, CheckKind.TYPE1_EMPTY,
							"Type 1: present with a value", "Attribute is present but empty"));
					return true;
				}
			}
			case TYPE_2 -> {
				if (!present) {
					findings.add(new ConformanceFinding(display, name, moduleId, Severity.WARNING,
							CheckKind.TYPE2_MISSING, "Type 2: present, possibly empty", "Attribute is missing"));
					return true;
				}
			}
			case TYPE_1C -> {
				if (present && !nonEmpty) {
					findings.add(new ConformanceFinding(display, name, moduleId, Severity.ERROR, CheckKind.TYPE1_EMPTY,
							"Type 1C: non-empty when present", "Attribute is present but empty"));
					return true;
				}
			}
			default -> {
				// Type 2C missing/empty and Type 3 are conformant in absence of an
				// evaluable condition
			}
		}
		return false;
	}

	/**
	 * Sweeps the actual dataset content: VR correctness, VM bounds, enumerated values,
	 * retired attributes, private-block structure and (optionally) value-content
	 * conformity, recursing into sequence items down to {@code maxSequenceDepth} levels.
	 */
	private void sweepDataset(Attributes attrs, Set<Integer> bulkPresentTags, String parentPath, int depth,
			boolean implicitVr, boolean checkValueConformity, int maxSequenceDepth, List<ConformanceFinding> findings) {
		checkPrivateBlocks(attrs, parentPath, findings);
		for (int tag : attrs.tags()) {
			if (TagUtils.isGroupLength(tag) || TagUtils.isPrivateGroup(tag) || bulkPresentTags.contains(tag)) {
				continue;
			}
			VR vr = attrs.getVR(tag);
			String display = parentPath + TagUtils.toString(tag);
			String id = TagUtils.toHexString(tag).toLowerCase(Locale.ROOT);
			var detail = standard.getAttributeDetail(id);

			if (detail != null && "Y".equalsIgnoreCase(detail.retired())) {
				findings.add(
						new ConformanceFinding(display, detail.name(), null, Severity.INFO, CheckKind.RETIRED_ATTRIBUTE,
								"Attribute is retired in the current DICOM standard", "Attribute is used"));
			}

			checkVr(detail, vr, display, implicitVr, findings);

			if (!VM_CHECK_EXCLUDED_VRS.contains(vr) && attrs.containsValue(tag)) {
				String[] values = attrs.getStrings(tag);
				if (values != null && values.length > 0) {
					checkVm(detail, values.length, display, findings);
					checkEnumeratedValues(id, values, display, findings);
					if (checkValueConformity) {
						checkValueFormat(tag, vr, values, display, findings);
					}
				}
			}

			if (vr == VR.SQ && depth < maxSequenceDepth) {
				Sequence sequence = attrs.getSequence(tag);
				if (sequence != null) {
					for (Attributes item : sequence) {
						sweepDataset(item, Set.of(), display + " > ", depth + 1, implicitVr, checkValueConformity,
								maxSequenceDepth, findings);
					}
				}
			}
		}
	}

	/**
	 * Optional value-content conformity check: verifies that each string value obeys the
	 * length and format/character-repertoire rules of its VR ({@link VrValueRules}). At
	 * most one length and one format finding is raised per attribute to keep reports
	 * readable.
	 */
	private void checkValueFormat(int tag, VR vr, String[] values, String display, List<ConformanceFinding> findings) {
		boolean lengthReported = false;
		boolean formatReported = false;
		for (String raw : values) {
			if (raw == null) {
				continue;
			}
			String value = trimPadding(raw);
			if (value.isEmpty()) {
				continue;
			}
			if (!lengthReported) {
				String expectation = VrValueRules.lengthExpectation(vr, value);
				if (expectation != null) {
					// Small / structured fields are commonly rejected by receivers when
					// too
					// long, so they are an ERROR; long free-text overflow stays a WARNING
					Severity severity = VrValueRules.lengthOverflowIsError(vr) ? Severity.ERROR : Severity.WARNING;
					findings.add(new ConformanceFinding(display, attributeName(tag), null, severity,
							CheckKind.VALUE_TOO_LONG, expectation, value.length() + " characters"));
					lengthReported = true;
				}
			}
			if (!formatReported) {
				String expectation = VrValueRules.formatExpectation(vr, value);
				if (expectation != null) {
					findings.add(new ConformanceFinding(display, attributeName(tag), null, Severity.WARNING,
							CheckKind.VALUE_FORMAT, expectation, summarize(value)));
					formatReported = true;
				}
			}
			if (lengthReported && formatReported) {
				break;
			}
		}
	}

	/** Strips DICOM trailing padding (space / null) before applying value rules. */
	private static String trimPadding(String value) {
		int end = value.length();
		while (end > 0 && (value.charAt(end - 1) == ' ' || value.charAt(end - 1) == '\0')) {
			end--;
		}
		return value.substring(0, end);
	}

	/**
	 * Pixel-geometry coherence, mirroring dciodvfy's
	 * {@code checkPixelAspectRatioValidIfPresent} and
	 * {@code checkPixelSpacingCalibration}: an explicit Pixel Aspect Ratio (0028,0034) of
	 * 1\1 is not permitted (it is the implied default — an ERROR in dciodvfy); and when
	 * Pixel Spacing (0028,0030) is given without a Pixel Spacing Calibration Type
	 * (0028,0A02), it must match the Imager Pixel Spacing (0018,1164) and Nominal Scanned
	 * Pixel Spacing (0018,2010) if those are present.
	 */
	private void checkPixelGeometry(Attributes attrs, List<ConformanceFinding> findings) {
		int[] aspect = readInts(attrs, Tag.PixelAspectRatio);
		if (aspect != null && aspect.length == 2 && aspect[0] == aspect[1]) {
			findings.add(new ConformanceFinding(TagUtils.toString(Tag.PixelAspectRatio), "Pixel Aspect Ratio", null,
					Severity.ERROR, CheckKind.PIXEL_GEOMETRY, "absent when the ratio is 1:1 (the implied default)",
					aspect[0] + "\\" + aspect[1]));
		}
		// Differing spacings are only legitimate when a calibration type explains them
		if (!attrs.containsValue(Tag.PixelSpacing) || attrs.contains(Tag.PixelSpacingCalibrationType)) {
			return;
		}
		double[] pixelSpacing = readDoubles(attrs, Tag.PixelSpacing);
		if (pixelSpacing == null || pixelSpacing.length < 2) {
			return;
		}
		checkSpacingMatches(attrs, pixelSpacing, Tag.ImagerPixelSpacing, "Imager Pixel Spacing", findings);
		checkSpacingMatches(attrs, pixelSpacing, Tag.NominalScannedPixelSpacing, "Nominal Scanned Pixel Spacing",
				findings);
	}

	private void checkSpacingMatches(Attributes attrs, double[] pixelSpacing, int tag, String name,
			List<ConformanceFinding> findings) {
		double[] other = readDoubles(attrs, tag);
		if (other != null && other.length >= 2 && (pixelSpacing[0] != other[0] || pixelSpacing[1] != other[1])) {
			findings.add(new ConformanceFinding(TagUtils.toString(Tag.PixelSpacing), "Pixel Spacing", null,
					Severity.WARNING, CheckKind.PIXEL_GEOMETRY,
					"equal to " + name + ", or a Pixel Spacing Calibration Type explaining the difference",
					"Pixel Spacing differs from " + name));
		}
	}

	/**
	 * Flags direct identifiers (curated list) still carrying a value after the
	 * de-identification pipeline — a residual privacy risk. NOTE: this is not a check the
	 * dciodvfy verifier performs; the tag set mirrors the attributes removed by
	 * dciodvfy's companion de-identifier (dcanon). On-mission for a gateway whose purpose
	 * is de-identification. Only run for a de-identified destination (see the
	 * {@code deidentified} flag on {@link #validate}); for a destination that forwards
	 * identified data by design these identifiers are expected, not residual.
	 */
	private void checkResidualIdentifiers(Attributes attrs, List<ConformanceFinding> findings) {
		for (int tag : rules.getIdentifyingAttributes()) {
			if (attrs.containsValue(tag)) {
				findings.add(new ConformanceFinding(TagUtils.toString(tag), attributeName(tag), null, Severity.WARNING,
						CheckKind.PRIVACY_RISK, "removed or emptied by de-identification",
						"a direct identifier is still present"));
			}
		}
	}

	/**
	 * Laterality consistency, mirroring dciodvfy's {@code LateralityRequired} condition
	 * (condn.tpl): a paired body part is expected to carry a Laterality (0020,0060). A
	 * body part is treated as paired unless its Body Part Examined (0018,0015) is in the
	 * curated unpaired list, another laterality is already conveyed (Image / Measurement
	 * Laterality, Frame Anatomy), or the object is a segmentation / specimen / waveform
	 * for which laterality does not apply. Deliberate divergence from dciodvfy: an absent
	 * Body Part Examined is not treated as paired, to avoid flagging the many real-world
	 * images that legitimately carry neither a body part nor a laterality. The R/L and
	 * R/L/U/B value sets themselves are enforced by the enumerated-value rules.
	 */
	private void checkLaterality(Attributes attrs, List<ConformanceFinding> findings) {
		String bodyPart = attrs.getString(Tag.BodyPartExamined);
		if (bodyPart == null || bodyPart.isBlank()) {
			return;
		}
		String bp = bodyPart.trim().toUpperCase(Locale.ROOT);
		if (rules.getUnpairedBodyParts().contains(bp) || lateralityConveyedOrNotApplicable(attrs)) {
			return;
		}
		String laterality = attrs.getString(Tag.Laterality);
		if (laterality == null || laterality.isBlank()) {
			findings.add(new ConformanceFinding(TagUtils.toString(Tag.Laterality), "Laterality", null, Severity.WARNING,
					CheckKind.LATERALITY, "a Laterality for the paired body part " + bp, "no laterality is given"));
		}
	}

	/**
	 * dciodvfy {@code LateralityRequired} exclusions: another laterality is already
	 * given, or laterality does not apply to this kind of object.
	 */
	private static boolean lateralityConveyedOrNotApplicable(Attributes attrs) {
		if (attrs.containsValue(Tag.ImageLaterality) || attrs.contains(Tag.MeasurementLaterality)
				|| attrs.contains(Tag.FrameAnatomySequence) || attrs.contains(Tag.SegmentSequence)
				|| attrs.contains(Tag.SpecimenDescriptionSequence)) {
			return true;
		}
		String modality = attrs.getString(Tag.Modality);
		return "ECG".equals(modality) || "EPS".equals(modality) || "RESP".equals(modality);
	}

	/**
	 * Flags numeric attributes carrying a value of zero. dciodvfy distinguishes
	 * structural counts / geometry where zero is illegal ({@code NotZeroError}) from
	 * acquisition / physics parameters where zero is merely implausible
	 * ({@code NotZeroWarning}); the two curated sets map to ERROR and WARNING
	 * respectively.
	 */
	private void checkImplausibleValues(Attributes attrs, List<ConformanceFinding> findings) {
		checkZeroValues(attrs, rules.getZeroIsErrorAttributes(), Severity.ERROR, findings);
		checkZeroValues(attrs, rules.getZeroIsWarningAttributes(), Severity.WARNING, findings);
	}

	private void checkZeroValues(Attributes attrs, Set<Integer> tags, Severity severity,
			List<ConformanceFinding> findings) {
		for (int tag : tags) {
			if (!attrs.containsValue(tag)) {
				continue;
			}
			double[] values = readDoubles(attrs, tag);
			if (values == null) {
				continue;
			}
			for (double value : values) {
				if (value == 0) {
					findings.add(new ConformanceFinding(TagUtils.toString(tag), attributeName(tag), null, severity,
							CheckKind.IMPLAUSIBLE_VALUE, "a non-zero value", "zero"));
					break;
				}
			}
		}
	}

	/**
	 * Code-sequence content checks, mirroring dciodvfy's
	 * {@code checkCodeValuesDoNotContainInappropriateCharacters} and
	 * {@code checkCodeSequenceItemsAreNotUnknown}: a Code Value (0008,0100) must use only
	 * the characters allowed by its Coding Scheme Designator (0008,0102) (SNOMED: A-Z,
	 * 0-9, '-'; DICOM: A-Z, 0-9), and a code item must not encode the SNOMED/DICOM
	 * "Unknown" concept. (dciodvfy does NOT treat SRT/SNM3 as deprecated in favour of SCT
	 * — it recognises them as equivalent SNOMED designators.) Recurses through code
	 * sequences independently of the configured sweep depth; each issue is reported once
	 * per instance.
	 */
	private void checkCodeSequences(Attributes attrs, List<ConformanceFinding> findings) {
		scanCodeSequences(attrs, 0, new LinkedHashSet<>(), findings);
	}

	private void scanCodeSequences(Attributes attrs, int depth, Set<String> reported,
			List<ConformanceFinding> findings) {
		String designator = trimmedOrNull(attrs.getString(Tag.CodingSchemeDesignator));
		String codeValue = trimmedOrNull(attrs.getString(Tag.CodeValue));
		if (designator != null && codeValue != null) {
			checkCodeValueCharacters(designator, codeValue, reported, findings);
			checkUnknownConcept(designator, codeValue, attrs.getString(Tag.CodeMeaning), reported, findings);
		}
		if (depth >= MAX_CODING_SCHEME_DEPTH) {
			return;
		}
		for (int tag : attrs.tags()) {
			if (attrs.getVR(tag) == VR.SQ) {
				Sequence sequence = attrs.getSequence(tag);
				if (sequence != null) {
					for (Attributes item : sequence) {
						scanCodeSequences(item, depth + 1, reported, findings);
					}
				}
			}
		}
	}

	private static boolean isSnomedDesignator(String designator) {
		return "SRT".equals(designator) || "SNM3".equals(designator) || "99SDM".equals(designator);
	}

	private void checkCodeValueCharacters(String designator, String codeValue, Set<String> reported,
			List<ConformanceFinding> findings) {
		boolean snomed = isSnomedDesignator(designator);
		boolean dicom = "DCM".equals(designator);
		if (!snomed && !dicom) {
			return;
		}
		for (int i = 0; i < codeValue.length(); i++) {
			char c = codeValue.charAt(i);
			boolean ok = (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (snomed && c == '-');
			if (!ok && reported.add("invalid:" + designator + "/" + codeValue)) {
				findings.add(new ConformanceFinding(TagUtils.toString(Tag.CodeValue), "Code Value", null,
						Severity.WARNING, CheckKind.CODE_VALUE_INVALID,
						"only %s for coding scheme %s".formatted(snomed ? "A-Z, 0-9 or '-'" : "A-Z or 0-9", designator),
						codeValue));
				return;
			}
		}
	}

	private void checkUnknownConcept(String designator, String codeValue, String codeMeaning, Set<String> reported,
			List<ConformanceFinding> findings) {
		boolean unknown = ("SCT".equals(designator) && "261665006".equals(codeValue))
				|| (isSnomedDesignator(designator) && "R-41198".equals(codeValue))
				|| (codeMeaning != null && codeMeaning.trim().equalsIgnoreCase("Unknown"));
		if (unknown && reported.add("unknown:" + designator + "/" + codeValue)) {
			findings.add(new ConformanceFinding(TagUtils.toString(Tag.CodeValue), "Code Value", null, Severity.WARNING,
					CheckKind.CODE_UNKNOWN_CONCEPT, "a specific coded concept",
					"the code denotes the \"Unknown\" concept (%s, %s)".formatted(designator, codeValue)));
		}
	}

	private static String trimmedOrNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	/**
	 * UIDs that identify different entities must not be reused (mirrors dciodvfy's
	 * {@code checkUIDsAreNotReusedForDifferentEntities}): the SOP Instance, Series
	 * Instance, Study Instance and Frame of Reference UIDs must all differ from one
	 * another. Reusing one value across entity levels breaks identity, so each clash is
	 * an ERROR.
	 */
	private void checkUidReuse(Attributes attrs, List<ConformanceFinding> findings) {
		for (int i = 0; i < ENTITY_UID_TAGS.length; i++) {
			String first = trimmedOrNull(attrs.getString(ENTITY_UID_TAGS[i]));
			if (first == null) {
				continue;
			}
			for (int j = i + 1; j < ENTITY_UID_TAGS.length; j++) {
				if (first.equals(trimmedOrNull(attrs.getString(ENTITY_UID_TAGS[j])))) {
					findings.add(new ConformanceFinding(TagUtils.toString(ENTITY_UID_TAGS[j]), ENTITY_UID_NAMES[j],
							null, Severity.ERROR, CheckKind.UID_REUSE, "a UID distinct from the " + ENTITY_UID_NAMES[i],
							"the same UID as the " + ENTITY_UID_NAMES[i]));
				}
			}
		}
	}

	/**
	 * Image Orientation (Patient) (0020,0037) must be two unit direction-cosine vectors
	 * (row and column) that are mutually orthogonal, mirroring dciodvfy's
	 * {@code checkOrientationsAreUnitVectors} / {@code checkOrientationsAreOrthogonal}
	 * (tolerance 1e-4). Both deviations are ERRORs.
	 */
	private void checkImageOrientation(Attributes attrs, List<ConformanceFinding> findings) {
		double[] iop = readDoubles(attrs, Tag.ImageOrientationPatient);
		if (iop == null || iop.length < 6) {
			return;
		}
		checkUnitVector(iop, 0, "row", findings);
		checkUnitVector(iop, 3, "column", findings);
		double dot = iop[0] * iop[3] + iop[1] * iop[4] + iop[2] * iop[5];
		if (Math.abs(dot) > ORIENTATION_TOLERANCE) {
			findings.add(new ConformanceFinding(TagUtils.toString(Tag.ImageOrientationPatient),
					"Image Orientation (Patient)", null, Severity.ERROR, CheckKind.IMAGE_ORIENTATION,
					"orthogonal row and column direction cosines", "row·column = " + fmt(dot)));
		}
	}

	private void checkUnitVector(double[] vectors, int offset, String which, List<ConformanceFinding> findings) {
		double sumOfSquares = vectors[offset] * vectors[offset] + vectors[offset + 1] * vectors[offset + 1]
				+ vectors[offset + 2] * vectors[offset + 2];
		if (Math.abs(sumOfSquares - 1) > ORIENTATION_TOLERANCE) {
			findings.add(new ConformanceFinding(TagUtils.toString(Tag.ImageOrientationPatient),
					"Image Orientation (Patient)", null, Severity.ERROR, CheckKind.IMAGE_ORIENTATION,
					"a unit " + which + " direction-cosine vector", "|" + which + "|² = " + fmt(sumOfSquares)));
		}
	}

	/**
	 * Spacing Between Slices (0018,0088) must not be negative (mirrors dciodvfy's
	 * {@code checkSpacingBetweenSlicesIsNotNegative}). Nuclear Medicine images are
	 * exempt: they legitimately use a negative spacing to convey slice direction.
	 */
	private void checkSpacingBetweenSlices(Attributes attrs, String sopClassUid, List<ConformanceFinding> findings) {
		if (!attrs.containsValue(Tag.SpacingBetweenSlices) || UID.NuclearMedicineImageStorage.equals(sopClassUid)) {
			return;
		}
		double[] values = readDoubles(attrs, Tag.SpacingBetweenSlices);
		if (values != null && values.length > 0 && values[0] < 0) {
			findings.add(new ConformanceFinding(TagUtils.toString(Tag.SpacingBetweenSlices), "Spacing Between Slices",
					null, Severity.ERROR, CheckKind.IMPLAUSIBLE_VALUE, "a non-negative value", fmt(values[0])));
		}
	}

	/**
	 * Patient Orientation (0020,0020) values must use only the biped direction codes (A,
	 * P, H, F, L, R), must not combine opposing directions (A/P, H/F or L/R) within one
	 * value, and the row and column directions must differ (mirrors dciodvfy's
	 * {@code checkPatientOrientationValuesForBiped}). Quadruped orientation uses a
	 * different code set and is skipped.
	 */
	private void checkPatientOrientation(Attributes attrs, List<ConformanceFinding> findings) {
		String[] values = attrs.getStrings(Tag.PatientOrientation);
		if (values == null || values.length == 0
				|| "QUADRUPED".equalsIgnoreCase(attrs.getString(Tag.AnatomicalOrientationType))) {
			return;
		}
		String first = null;
		for (int index = 0; index < values.length; index++) {
			String value = values[index] == null ? "" : values[index].trim().toUpperCase(Locale.ROOT);
			if (value.isEmpty()) {
				continue;
			}
			checkPatientOrientationValue(value, findings);
			if (index == 0) {
				first = value;
			}
			else if (value.equals(first)) {
				findings.add(new ConformanceFinding(TagUtils.toString(Tag.PatientOrientation), "Patient Orientation",
						null, Severity.ERROR, CheckKind.PATIENT_ORIENTATION, "distinct row and column directions",
						"both directions are " + value));
			}
		}
	}

	private void checkPatientOrientationValue(String value, List<ConformanceFinding> findings) {
		boolean illegal = false;
		for (int k = 0; k < value.length(); k++) {
			if (PATIENT_ORIENTATION_BIPED_CODES.indexOf(value.charAt(k)) < 0) {
				illegal = true;
				break;
			}
		}
		if (illegal) {
			findings.add(new ConformanceFinding(TagUtils.toString(Tag.PatientOrientation), "Patient Orientation", null,
					Severity.ERROR, CheckKind.PATIENT_ORIENTATION, "only the codes L, R, A, P, H or F", value));
		}
		if (hasBoth(value, 'A', 'P') || hasBoth(value, 'H', 'F') || hasBoth(value, 'L', 'R')) {
			findings.add(new ConformanceFinding(TagUtils.toString(Tag.PatientOrientation), "Patient Orientation", null,
					Severity.ERROR, CheckKind.PATIENT_ORIENTATION, "no opposing directions within one value", value));
		}
	}

	private static boolean hasBoth(String value, char a, char b) {
		return value.indexOf(a) >= 0 && value.indexOf(b) >= 0;
	}

	private static String fmt(double value) {
		return "%.5f".formatted(value);
	}

	/**
	 * Enhanced multi-frame: the number of Per-frame Functional Groups Sequence
	 * (5200,9230) items must equal Number of Frames (0028,0008) (mirrors dciodvfy's
	 * {@code checkCountPerFrameFunctionalGroupsMatchesNumberOfFrames}). Number of Frames
	 * defaults to 1 when absent. Cheap (item count only), so runs regardless of depth.
	 */
	private void checkPerFrameFunctionalGroupCount(Attributes attrs, List<ConformanceFinding> findings) {
		Sequence perFrame = attrs.getSequence(Tag.PerFrameFunctionalGroupsSequence);
		if (perFrame == null || perFrame.isEmpty()) {
			return;
		}
		int frames = attrs.getInt(Tag.NumberOfFrames, 1);
		if (perFrame.size() != frames) {
			findings.add(new ConformanceFinding(TagUtils.toString(Tag.PerFrameFunctionalGroupsSequence),
					"Per-frame Functional Groups Sequence", null, Severity.ERROR, CheckKind.MULTIFRAME,
					"one item per frame (%d)".formatted(frames), "%d items".formatted(perFrame.size())));
		}
	}

	/**
	 * Segmentation: Segment Number (0062,0004) of each Segment Sequence (0062,0002) item
	 * must increase monotonically from one by one (mirrors dciodvfy's
	 * {@code checkSegmentNumbersMonotonicallyIncreasingFromOneByOne}). LABELMAP
	 * segmentations are exempt. Only the first offending item is reported.
	 */
	private void checkSegmentNumbering(Attributes attrs, List<ConformanceFinding> findings) {
		Sequence segments = attrs.getSequence(Tag.SegmentSequence);
		if (segments == null || segments.isEmpty() || "LABELMAP".equals(attrs.getString(Tag.SegmentationType))) {
			return;
		}
		for (int s = 0; s < segments.size(); s++) {
			Attributes segment = segments.get(s);
			if (!segment.containsValue(Tag.SegmentNumber)) {
				continue;
			}
			int number = segment.getInt(Tag.SegmentNumber, 0);
			if (number != s + 1) {
				findings.add(new ConformanceFinding(TagUtils.toString(Tag.SegmentNumber), "Segment Number", null,
						Severity.ERROR, CheckKind.SEGMENTATION, "%d (one-based item position)".formatted(s + 1),
						"item %d has Segment Number %d".formatted(s + 1, number)));
				return;
			}
		}
	}

	/**
	 * Enhanced multi-frame: in each Per-frame Functional Groups Sequence item, the Frame
	 * Content Sequence (0020,9111) Dimension Index Values (0020,9157) must have one value
	 * per item of the Dimension Index Sequence (0020,9222) (mirrors dciodvfy's
	 * {@code checkCountOfDimensionIndexValuesMatchesDimensionIndexSequence}). Deep-only:
	 * Dimension Index Values live two sequence levels down. Reports the first mismatch.
	 */
	private void checkDimensionIndexValueCount(Attributes attrs, List<ConformanceFinding> findings) {
		Sequence dimensions = attrs.getSequence(Tag.DimensionIndexSequence);
		Sequence perFrame = attrs.getSequence(Tag.PerFrameFunctionalGroupsSequence);
		if (dimensions == null || dimensions.isEmpty() || perFrame == null || perFrame.isEmpty()) {
			return;
		}
		int expected = dimensions.size();
		for (int f = 0; f < perFrame.size(); f++) {
			Sequence frameContent = perFrame.get(f).getSequence(Tag.FrameContentSequence);
			if (frameContent == null || frameContent.isEmpty()) {
				continue;
			}
			int[] values = frameContent.get(0).getInts(Tag.DimensionIndexValues);
			int actual = values == null ? 0 : values.length;
			if (actual != expected) {
				findings.add(new ConformanceFinding(TagUtils.toString(Tag.DimensionIndexValues),
						"Dimension Index Values", null, Severity.ERROR, CheckKind.MULTIFRAME,
						"%d values (one per dimension)".formatted(expected),
						"%d values in frame %d".formatted(actual, f + 1)));
				return;
			}
		}
	}

	/**
	 * Enhanced multi-frame: a functional-group sequence carried in the (single) Shared
	 * Functional Groups Sequence (5200,9229) item must not also appear in any Per-frame
	 * Functional Groups Sequence item (mirrors dciodvfy's
	 * {@code checkPerFrameFunctionalGroupsSequencesAreNotAlreadyPresentInSharedFunctionalGroup}).
	 * Deep-only. Each duplicated functional group is reported once.
	 */
	private void checkFunctionalGroupExclusivity(Attributes attrs, List<ConformanceFinding> findings) {
		Sequence shared = attrs.getSequence(Tag.SharedFunctionalGroupsSequence);
		Sequence perFrame = attrs.getSequence(Tag.PerFrameFunctionalGroupsSequence);
		if (shared == null || shared.size() != 1 || perFrame == null || perFrame.isEmpty()) {
			return;
		}
		Attributes sharedItem = shared.get(0);
		Set<Integer> reported = new LinkedHashSet<>();
		for (int f = 0; f < perFrame.size(); f++) {
			Attributes frame = perFrame.get(f);
			for (int tag : frame.tags()) {
				if (frame.getVR(tag) == VR.SQ && sharedItem.contains(tag) && reported.add(tag)) {
					findings.add(new ConformanceFinding(TagUtils.toString(tag), attributeName(tag), null,
							Severity.ERROR, CheckKind.MULTIFRAME,
							"present in either Shared or Per-frame Functional Groups, not both",
							"also present in the Shared Functional Groups (frame %d)".formatted(f + 1)));
				}
			}
		}
	}

	/**
	 * Flags standard (public) attributes present at the top level that belong to no
	 * module of this SOP Class's IOD — a Standard Extended SOP Class usage (mirrors
	 * dciodvfy). Private, retired (reported separately), file-meta and a few
	 * universally-allowed attributes are excluded. Skipped when the IOD is unknown
	 * (handled elsewhere).
	 */
	private void checkNonStandardAttributes(Attributes attrs, Map<Module, Map<String, ModuleAttribute>> modules,
			List<ConformanceFinding> findings) {
		if (modules == null || modules.isEmpty()) {
			return;
		}
		Set<Integer> allowed = new LinkedHashSet<>(ALWAYS_ALLOWED_TAGS);
		for (Map<String, ModuleAttribute> moduleAttributes : modules.values()) {
			for (String tagPath : moduleAttributes.keySet()) {
				Integer tag = parseTag(firstSegment(tagPath));
				if (tag != null) {
					allowed.add(tag);
				}
			}
		}
		for (int tag : attrs.tags()) {
			if (TagUtils.isGroupLength(tag) || isPrivateTag(tag) || TagUtils.groupNumber(tag) == 0x0002
					|| allowed.contains(tag)) {
				continue;
			}
			String id = TagUtils.toHexString(tag).toLowerCase(Locale.ROOT);
			var detail = standard.getAttributeDetail(id);
			// Only known, non-retired standard attributes qualify: unknown/private tags
			// are out of scope and retired ones are reported as RETIRED_ATTRIBUTE
			if (detail == null || "Y".equalsIgnoreCase(detail.retired())) {
				continue;
			}
			findings.add(new ConformanceFinding(TagUtils.toString(tag), detail.name(), null, Severity.INFO,
					CheckKind.NON_STANDARD_ATTRIBUTE, "an attribute defined in the IOD of this SOP Class",
					"standard attribute not part of this IOD (Standard Extended SOP Class)"));
		}
	}

	private static int[] readInts(Attributes attrs, int tag) {
		try {
			return attrs.getInts(tag);
		}
		catch (NumberFormatException _) {
			// Malformed integer string: a separate VALUE_FORMAT check covers it
			return null;
		}
	}

	private static double[] readDoubles(Attributes attrs, int tag) {
		try {
			double[] values = attrs.getDoubles(tag);
			if (values != null && values.length > 0) {
				return values;
			}
			// dcm4che getDoubles() yields null for binary integer VRs (US/SS/UL/SL):
			// fall back to an integer read so structural counts are still covered
			int[] ints = attrs.getInts(tag);
			if (ints == null) {
				return null;
			}
			double[] widened = new double[ints.length];
			for (int i = 0; i < ints.length; i++) {
				widened[i] = ints[i];
			}
			return widened;
		}
		catch (NumberFormatException _) {
			// Malformed numeric string: a separate VALUE_FORMAT check covers it
			return null;
		}
	}

	/**
	 * Checks the structure of the private blocks of one dataset (PS3.5 §7.8): every
	 * private data element must belong to a block reserved by a non-empty Private Creator
	 * Data Element. Vendor-specific values are not interpreted — only the block structure
	 * is verified, which needs no private dictionary.
	 */
	private void checkPrivateBlocks(Attributes attrs, String parentPath, List<ConformanceFinding> findings) {
		Set<Integer> reportedOrphanBlocks = new LinkedHashSet<>();
		for (int tag : attrs.tags()) {
			if (TagUtils.isGroupLength(tag) || !isPrivateTag(tag)) {
				continue;
			}
			int element = tag & 0xFFFF;
			if (isPrivateCreatorElement(element)) {
				// Private Creator Data Element (gggg,0010-00FF) must carry an identifier
				if (!attrs.containsValue(tag)) {
					findings.add(new ConformanceFinding(parentPath + TagUtils.toString(tag), "Private Creator", null,
							Severity.WARNING, CheckKind.PRIVATE_CREATOR_INVALID,
							"A non-empty private creator identifier", "Private Creator is present but empty"));
				}
				continue;
			}
			int block = (element >>> 8) & 0xFF;
			if (block < 0x10) {
				// (gggg,0001-000F) and blocks below 0x10 are reserved, not standard
				// private
				// data element blocks — out of scope
				continue;
			}
			int creatorTag = (tag & 0xFFFF0000) | block;
			// Presence (not value): a present-but-empty creator is reported separately as
			// PRIVATE_CREATOR_INVALID when its own element is reached
			if (!attrs.contains(creatorTag) && reportedOrphanBlocks.add(creatorTag)) {
				findings.add(new ConformanceFinding(parentPath + TagUtils.toString(creatorTag), "Private Creator", null,
						Severity.WARNING, CheckKind.PRIVATE_CREATOR_MISSING, "A Private Creator reserving this block",
						"No Private Creator defines this private block"));
			}
		}
	}

	/** Private data elements live in odd groups. */
	private static boolean isPrivateTag(int tag) {
		return (tag & 0x00010000) != 0;
	}

	/** Private Creator Data Elements occupy elements (gggg,0010) to (gggg,00FF). */
	private static boolean isPrivateCreatorElement(int element) {
		return (element & 0xFF00) == 0 && (element & 0x00FF) >= 0x10;
	}

	private void checkVr(AttributeDetail detail, VR vr, String display, boolean implicitVr,
			List<ConformanceFinding> findings) {
		if (detail == null || detail.valueRepresentation() == null || implicitVr) {
			return;
		}
		Set<String> expected = Set.of(detail.valueRepresentation().split(" or "));
		if (expected.contains(vr.name())) {
			return;
		}
		if (vr == VR.UN) {
			findings.add(new ConformanceFinding(display, detail.name(), null, Severity.INFO, CheckKind.VR_MISMATCH,
					"VR " + detail.valueRepresentation(), "VR UN (unknown)"));
		}
		else {
			findings.add(new ConformanceFinding(display, detail.name(), null, Severity.ERROR, CheckKind.VR_MISMATCH,
					"VR " + detail.valueRepresentation(), "VR " + vr.name()));
		}
	}

	private void checkVm(AttributeDetail detail, int count, String display, List<ConformanceFinding> findings) {
		if (detail == null) {
			return;
		}
		VmSpec vmSpec = VmSpec.parse(detail.valueMultiplicity());
		if (vmSpec != null && !vmSpec.matches(count)) {
			findings.add(new ConformanceFinding(display, detail.name(), null, Severity.ERROR, CheckKind.VM_VIOLATION,
					"VM " + detail.valueMultiplicity(), count + " value(s)"));
		}
	}

	private void checkEnumeratedValues(String id, String[] values, String display, List<ConformanceFinding> findings) {
		EnumeratedRule rule = rules.getEnumeratedValues().get(id);
		if (rule == null) {
			return;
		}
		List<String> allowed = rule.getValues();
		Set<String> invalid = new LinkedHashSet<>();
		for (String value : values) {
			String trimmed = value == null ? "" : value.trim();
			if (!trimmed.isEmpty() && !allowed.contains(trimmed)) {
				invalid.add(trimmed);
			}
		}
		if (!invalid.isEmpty()) {
			// A value outside a closed Enumerated Values set is a standard violation
			// (ERROR);
			// an unexpected Defined Term is only a deviation worth a WARNING
			Severity severity = rule.isClosed() ? Severity.ERROR : Severity.WARNING;
			findings.add(new ConformanceFinding(display, attributeName(parseTag(id)), null, severity,
					CheckKind.ENUMERATED_VALUE, "One of " + allowed, summarize(String.join("\\", invalid))));
		}
	}

	private String attributeName(Integer tag) {
		if (tag == null) {
			return "Unknown attribute";
		}
		var detail = standard.getAttributeDetail(TagUtils.toHexString(tag).toLowerCase(Locale.ROOT));
		if (detail != null && detail.name() != null) {
			return detail.name();
		}
		String keyword = ElementDictionary.keywordOf(tag, null);
		return keyword == null || keyword.isEmpty() ? TagUtils.toString(tag) : keyword;
	}

	private static String displayPath(Integer parentTag, int tag) {
		return parentTag == null ? TagUtils.toString(tag)
				: TagUtils.toString(parentTag) + " > " + TagUtils.toString(tag);
	}

	private static String firstSegment(String tagPath) {
		int separator = tagPath.indexOf(':');
		return separator < 0 ? tagPath : tagPath.substring(0, separator);
	}

	private static Integer parseTag(String hexTag) {
		if (hexTag == null) {
			return null;
		}
		try {
			return (int) Long.parseLong(hexTag, 16);
		}
		catch (NumberFormatException e) {
			// Repeating-group placeholders like "60xx3000" cannot be parsed
			return null;
		}
	}

	private static String summarize(String value) {
		return value.length() <= MAX_VALUE_SUMMARY_LENGTH ? value : value.substring(0, MAX_VALUE_SUMMARY_LENGTH) + "…";
	}

}
