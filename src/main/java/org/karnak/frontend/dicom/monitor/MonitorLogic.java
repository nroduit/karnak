/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.dicom.monitor;

import java.util.List;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.model.dicom.ConfigNode;
import org.karnak.backend.model.dicom.DicomNodeList;
import org.karnak.backend.model.dicom.WebDestinationNode;
import org.karnak.backend.model.dicom.result.DicomCapabilitiesResult;
import org.karnak.backend.model.dicom.result.DicomNodeCheckResult;
import org.karnak.backend.model.dicom.result.WebNodeCheckResult;
import org.karnak.backend.service.dicom.DicomCapabilitiesCheckService;
import org.karnak.backend.service.dicom.DicomNodeCheckService;
import org.karnak.backend.service.dicom.DicomWebCheckService;
import org.weasis.core.util.annotations.Generated;

@Generated()
@NullUnmarked
public class MonitorLogic {

	// PAGE
	private final MonitorView view;

	// SERVICES
	private final DicomNodeCheckService dicomNodeCheckService;

	private final DicomCapabilitiesCheckService dicomCapabilitiesCheckService;

	private final DicomWebCheckService dicomWebCheckService;

	// DATA
	private DicomNodeList dicomNodeListSelected;

	public MonitorLogic(MonitorView view, DicomNodeCheckService dicomNodeCheckService,
			DicomCapabilitiesCheckService dicomCapabilitiesCheckService, DicomWebCheckService dicomWebCheckService) {
		this.view = view;
		this.dicomNodeCheckService = dicomNodeCheckService;
		this.dicomCapabilitiesCheckService = dicomCapabilitiesCheckService;
		this.dicomWebCheckService = dicomWebCheckService;
	}

	public void dicomNodeListSelected(DicomNodeList dicomNodeList) {
		this.dicomNodeListSelected = dicomNodeList;
	}

	public void dicomEcho(String callingAet) {
		List<DicomNodeCheckResult> results = dicomNodeCheckService.check(callingAet, dicomNodeListSelected);
		view.displayResults(results);
	}

	public DicomCapabilitiesResult probeCapabilities(String callingAet, ConfigNode node) {
		return dicomCapabilitiesCheckService.probe(callingAet, node);
	}

	public List<WebNodeCheckResult> checkWebDestinations(List<WebDestinationNode> destinations) {
		return dicomWebCheckService.check(destinations);
	}

}