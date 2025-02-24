/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.dicom;

import java.util.Objects;
import org.weasis.core.util.StringUtil;
import org.weasis.dicom.param.DicomNode;

public class ConfigNode {

	private String name;

	private DicomNode calledNode;

	public ConfigNode(String name, DicomNode calledNode) {
		this.name = Objects.requireNonNull(name);
		this.calledNode = Objects.requireNonNull(calledNode);
	}

	@Override
	public String toString() {
		return name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		if (StringUtil.hasText(name)) {
			this.name = name;
		}
	}

	public String getAet() {
		return calledNode.getAet();
	}

	public String getHostname() {
		return calledNode.getHostname();
	}

	public Integer getPort() {
		return calledNode.getPort();
	}

	public DicomNode getCalledNode() {
		return calledNode;
	}

	public void setCalledNode(DicomNode calledNode) {
		if (calledNode != null) {
			this.calledNode = calledNode;
		}
	}

}
