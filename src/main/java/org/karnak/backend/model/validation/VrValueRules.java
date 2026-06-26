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

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.dcm4che3.data.VR;

/**
 * Value-content rules per Value Representation (DICOM PS3.5 §6.2, Table 6.2-1): maximum
 * length and character repertoire / format. Used by the optional value-conformity check.
 * Only string VRs with a well-defined, machine-checkable constraint are covered; binary
 * VRs and free-text VRs without a useful bound are left alone.
 *
 * <p>
 * Values are expected to be already stripped of DICOM trailing padding. Range-matching
 * forms (used in queries, not stored objects) are not accepted.
 */
final class VrValueRules {

	/** Maximum length in characters, per value, for the bounded string VRs. */
	private static final Map<VR, Integer> MAX_LENGTH = new EnumMap<>(VR.class);

	static {
		MAX_LENGTH.put(VR.AE, 16);
		MAX_LENGTH.put(VR.CS, 16);
		MAX_LENGTH.put(VR.DS, 16);
		MAX_LENGTH.put(VR.IS, 12);
		MAX_LENGTH.put(VR.LO, 64);
		MAX_LENGTH.put(VR.SH, 16);
		MAX_LENGTH.put(VR.ST, 1024);
		MAX_LENGTH.put(VR.LT, 10240);
		MAX_LENGTH.put(VR.UI, 64);
		MAX_LENGTH.put(VR.DT, 26);
	}

	/**
	 * Small / fixed-width structured VRs (identifiers, codes, dates, numbers): an
	 * over-long value here is commonly rejected outright by receivers, so it is treated
	 * as an ERROR. The longer free-text VRs (LO, ST, LT, PN) are more leniently handled,
	 * so their overflow stays a WARNING.
	 */
	private static final Set<VR> SMALL_FIELD_VRS = EnumSet.of(VR.AE, VR.AS, VR.CS, VR.DA, VR.DS, VR.DT, VR.IS, VR.SH,
			VR.TM, VR.UI);

	private static final int PN_MAX_GROUP_LENGTH = 64;

	private static final int PN_MAX_GROUPS = 3;

	private static final int PN_MAX_COMPONENTS = 5;

	// PS3.5: DA is exactly YYYYMMDD, digits only (the legacy dotted form is no longer
	// valid)
	private static final Pattern DA = Pattern.compile("\\d{8}");

	// PS3.5: HH[MM[SS[.FFFFFF]]], digits and '.' only (the legacy colon form is no longer
	// valid)
	private static final Pattern TM = Pattern.compile("\\d{2}(\\d{2}(\\d{2}(\\.\\d{1,6})?)?)?");

	// PS3.5: YYYY[MM[DD[HH[MM[SS[.FFFFFF]]]]]] body, with an optional &ZZXX offset
	// checked apart. A fractional second is only valid on a full 14-digit datetime,
	// enforced separately so the pattern itself stays flat.
	private static final Pattern DT = Pattern.compile("\\d{4}(\\d{2}){0,5}(\\.\\d{1,6})?");

	private static final int DT_FULL_DIGITS = 14;

	private static final Pattern DT_OFFSET = Pattern.compile("[+-]\\d{4}");

	private static final String DA_FORMAT = "a valid date YYYYMMDD";

	private static final String TM_FORMAT = "a valid 24-hour time HHMMSS.FFFFFF";

	private static final String DT_FORMAT = "a valid datetime YYYYMMDDHHMMSS.FFFFFF&ZZXX";

	private static final Pattern IS = Pattern.compile("[+-]?\\d+");

	private static final Pattern DS = Pattern.compile("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?");

	private static final Pattern CS = Pattern.compile("[A-Z0-9 _]*");

	private static final Pattern AS = Pattern.compile("\\d{3}[DWMY]");

	// PS3.5 §9.1: dot-separated digit components; a component must not start with 0
	// unless it is a single 0 (registration authorities' leading zeros must be
	// stripped on encoding). Matched per-component to avoid the backtracking of a
	// repeated group.
	private static final Pattern UI_COMPONENT = Pattern.compile("0|[1-9]\\d*");

	private VrValueRules() {
	}

	/**
	 * @return a human-readable description of the length limit when {@code value} exceeds
	 * the maximum for {@code vr}, otherwise {@code null}
	 */
	static String lengthExpectation(VR vr, String value) {
		Integer max = MAX_LENGTH.get(vr);
		if (max != null && value.length() > max) {
			return "at most %d characters (VR %s)".formatted(max, vr.name());
		}
		if (vr == VR.PN) {
			for (String group : value.split("=", -1)) {
				if (group.length() > PN_MAX_GROUP_LENGTH) {
					return "at most %d characters per Person Name component group".formatted(PN_MAX_GROUP_LENGTH);
				}
			}
		}
		return null;
	}

	/**
	 * @return true when an over-long value of this VR should be an ERROR (small /
	 * structured field, commonly rejected by receivers) rather than a WARNING (long
	 * free-text field)
	 */
	static boolean lengthOverflowIsError(VR vr) {
		return SMALL_FIELD_VRS.contains(vr);
	}

	/**
	 * @return a human-readable description of the expected format when {@code value} does
	 * not satisfy the format/character-repertoire rule of {@code vr}, otherwise
	 * {@code null}
	 */
	static String formatExpectation(VR vr, String value) {
		return switch (vr) {
			case DA -> daExpectation(value);
			case TM -> tmExpectation(value);
			case DT -> dtExpectation(value);
			case IS -> isValidIntegerString(value) ? null : "an integer in -2^31..2^31-1";
			case DS -> DS.matcher(value).matches() ? null : "a decimal number";
			case CS -> CS.matcher(value).matches() ? null : "uppercase letters, digits, space or underscore";
			case AS -> AS.matcher(value).matches() ? null : "nnn followed by D, W, M or Y";
			case UI -> isValidUid(value) ? null
					: "dot-separated digit components, none with a leading zero (unless a single 0)";
			case PN -> pnStructureExpectation(value);
			default -> null;
		};
	}

	private static String daExpectation(String value) {
		if (!DA.matcher(value).matches() || outOfRange(value, 4, 6, 1, 12) || outOfRange(value, 6, 8, 1, 31)) {
			return DA_FORMAT;
		}
		return null;
	}

	private static String tmExpectation(String value) {
		if (!TM.matcher(value).matches() || outOfRange(value, 0, 2, 0, 23)) {
			return TM_FORMAT;
		}
		// MM and SS are validated only when present (right-truncation is allowed)
		if (value.length() >= 4 && outOfRange(value, 2, 4, 0, 59)) {
			return TM_FORMAT;
		}
		if (value.length() >= 6 && outOfRange(value, 4, 6, 0, 60)) {
			return TM_FORMAT;
		}
		return null;
	}

	private static String dtExpectation(String value) {
		String body = value;
		// A trailing &ZZXX UTC offset is validated and stripped before the body checks
		String tail = value.length() >= 5 ? value.substring(value.length() - 5) : "";
		if (DT_OFFSET.matcher(tail).matches()) {
			body = value.substring(0, value.length() - 5);
			if (outOfRange(tail, 1, 3, 0, 23) || outOfRange(tail, 3, 5, 0, 59)) {
				return DT_FORMAT;
			}
		}
		if (!DT.matcher(body).matches()) {
			return DT_FORMAT;
		}
		int dot = body.indexOf('.');
		String digits = dot < 0 ? body : body.substring(0, dot);
		// A fractional second is only valid on a full YYYYMMDDHHMMSS datetime
		if (dot >= 0 && digits.length() != DT_FULL_DIGITS) {
			return DT_FORMAT;
		}
		return dtDigitsInRange(digits) ? null : DT_FORMAT;
	}

	/** YYYY is always present; the remaining fields are validated only when present. */
	private static boolean dtDigitsInRange(String digits) {
		return !(digits.length() >= 6 && outOfRange(digits, 4, 6, 1, 12))
				&& !(digits.length() >= 8 && outOfRange(digits, 6, 8, 1, 31))
				&& !(digits.length() >= 10 && outOfRange(digits, 8, 10, 0, 23))
				&& !(digits.length() >= 12 && outOfRange(digits, 10, 12, 0, 59))
				&& !(digits.length() >= DT_FULL_DIGITS && outOfRange(digits, 12, 14, 0, 60));
	}

	/** True when the two-digit field {@code value[from:to]} is outside [min, max]. */
	private static boolean outOfRange(String value, int from, int to, int min, int max) {
		int parsed = Integer.parseInt(value.substring(from, to));
		return parsed < min || parsed > max;
	}

	private static boolean isValidUid(String value) {
		if (value.isEmpty()) {
			return false;
		}
		for (String component : value.split("\\.", -1)) {
			if (!UI_COMPONENT.matcher(component).matches()) {
				return false;
			}
		}
		return true;
	}

	private static boolean isValidIntegerString(String value) {
		if (!IS.matcher(value).matches()) {
			return false;
		}
		try {
			long parsed = Long.parseLong(value);
			return parsed >= Integer.MIN_VALUE && parsed <= Integer.MAX_VALUE;
		}
		catch (NumberFormatException _) {
			return false;
		}
	}

	private static String pnStructureExpectation(String value) {
		String[] groups = value.split("=", -1);
		if (groups.length > PN_MAX_GROUPS) {
			return "at most %d Person Name component groups".formatted(PN_MAX_GROUPS);
		}
		for (String group : groups) {
			if (group.split("\\^", -1).length > PN_MAX_COMPONENTS) {
				return "at most %d components per Person Name group".formatted(PN_MAX_COMPONENTS);
			}
		}
		return null;
	}

}