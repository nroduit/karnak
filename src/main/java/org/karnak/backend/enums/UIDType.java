/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.enums;

import java.util.Arrays;
import lombok.Getter;
import org.jspecify.annotations.NullUnmarked;

@Getter
@NullUnmarked
public enum UIDType {

	EXPLICIT_VR_LITTLE_ENDIAN("1.2.840.10008.1.2.1", "Explicit VR - Little Endian"),
	JPEG_BASELINE_8BIT("1.2.840.10008.1.2.4.50",
			"JPEG Baseline (Process 1): Default Transfer Syntax for Lossy JPEG 8 Bit Image Compression"),
	JPEG_EXTENDED_12BIT("1.2.840.10008.1.2.4.51",
			"JPEG Extended (Process 2 & 4): Default Transfer Syntax for Lossy JPEG 12 Bit Image Compression (Process 4 only)"),
	JPEG_SPECTRAL_SELECTION_NON_HIERARCHICAL_68("1.2.840.10008.1.2.4.53",
			"JPEG Spectral Selection, Non-Hierarchical (Process 6 & 8) (Retired)"),
	JPEG_FULL_PROGRESSION_NON_HIERARCHICAL_1012("1.2.840.10008.1.2.4.55",
			"JPEG Full Progression, Non-Hierarchical (Process 10 & 12) (Retired)"),
	JPEG_LOSSLESS("1.2.840.10008.1.2.4.57", "JPEG Lossless, Non-Hierarchical (Process 14)"),
	JPEG_LOSSLESS_SV1("1.2.840.10008.1.2.4.70",
			"JPEG Lossless, Non-Hierarchical, First-Order Prediction (Process 14 [Selection Value 1]): Default Transfer Syntax for Lossless JPEG Image Compression"),
	JPEG_LS_LOSSLESS("1.2.840.10008.1.2.4.80", "JPEG-LS Lossless Image Compression"),
	JPEG_LS_NEAR_LOSSLESS("1.2.840.10008.1.2.4.81", "JPEG-LS Lossy (Near-Lossless) Image Compression"),
	JPEG_2000_LOSSLESS("1.2.840.10008.1.2.4.90", "JPEG 2000 Image Compression (Lossless Only)"),
	JPEG_2000("1.2.840.10008.1.2.4.91", "JPEG 2000 Image Compression");

	public static final String DEFAULT_DESCRIPTION = "Keep original transfer syntax";

	private final String code;

	private final String description;

	UIDType(String code, String description) {
		this.code = code;
		this.description = description;
	}

	/**
	 * Get the enum from the role in parameter
	 * @param code Code of the enum
	 * @return UIDType found
	 */
	public static UIDType fromCode(final String code) {
		if (code != null) {
			return Arrays.stream(UIDType.values())
				.filter(u -> code.trim().equalsIgnoreCase(u.getCode()))
				.findFirst()
				.orElse(null);
		}
		return null;
	}

	/**
	 * Get the enum from the description in parameter
	 * @param description Description of the enum
	 * @return UIDType found
	 */
	public static UIDType fromDescription(final String description) {
		if (description != null) {
			return Arrays.stream(UIDType.values())
				.filter(u -> description.trim().equalsIgnoreCase(u.getDescription()))
				.findFirst()
				.orElse(null);
		}
		return null;
	}

	public static String descriptionOf(final String code) {
		UIDType uidType = fromCode(code);
		return uidType != null ? uidType.getDescription() : DEFAULT_DESCRIPTION;
	}

}
