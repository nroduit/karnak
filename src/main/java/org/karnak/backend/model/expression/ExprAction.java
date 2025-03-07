/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.expression;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Objects;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.img.util.DicomUtils;
import org.karnak.backend.exception.ExpressionActionException;
import org.karnak.backend.model.action.ActionItem;
import org.karnak.backend.model.action.ExcludeInstance;
import org.karnak.backend.model.action.Keep;
import org.karnak.backend.model.action.Remove;
import org.karnak.backend.model.action.Replace;
import org.karnak.backend.model.action.ReplaceNull;
import org.karnak.backend.model.action.UID;
import org.karnak.backend.util.DicomObjectTools;
import org.weasis.core.util.StringUtil;

public class ExprAction implements ExpressionItem {

	private int tag;

	private VR vr;

	private String stringValue;

	private Attributes dcm;

	private Attributes dcmCopy;

	public ExprAction(int tag, VR vr, Attributes dcm, Attributes dcmCopy) {
		this.tag = tag;
		this.vr = Objects.requireNonNull(vr);
		this.stringValue = dcmCopy.getString(this.tag);
		this.dcmCopy = dcmCopy;
		this.dcm = dcm;
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

	public int getTag() {
		return tag;
	}

	public void setTag(int tag) {
		this.tag = tag;
	}

	public VR getVr() {
		return vr;
	}

	public void setVr(VR vr) {
		this.vr = vr;
	}

	public String getStringValue() {
		return stringValue;
	}

	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
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

	public ActionItem ReplaceFromUriPost(String dummyValue) {
		String response = null;

		if (stringValue != null) {
			try {
				// TODO: to improve
				HttpResponse<String> httpResponse = HttpClient.newBuilder()
					.build()
					.send(HttpRequest.newBuilder()
						.uri(new URI(dummyValue))
						.POST(HttpRequest.BodyPublishers.ofString(stringValue))
						.build(), BodyHandlers.ofString());
				response = httpResponse.body();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			catch (Exception e) {
				throw new ExpressionActionException(
						"Issue when using action ReplaceFromUriPost:%s".formatted(e.getMessage()));
			}
		}

		ActionItem replace = new Replace("D");
		replace.setDummyValue(response);
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
		replace.setDummyValue(DicomUtils.getPatientAgeInPeriod(this.dcmCopy, Tag.PatientAge, false));
		return replace;
	}

	public ActionItem ExcludeInstance() {
		return new ExcludeInstance("E");
	}

}
