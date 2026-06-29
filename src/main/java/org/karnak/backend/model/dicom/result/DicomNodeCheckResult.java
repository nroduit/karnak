/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.dicom.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.model.dicom.ConfigNode;

/**
 * Aggregated check of a single DICOM node: the {@link DicomEchoResult DICOM echo} and the
 * {@link NetworkCheckResult network reachability} for the called node, as queried with a
 * given calling AE Title. One instance maps to one row in the result grid.
 */
@AllArgsConstructor
@Getter
@NullUnmarked
public class DicomNodeCheckResult {

	private String callingAET;

	private ConfigNode calledNode;

	private DicomEchoResult dicomEchoResult;

	private NetworkCheckResult networkCheckResult;

	public String getCalledNodeDescription() {
		return (this.calledNode != null) ? this.calledNode.getName() : "";
	}

	public String getCalledNodeNetworkDetails() {
		return (this.calledNode != null)
				? this.calledNode.getAet() + " " + this.calledNode.getHostname() + " " + this.calledNode.getPort() : "";
	}

}