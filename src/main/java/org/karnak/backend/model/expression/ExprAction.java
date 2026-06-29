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

	private @Nullable Attributes dcmCopy;

	public ExprAction(int tag, VR vr, Attributes dcmCopy) {
		this.tag = tag;
		this.vr = Objects.requireNonNull(vr);
		this.stringValue = dcmCopy.getString(this.tag);
		this.dcmCopy = dcmCopy;
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

	public String getString(int tag) {
		return DicomUtils.getStringFromDicomElement(dcmCopy, tag);
	}

	public boolean tagIsPresent(int tag) {
		return DicomObjectTools.containsTagInAllAttributes(tag, dcmCopy);
	}

	public ActionItem ComputePatientAge() {
		ActionItem replace = new Replace("D");
		Attributes localCopy = dcmCopy;
		if (localCopy != null) {
			replace.setDummyValue(DicomUtils.getPatientAgeInPeriod(localCopy, Tag.PatientAge, false));
		}
		return replace;
	}

	public ActionItem ExcludeInstance() {
		return new ExcludeInstance("E");
	}

}
