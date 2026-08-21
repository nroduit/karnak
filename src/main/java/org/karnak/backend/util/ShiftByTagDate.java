/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.util;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.util.TagUtils;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.model.expression.ExprCondition;

/**
 * {@code shift_by_tag} option of the {@code action.on.dates} profile item: shifts a
 * date/time value by the amounts stored in two other attributes of the same study, named
 * by the {@code days_tag} and {@code seconds_tag} arguments.
 */
@Slf4j
@NullUnmarked
public class ShiftByTagDate {

	private ShiftByTagDate() {
	}

	/**
	 * Checks the arguments of the option. Both {@code days_tag} and {@code seconds_tag}
	 * are optional; an omitted one simply contributes no shift on its unit.
	 * @param argumentEntities arguments of the profile item
	 */
	public static void verifyShiftArguments(List<ArgumentEntity> argumentEntities) {
		// All arguments are optional
	}

	/**
	 * Shifts the value of {@code tag} by the amounts read from the attributes named by
	 * the {@code days_tag} / {@code seconds_tag} arguments.
	 *
	 * <p>
	 * The two datasets play distinct roles and are <b>not</b> interchangeable:
	 * <ul>
	 * <li>{@code dcm} is the dataset currently being de-identified, at the nesting level
	 * of {@code tag}: during sequence recursion in
	 * {@link org.karnak.backend.service.profilepipe.Profile#applyAction} it is the
	 * sequence <i>item</i>. It is the only dataset holding the value to shift, but its
	 * other attributes may already have been de-identified, since the profile walks tags
	 * in ascending order.</li>
	 * <li>{@code original} is the untouched copy of that same dataset, taken before the
	 * pipeline started, from which the shift amounts are resolved outwards — the item
	 * first, then the enclosing study, where they usually live. Reading them from
	 * {@code dcm} would return the de-identified value whenever the referenced tag sorts
	 * before {@code tag}.</li>
	 * </ul>
	 *
	 * <p>
	 * A configured shift tag that cannot be resolved to a number yields {@code null}
	 * rather than a silent zero shift: returning the original value would let the
	 * unshifted date reach the destination, whereas {@code null} leaves the attribute to
	 * the following profile items (typically removed by the basic DICOM profile).
	 * @param dcm dataset being de-identified, at the nesting level of {@code tag}
	 * @param original untouched copy of {@code dcm}, used to resolve the shift tags
	 * @param tag tag whose value must be shifted
	 * @param argumentEntities arguments of the profile item
	 * @return the shifted value, or {@code null} if the value or a configured shift tag
	 * could not be resolved
	 */
	public static @Nullable String shift(Attributes dcm, Attributes original, int tag,
			List<ArgumentEntity> argumentEntities) {
		verifyShiftArguments(argumentEntities);

		String dcmElValue = dcm.getString(tag);
		Integer shiftDays = resolveShiftAmount(dcm, original, argumentEntities, "days_tag");
		Integer shiftSeconds = resolveShiftAmount(dcm, original, argumentEntities, "seconds_tag");
		if (shiftDays == null || shiftSeconds == null) {
			return null;
		}

		return ShiftDate.shiftValue(dcm, tag, dcmElValue, shiftDays, shiftSeconds);
	}

	/**
	 * Resolves the shift amount held by the tag named by {@code argumentKey}.
	 * @param dcm dataset being de-identified, used as a last resort when the copy does
	 * not hold the shift tag
	 * @param original untouched copy of {@code dcm}, resolved from its own level outwards
	 * @param argumentEntities arguments of the profile item
	 * @param argumentKey {@code days_tag} or {@code seconds_tag}
	 * @return {@code 0} when the argument is not configured, the parsed amount when it
	 * resolves, {@code null} when it is configured but absent or not a number
	 */
	private static @Nullable Integer resolveShiftAmount(Attributes dcm, Attributes original,
			List<ArgumentEntity> argumentEntities, String argumentKey) {
		String shiftTagValue = ArgumentUtil.stringValue(argumentEntities, argumentKey, null);
		if (shiftTagValue == null || shiftTagValue.isBlank()) {
			// Argument not configured: no shift on this unit
			return 0;
		}

		int shiftTag = ExprCondition.intFromHexString(shiftTagValue);
		String value = DicomObjectTools.getStringInScope(original, shiftTag);
		if (value == null) {
			value = dcm.getString(shiftTag);
		}
		if (value == null) {
			log.warn("Shift tag {} given by the argument {} is not present, the date is not shifted",
					TagUtils.toString(shiftTag), argumentKey);
			return null;
		}

		try {
			return Integer.valueOf(value.trim());
		}
		catch (NumberFormatException _) {
			log.warn("Shift tag {} given by the argument {} holds the non numeric value {}, the date is not shifted",
					TagUtils.toString(shiftTag), argumentKey, value);
			return null;
		}
	}

}