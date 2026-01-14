/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.dicom;

public class DicomEchoQueryData {

	private static final String DEFAULT_VALUE_FOR_CALLING_AET = "DCM-TOOLS";

	private String callingAet;

	private DicomNodeList calledDicomNodeType;

	private String calledAet;

	private String calledHostname;

	private Integer calledPort;

	public DicomEchoQueryData() {
		reset();
	}

	public String getCallingAet() {
		return callingAet;
	}

	public void setCallingAet(String callingAeTitle) {
		this.callingAet = callingAeTitle;
	}

	public DicomNodeList getCalledDicomNodeType() {
		return calledDicomNodeType;
	}

	public void setCalledDicomNodeType(DicomNodeList calledDicomNodeType) {
		this.calledDicomNodeType = calledDicomNodeType;
	}

	public String getCalledAet() {
		return calledAet;
	}

	public void setCalledAet(String calledAet) {
		this.calledAet = calledAet;
	}

	public String getCalledHostname() {
		return calledHostname;
	}

	public void setCalledHostname(String calledHostname) {
		this.calledHostname = calledHostname;
	}

	public Integer getCalledPort() {
		return calledPort;
	}

	public void setCalledPort(Integer calledPort) {
		this.calledPort = calledPort;
	}

	public void reset() {
		callingAet = DEFAULT_VALUE_FOR_CALLING_AET;
	}

}
