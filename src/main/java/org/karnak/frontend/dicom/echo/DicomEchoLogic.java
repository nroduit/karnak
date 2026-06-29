/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.dicom.echo;

import org.karnak.backend.model.dicom.ConfigNode;
import org.karnak.backend.model.dicom.DicomEchoQueryData;
import org.karnak.backend.model.dicom.result.DicomCapabilitiesResult;
import org.karnak.backend.model.dicom.result.DicomNodeCheckResult;
import org.karnak.backend.service.dicom.DicomCapabilitiesCheckService;
import org.karnak.backend.service.dicom.DicomNodeCheckService;
import org.weasis.core.util.annotations.Generated;
import org.weasis.dicom.param.DicomNode;

@Generated()
public class DicomEchoLogic {

	// PAGE
	private final DicomEchoView view;

	// SERVICES
	private final DicomNodeCheckService dicomNodeCheckService;

	private final DicomCapabilitiesCheckService dicomCapabilitiesCheckService;

	public DicomEchoLogic(DicomEchoView view, DicomNodeCheckService dicomNodeCheckService,
			DicomCapabilitiesCheckService dicomCapabilitiesCheckService) {
		this.view = view;
		this.dicomNodeCheckService = dicomNodeCheckService;
		this.dicomCapabilitiesCheckService = dicomCapabilitiesCheckService;
	}

	public void dicomEcho(DicomEchoQueryData data) {
		DicomNode dcmNode = new DicomNode(data.getCalledAet(), data.getCalledHostname(), data.getCalledPort());
		ConfigNode calledNode = new ConfigNode(data.getCalledAet(), dcmNode);

		DicomNodeCheckResult result = dicomNodeCheckService.check(data.getCallingAet(), calledNode);
		view.displayResult(result);

		DicomCapabilitiesResult capabilities = dicomCapabilitiesCheckService.probe(data.getCallingAet(), calledNode);
		view.displayCapabilities(capabilities);
	}

}