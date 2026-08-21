/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
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
import java.util.stream.Stream;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.data.entity.ExcludedTagEntity;
import org.karnak.backend.data.entity.IncludedTagEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.data.entity.TagEntity;
import org.karnak.backend.exception.ProfileException;
import org.karnak.backend.model.action.ActionItem;
import org.karnak.backend.model.expression.ExprCondition;
import org.karnak.backend.model.expression.ExpressionError;
import org.karnak.backend.model.expression.ExpressionResult;
import org.karnak.backend.model.profilepipe.TagActionMap;
import org.karnak.backend.model.profilepipe.TagPathPattern;

public abstract class AbstractProfileItem implements ProfileItem {

	@Getter
	protected final String name;

	@Getter
	protected final String codeName;

	@Getter
	protected final String condition;

	protected final String action;

	@Getter
	protected final String option;

	protected final List<ArgumentEntity> argumentEntities;

	protected final List<IncludedTagEntity> tagEntities;

	protected final List<ExcludedTagEntity> excludedTagEntities;

	protected final Map<Integer, ActionItem> tagMap;

	@Getter
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

	public List<ArgumentEntity> getArguments() {
		return argumentEntities;
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
	public @Nullable ActionItem put(int tag, ActionItem action) {
		Objects.requireNonNull(action);
		return tagMap.put(tag, action);
	}

	@Override
	public void profileValidation() throws ProfileException {
		validateTagPaths();
		validateCondition();
	}

	/**
	 * Validates the configured tags, rejecting a malformed path such as
	 * {@code (0040,0275).}. A bare tag or a wildcard tag pattern is left to
	 * {@link TagActionMap}, which has always accepted them.
	 */
	protected void validateTagPaths() throws ProfileException {
		for (TagEntity tag : concat(tagEntities, excludedTagEntities)) {
			String value = tag.getTagValue();
			if (TagPathPattern.isPath(value) && !TagPathPattern.isValid(value)) {
				throw new ProfileException("Cannot build the profile " + codeName + ": invalid tag path " + value);
			}
		}
	}

	/**
	 * Rejects any configured tag path, for the items that can only work on a tag of the
	 * top-level dataset. Adding an attribute inside a sequence is not supported yet.
	 */
	protected void rejectTagPaths() throws ProfileException {
		for (TagEntity tag : concat(tagEntities, excludedTagEntities)) {
			if (TagPathPattern.isPath(tag.getTagValue())) {
				throw new ProfileException("Cannot build the profile " + codeName + ": the tag path "
						+ tag.getTagValue() + " is not supported, a tag can only be added to the top-level dataset");
			}
		}
	}

	private static List<? extends TagEntity> concat(@Nullable List<? extends TagEntity> included,
			@Nullable List<? extends TagEntity> excluded) {
		return Stream
			.concat(included == null ? Stream.empty() : included.stream(),
					excluded == null ? Stream.empty() : excluded.stream())
			.toList();
	}

	/** Validates the optional {@link #condition} SpEL expression. */
	protected void validateCondition() throws ProfileException {
		if (condition == null) {
			return;
		}
		ExpressionError expressionError = ExpressionResult.isValid(condition, new ExprCondition(), Boolean.class);
		if (!expressionError.isValid()) {
			throw new ProfileException(expressionError.getMsg());
		}
	}

	/**
	 * Maps each included tag (and each excluded tag when {@code excluded} is non-null) to
	 * the action.
	 */
	protected void mapTagsToAction(TagActionMap included, @Nullable TagActionMap excluded, ActionItem action) {
		if (tagEntities != null) {
			tagEntities.forEach(tag -> included.put(tag.getTagValue(), action));
		}
		if (excluded != null && excludedTagEntities != null) {
			excludedTagEntities.forEach(tag -> excluded.put(tag.getTagValue(), action));
		}
	}

}
