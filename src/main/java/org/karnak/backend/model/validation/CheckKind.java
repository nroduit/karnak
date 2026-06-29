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

/**
 * The kind of conformance check that produced a finding. Label and color are used by the
 * HTML report renderer; the color identifies the issue category: red/orange for presence
 * requirements, purple for encoding (VR/VM), teal for value constraints, blue for
 * cross-dataset consistency, brown for private-block structure, gray for retired usages.
 */
public enum CheckKind {

	UNKNOWN_SOP_CLASS("Unknown SOP Class", "#d35400"),

	RETIRED_SOP_CLASS("Retired SOP Class", "#7f8c8d"),

	MODULE_MISSING("Missing module", "#c0392b"),

	TYPE1_MISSING("Type 1 missing", "#e74c3c"),

	TYPE1_EMPTY("Type 1 empty", "#e74c3c"),

	TYPE2_MISSING("Type 2 missing", "#e67e22"),

	VR_MISMATCH("VR mismatch", "#8e44ad"),

	VM_VIOLATION("VM violation", "#9b59b6"),

	ENUMERATED_VALUE("Invalid value", "#16a085"),

	VALUE_TOO_LONG("Value too long", "#117a65"),

	VALUE_FORMAT("Value format", "#0e6655"),

	PIXEL_GEOMETRY("Pixel geometry", "#117a65"),

	LATERALITY("Laterality", "#117a65"),

	IMPLAUSIBLE_VALUE("Implausible value", "#16a085"),

	CODE_VALUE_INVALID("Invalid code value", "#0e6655"),

	CODE_UNKNOWN_CONCEPT("Unknown concept code", "#7f8c8d"),

	IMAGE_ORIENTATION("Image orientation", "#117a65"),

	PATIENT_ORIENTATION("Patient orientation", "#16a085"),

	UID_REUSE("UID reuse", "#2980b9"),

	MULTIFRAME("Multi-frame consistency", "#8e44ad"),

	SEGMENTATION("Segmentation", "#8e44ad"),

	NON_STANDARD_ATTRIBUTE("Non-standard attribute", "#2980b9"),

	PRIVACY_RISK("Residual identifier", "#c0392b"),

	PRIVATE_CREATOR_MISSING("Private creator missing", "#a04000"),

	PRIVATE_CREATOR_INVALID("Private creator invalid", "#a04000"),

	RETIRED_ATTRIBUTE("Retired attribute", "#7f8c8d"),

	RETIRED_TRANSFER_SYNTAX("Retired transfer syntax", "#7f8c8d"),

	MODALITY_SOP_MISMATCH("Modality/SOP Class mismatch", "#2980b9"),

	STUDY_UID_MISMATCH("Study UID mismatch", "#2980b9"),

	PATIENT_IDENTITY_MISMATCH("Patient identity mismatch", "#2980b9"),

	FRAME_OF_REFERENCE_MISMATCH("Frame of Reference mismatch", "#2980b9");

	private final String label;

	private final String color;

	CheckKind(String label, String color) {
		this.label = label;
		this.color = color;
	}

	public String getLabel() {
		return label;
	}

	public String getColor() {
		return color;
	}

}
