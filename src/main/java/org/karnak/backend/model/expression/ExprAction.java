/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.expression;

import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.img.util.DicomUtils;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.karnak.backend.model.action.ActionItem;
import org.karnak.backend.model.action.ExcludeInstance;
import org.karnak.backend.model.action.Keep;
import org.karnak.backend.model.action.Remove;
import org.karnak.backend.model.action.Replace;
import org.karnak.backend.model.action.ReplaceNull;
import org.karnak.backend.model.action.UID;
import org.karnak.backend.util.DicomObjectTools;
import org.weasis.core.util.StringUtil;

/**
 * Exposes the attributes of the object being de-identified to the expression of a profile
 * item, and builds the action it returns.
 *
 * <p>
 * The dataset it reads from is the untouched copy of the object at the nesting level of
 * the tag being processed: {@link #getString(int)} resolves a tag from that level
 * outwards, so an expression written for an attribute nested in a sequence sees both the
 * attributes of its item and those of the enclosing study.
 */
@NullUnmarked
public class ExprAction implements ExpressionItem {

	@Setter
	@Getter
	private int tag;

	@Setter
	@Getter
	private VR vr;

	@Setter
	@Getter
	private String stringValue;

	private @Nullable Attributes original;

	public ExprAction(int tag, VR vr, Attributes original) {
		this.tag = tag;
		this.vr = Objects.requireNonNull(vr);
		this.stringValue = original.getString(this.tag);
		this.original = original;
	}

	public ExprAction(int tag, VR vr, String stringValue) {
		this.tag = tag;
		this.vr = Objects.requireNonNull(vr);
		this.stringValue = stringValue;
	}

	public static boolean isHexTag(String elem) {
		String cleanElem = elem.replaceAll("[(),]", "").toUpperCase();

		if (!StringUtil.hasText(cleanElem) || cleanElem.length() != 8) {
			return false;
		}
		return cleanElem.matches("[0-9A-FX]+");
	}

	public ActionItem Keep() {
		return new Keep("K");
	}

	public ActionItem Remove() {
		return new Remove("X");
	}

	public ActionItem Replace(String dummyValue) {
		ActionItem replace = new Replace("D");
		replace.setDummyValue(dummyValue);
		return replace;
	}

	public ActionItem UID() {
		return new UID("U");
	}

	public ActionItem ReplaceNull() {
		return new ReplaceNull("Z");
	}

	/**
	 * Returns the value of a tag, looked up from the dataset the expression is evaluated
	 * on outwards: a tag of the enclosing study is visible from an expression evaluated
	 * for an attribute nested in a sequence.
	 * @param tag tag to look up
	 * @return the value found in the innermost dataset holding the tag, or {@code null}
	 */
	public @Nullable String getString(int tag) {
		return original == null ? null : DicomObjectTools.getStringInScope(original, tag);
	}

	/**
	 * Tells whether a tag is present anywhere in the object, at any nesting level, and
	 * not only below the dataset the expression is evaluated on.
	 * @param tag tag to look up
	 * @return {@code true} when any dataset of the object holds the tag
	 */
	public boolean tagIsPresent(int tag) {
		return original != null && DicomObjectTools.containsTagInAllAttributes(tag, original.getRoot());
	}

	public ActionItem ComputePatientAge() {
		ActionItem replace = new Replace("D");
		Attributes localCopy = original;
		if (localCopy != null) {
			// The age is derived from the patient and study modules, which live at the
			// top level even when the expression is evaluated inside a sequence
			replace.setDummyValue(DicomUtils.getPatientAgeInPeriod(localCopy.getRoot(), Tag.PatientAge, false));
		}
		return replace;
	}

	public ActionItem ExcludeInstance() {
		return new ExcludeInstance("E");
	}

}
