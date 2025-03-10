/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profiles;

import java.time.DateTimeException;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.VR;
import org.karnak.backend.data.entity.ExcludedTagEntity;
import org.karnak.backend.data.entity.IncludedTagEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.exception.ProfileException;
import org.karnak.backend.model.action.ActionItem;
import org.karnak.backend.model.action.MultipleActions;
import org.karnak.backend.model.action.Replace;
import org.karnak.backend.model.expression.ExprCondition;
import org.karnak.backend.model.expression.ExpressionError;
import org.karnak.backend.model.expression.ExpressionResult;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.model.profilepipe.TagActionMap;
import org.karnak.backend.util.DateFormat;
import org.karnak.backend.util.ShiftByTagDate;
import org.karnak.backend.util.ShiftDate;
import org.karnak.backend.util.ShiftRangeDate;

@Slf4j
public class ActionDates extends AbstractProfileItem {

	private final TagActionMap tagsAction;

	private final TagActionMap exceptedTagsAction;

	private final ActionItem actionByDefault;

	public ActionDates(ProfileElementEntity profileElementEntity) throws ProfileException {
		super(profileElementEntity);
		tagsAction = new TagActionMap();
		exceptedTagsAction = new TagActionMap();
		actionByDefault = new Replace("D");
		profileValidation();
		setActionHashMap();
	}

	private void setActionHashMap() {
		if (tagEntities != null && !tagEntities.isEmpty()) {
			for (IncludedTagEntity tag : tagEntities) {
				tagsAction.put(tag.getTagValue(), actionByDefault);
			}
		}
		if (excludedTagEntities != null && !excludedTagEntities.isEmpty()) {
			for (ExcludedTagEntity tag : excludedTagEntities) {
				exceptedTagsAction.put(tag.getTagValue(), actionByDefault);
			}
		}
	}

	@Override
	public void profileValidation() throws ProfileException {
		try {
			if (option == null) {
				throw new ProfileException("Cannot build the profile " + codeName
						+ " : An option must be given. Option available: [shift, shift_range, shift_by_tag, date_format]");
			}
			switch (option) {
				case "shift" -> ShiftDate.verifyShiftArguments(argumentEntities);
				case "shift_range" -> ShiftRangeDate.verifyShiftArguments(argumentEntities);
				case "shift_by_tag" -> ShiftByTagDate.verifyShiftArguments(argumentEntities);
				case "date_format" -> DateFormat.verifyPatternArguments(argumentEntities);
				default -> throw new ProfileException("Cannot build the profile " + codeName + " with the option given "
						+ option + " : Option available (shift, shift_range, shift_by_tag, date_format)");
			}
		}
		catch (Exception e) {
			throw e;
		}

		final ExpressionError expressionError = ExpressionResult.isValid(condition, new ExprCondition(new Attributes()),
				Boolean.class);
		if (condition != null && !expressionError.isValid()) {
			throw new ProfileException(expressionError.getMsg());
		}
	}

	@Override
	public ActionItem getAction(Attributes dcm, Attributes dcmCopy, int tag, HMAC hmac) {
		final VR vr = dcm.getVR(tag);

		if (vr == VR.AS || vr == VR.DA || vr == VR.DT || vr == VR.TM) {
			if (exceptedTagsAction.get(tag) != null) {
				return null;
			}

			if (!tagsAction.isEmpty() && tagsAction.get(tag) == null) {
				return null;
			}
			try {
				String dummyValue = applyOption(dcmCopy, tag, hmac);
				if (dummyValue != null) {
					actionByDefault.setDummyValue(dummyValue);
					return actionByDefault;
				}
			}
			catch (DateTimeException dateTimeException) {
				String dcmElValue = dcmCopy.getString(tag);
				log.warn(String.format("Invalid date %s, the most strictest action will be choose between X/Z/D",
						dcmElValue), dateTimeException);
				return new MultipleActions("X/Z/D");
			}
		}
		return null;
	}

	private String applyOption(Attributes dcmCopy, int tag, HMAC hmac) throws DateTimeException {
		return switch (option) {
			case "shift" -> ShiftDate.shift(dcmCopy, tag, argumentEntities);
			case "shift_range" -> ShiftRangeDate.shift(dcmCopy, tag, argumentEntities, hmac);
			case "shift_by_tag" -> ShiftByTagDate.shift(dcmCopy, tag, argumentEntities, hmac);
			case "date_format" -> DateFormat.format(dcmCopy, tag, argumentEntities);
			default -> null;
		};
	}

}
