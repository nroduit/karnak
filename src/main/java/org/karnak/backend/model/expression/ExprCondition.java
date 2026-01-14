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
import org.dcm4che3.img.util.DicomUtils;
import org.dcm4che3.util.TagUtils;

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
		String dcmValue = DicomUtils.getStringFromDicomElement(dcm, tag);
		return dcmValue != null && dcmValue.equals(value);
	}

	public boolean tagValueContains(String tag, String value) {
		int cleanTag = intFromHexString(tag);
		return tagValueContains(cleanTag, value);
	}

	public boolean tagValueContains(int tag, String value) {
		String dcmValue = DicomUtils.getStringFromDicomElement(dcm, tag);
		return dcmValue != null && dcmValue.contains(value);
	}

	public boolean tagValueBeginsWith(String tag, String value) {
		int cleanTag = intFromHexString(tag);
		return tagValueBeginsWith(cleanTag, value);
	}

	public boolean tagValueBeginsWith(int tag, String value) {
		String dcmValue = DicomUtils.getStringFromDicomElement(dcm, tag);
		return dcmValue != null && dcmValue.startsWith(value);
	}

	public boolean tagValueEndsWith(String tag, String value) {
		int cleanTag = intFromHexString(tag);
		return tagValueEndsWith(cleanTag, value);
	}

	public boolean tagValueEndsWith(int tag, String value) {
		String dcmValue = DicomUtils.getStringFromDicomElement(dcm, tag);
		return dcmValue != null && dcmValue.endsWith(value);
	}

	public boolean tagIsPresent(String tag) {
		int cleanTag = intFromHexString(tag);
		return dcm.getString(cleanTag) != null;
	}

	public boolean tagIsPresent(int tag) {
		return dcm.getString(tag) != null;
	}

}
