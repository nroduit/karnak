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

import java.util.List;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.VR;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.data.entity.ExcludedTagEntity;
import org.karnak.backend.data.entity.IncludedTagEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.exception.ProfileException;
import org.karnak.backend.model.action.AbstractAction;
import org.karnak.backend.model.action.ActionItem;
import org.karnak.backend.model.expression.ExprAction;
import org.karnak.backend.model.expression.ExpressionError;
import org.karnak.backend.model.expression.ExpressionResult;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.model.profilepipe.TagActionMap;

public class Expression extends AbstractProfileItem {

	private final TagActionMap tagsAction;

	private final TagActionMap exceptedTagsAction;

	private final ActionItem actionByDefault;

	public Expression(ProfileElementEntity profileElementEntity) throws ProfileException {
		super(profileElementEntity);
		tagsAction = new TagActionMap();
		exceptedTagsAction = new TagActionMap();
		actionByDefault = AbstractAction.convertAction("K");
		profileValidation();
		setActionHashMap();
	}

	private void setActionHashMap() {
		if (tagEntities != null) {
			for (IncludedTagEntity tag : tagEntities) {
				tagsAction.put(tag.getTagValue(), actionByDefault);
			}
			if (excludedTagEntities != null) {
				for (ExcludedTagEntity tag : excludedTagEntities) {
					exceptedTagsAction.put(tag.getTagValue(), actionByDefault);
				}
			}
		}
	}

	@Override
	public ActionItem getAction(Attributes dcm, Attributes dcmCopy, int tag, HMAC hmac) {
		if (exceptedTagsAction.get(tag) == null && tagsAction.get(tag) != null) {
			final String expr = argumentEntities.get(0).getArgumentValue();
			final ExprAction exprAction = new ExprAction(tag, dcm.getVR(tag), dcm, dcmCopy);
			return (ActionItem) ExpressionResult.get(expr, exprAction, ActionItem.class);
		}
		return null;
	}

	@Override
	public void profileValidation() throws ProfileException {
		if (argumentEntities.stream().noneMatch(argument -> argument.getArgumentKey().equals("expr"))) {
			List<String> args = argumentEntities.stream().map(ArgumentEntity::getArgumentKey).toList();
			throw new IllegalArgumentException(
					"Cannot build the expression: Missing argument, the class need [expr] as parameters. Parameters given "
							+ args);
		}

		final String expr = argumentEntities.get(0).getArgumentValue();
		final ExpressionError expressionError = ExpressionResult.isValid(expr,
				new ExprAction(1, VR.AE, new Attributes(), new Attributes()), ActionItem.class);

		if (!expressionError.isValid()) {
			throw new IllegalArgumentException(
					String.format("Expression is not valid: \n\r%s", expressionError.getMsg()));
		}
	}

}
