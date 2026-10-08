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

import org.dcm4che3.data.Attributes;
import org.dcm4che3.util.TagUtils;
import org.karnak.backend.util.DicomObjectTools;

/**
 * Exposes the attributes of the object being de-identified to the condition of a profile
 * item.
 *
 * <p>
 * Tags are resolved from the current dataset outwards, so a condition evaluated for an
 * attribute nested in a sequence sees both the attributes of its item and those of the
 * enclosing study — see {@link DicomObjectTools#getStringInScope}.
 */
public class ExprCondition implements ExpressionItem {

	private final Attributes dcm;

	public ExprCondition() {
		this(new Attributes());
	}

	public ExprCondition(Attributes dcm) {
		this.dcm = dcm;
	}

	public static void expressionValidation(String condition) {
		ExprCondition exprCondition = new ExprCondition();
		ExpressionResult.get(condition, exprCondition, Boolean.class);
	}

	public static int intFromHexString(String tag) {
		String cleanTag = tag.replaceAll("[(),]", "").toUpperCase();
		return TagUtils.intFromHexString(cleanTag);
	}

	public boolean tagValueIsPresent(String tag, String value) {
		int cleanTag = intFromHexString(tag);
		return tagValueIsPresent(cleanTag, value);
	}

	public boolean tagValueIsPresent(int tag, String value) {
		String dcmValue = DicomObjectTools.getStringInScope(dcm, tag);
		return dcmValue != null && dcmValue.equals(value);
	}

	public boolean tagValueContains(String tag, String value) {
		int cleanTag = intFromHexString(tag);
		return tagValueContains(cleanTag, value);
	}

	public boolean tagValueContains(int tag, String value) {
		String dcmValue = DicomObjectTools.getStringInScope(dcm, tag);
		return dcmValue != null && dcmValue.contains(value);
	}

	public boolean tagValueBeginsWith(String tag, String value) {
		int cleanTag = intFromHexString(tag);
		return tagValueBeginsWith(cleanTag, value);
	}

	public boolean tagValueBeginsWith(int tag, String value) {
		String dcmValue = DicomObjectTools.getStringInScope(dcm, tag);
		return dcmValue != null && dcmValue.startsWith(value);
	}

	public boolean tagValueEndsWith(String tag, String value) {
		int cleanTag = intFromHexString(tag);
		return tagValueEndsWith(cleanTag, value);
	}

	public boolean tagValueEndsWith(int tag, String value) {
		String dcmValue = DicomObjectTools.getStringInScope(dcm, tag);
		return dcmValue != null && dcmValue.endsWith(value);
	}

	public boolean tagIsPresent(String tag) {
		int cleanTag = intFromHexString(tag);
		return tagIsPresent(cleanTag);
	}

	public boolean tagIsPresent(int tag) {
		return DicomObjectTools.getStringInScope(dcm, tag) != null;
	}

}
