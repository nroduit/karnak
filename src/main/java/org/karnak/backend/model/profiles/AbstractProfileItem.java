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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.data.entity.ExcludedTagEntity;
import org.karnak.backend.data.entity.IncludedTagEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.exception.ProfileException;
import org.karnak.backend.model.action.ActionItem;
import org.karnak.backend.model.expression.ExprCondition;
import org.karnak.backend.model.expression.ExpressionError;
import org.karnak.backend.model.expression.ExpressionResult;

public abstract class AbstractProfileItem implements ProfileItem {

	protected final String name;

	protected final String codeName;

	protected final String condition;

	protected final String action;

	protected final String option;

	protected final List<ArgumentEntity> argumentEntities;

	protected final List<IncludedTagEntity> tagEntities;

	protected final List<ExcludedTagEntity> excludedTagEntities;

	protected final Map<Integer, ActionItem> tagMap;

	protected final Integer position;

	protected AbstractProfileItem(ProfileElementEntity profileElementEntity) {
		this.name = Objects.requireNonNull(profileElementEntity.getName());
		this.codeName = Objects.requireNonNull(profileElementEntity.getCodename());
		this.condition = profileElementEntity.getCondition();
		this.action = profileElementEntity.getAction();
		this.option = profileElementEntity.getOption();
		this.argumentEntities = profileElementEntity.getArgumentEntities();
		this.tagEntities = profileElementEntity.getIncludedTagEntities();
		this.excludedTagEntities = profileElementEntity.getExcludedTagEntities();
		this.position = profileElementEntity.getPosition();
		this.tagMap = new HashMap<>();
	}

	public String getName() {
		return name;
	}

	public String getCodeName() {
		return codeName;
	}

	public String getCondition() {
		return condition;
	}

	public String getOption() {
		return option;
	}

	public List<ArgumentEntity> getArguments() {
		return argumentEntities;
	}

	public Integer getPosition() {
		return position;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public void clearTagMap() {
		tagMap.clear();
	}

	@Override
	public ActionItem remove(int tag) {
		return tagMap.remove(tag);
	}

	@Override
	public ActionItem put(int tag, ActionItem action) {
		Objects.requireNonNull(action);
		return tagMap.put(tag, action);
	}

	@Override
	public void profileValidation() throws ProfileException {
		ExpressionError expressionError = ExpressionResult.isValid(condition, new ExprCondition(), Boolean.class);
		if (condition != null && !expressionError.isValid()) {
			throw new ProfileException(expressionError.getMsg());
		}
	}

}
