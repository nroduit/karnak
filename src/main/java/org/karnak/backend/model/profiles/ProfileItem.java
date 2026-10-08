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

import java.util.List;
import org.dcm4che3.data.Attributes;
import org.jspecify.annotations.Nullable;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.exception.ProfileException;
import org.karnak.backend.model.action.ActionItem;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.model.profilepipe.TagPath;

/** One item of a de-identification or tag-morphing profile. */
public interface ProfileItem {

	/**
	 * Returns the action this profile item wants to apply to {@code tag}, or {@code null}
	 * when it does not apply to it.
	 *
	 * <p>
	 * Two views of the same object are given, and implementations must pick deliberately
	 * between them:
	 * <ul>
	 * <li>{@code dcm} is the dataset being de-identified. It is the one that will be
	 * forwarded, and the one the returned action is executed on. Because
	 * {@link org.karnak.backend.service.profilepipe.Profile#applyAction} walks the tags
	 * in ascending order, every attribute of {@code dcm} sorting before {@code tag} may
	 * already have been de-identified. Read it for the value of {@code tag} itself, which
	 * is still untouched, and for what the object currently <i>is</i>.</li>
	 * <li>{@code original} is an untouched copy of {@code dcm}, taken before the pipeline
	 * started. Read it for anything else — another attribute, a condition, an expression
	 * — so that the decision does not depend on the order the tags are visited in.</li>
	 * </ul>
	 *
	 * <p>
	 * Both are at the same nesting level: when the profile recurses into a sequence, they
	 * are the item being visited and the copy of that same item. Enclosing datasets stay
	 * reachable from an item, so study-level attributes remain visible through
	 * {@link org.karnak.backend.util.DicomObjectTools#getStringInScope}.
	 * @param dcm dataset being de-identified, at the nesting level of {@code tag}
	 * @param original untouched copy of {@code dcm}
	 * @param tag tag being visited
	 * @param hmac hash context of the current patient
	 * @return the action to apply, or {@code null} when this item does not apply
	 */
	@Nullable ActionItem getAction(Attributes dcm, Attributes original, int tag, HMAC hmac);

	/**
	 * Same as {@link #getAction(Attributes, Attributes, int, HMAC)}, told in addition
	 * <i>where</i> {@code tag} sits in the object.
	 *
	 * <p>
	 * This is the method the pipeline calls. Only the items whose tags are configured by
	 * the user need to override it — the ones that may be given a path such as
	 * {@code (0040,0275).(0040,0007)} and must then apply only inside that sequence.
	 * Items selecting a fixed set of standard tags are location independent and keep the
	 * four-argument form, which this default delegates to.
	 * @param dcm dataset being de-identified, at the nesting level of {@code tag}
	 * @param original untouched copy of {@code dcm}
	 * @param tag tag being visited
	 * @param hmac hash context of the current patient
	 * @param path sequences enclosing {@code tag}, {@link TagPath#ROOT} at the top level
	 * @return the action to apply, or {@code null} when this item does not apply
	 */
	default @Nullable ActionItem getAction(Attributes dcm, Attributes original, int tag, HMAC hmac, TagPath path) {
		return getAction(dcm, original, tag, hmac);
	}

	@Nullable ActionItem put(int tag, ActionItem action);

	ActionItem remove(int tag);

	void clearTagMap();

	String getName();

	String getCodeName();

	String getCondition();

	String getOption();

	List<ArgumentEntity> getArguments();

	Integer getPosition();

	void profileValidation() throws ProfileException;

}
