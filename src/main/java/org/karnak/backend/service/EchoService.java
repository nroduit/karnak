/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import java.util.ArrayList;
import java.util.List;
import org.karnak.backend.dicom.DicomForwardDestination;
import org.karnak.backend.dicom.ForwardDestination;
import org.karnak.backend.dicom.ForwardDicomNode;
import org.karnak.backend.dicom.WebForwardDestination;
import org.karnak.backend.model.echo.DestinationEcho;
import org.karnak.backend.service.gateway.GatewaySetUpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.weasis.dicom.op.Echo;
import org.weasis.dicom.param.AdvancedParams;
import org.weasis.dicom.param.ConnectOptions;
import org.weasis.dicom.param.DicomNode;
import org.weasis.dicom.param.DicomState;

/**
 * Service managing echo
 */
@Service
public class EchoService {

	// Service
	private final GatewaySetUpService gatewaySetUpService;

	@Autowired
	public EchoService(final GatewaySetUpService gatewaySetUpService) {
		this.gatewaySetUpService = gatewaySetUpService;
	}

	/**
	 * Retrieve the configured destinations from the setup
	 * @param sourceAet Source AeTitle
	 * @return List of configured destinations
	 */
	public List<DestinationEcho> retrieveStatusConfiguredDestinations(String sourceAet) {
		List<DestinationEcho> destinationEchos = new ArrayList<>();
		gatewaySetUpService.getDestinationNode(sourceAet)
			.ifPresent(sourceNode -> fillDestinationsStatus(destinationEchos, sourceNode,
					gatewaySetUpService.getDestinations(sourceNode)));
		return destinationEchos;
	}

	/**
	 * Adds an echo status entry for each destination (C-ECHO for DICOM, URL-only for
	 * STOW).
	 */
	private void fillDestinationsStatus(List<DestinationEcho> destinationEchos, ForwardDicomNode sourceNode,
			List<ForwardDestination> destinations) {
		destinations.forEach(destination -> {
			if (destination instanceof DicomForwardDestination dicomDestination) {
				DicomNode calledNode = dicomDestination.getStreamSCU().getCalledNode();
				DicomState dicomState = Echo.process(buildEchoProcessParams(), sourceNode, calledNode);
				destinationEchos.add(new DestinationEcho(calledNode.getAet(), null, dicomState.getStatus()));
			}
			else if (destination instanceof WebForwardDestination webDestination) {
				destinationEchos.add(new DestinationEcho(null, webDestination.getRequestURL(), 0));
			}
		});
	}

	private AdvancedParams buildEchoProcessParams() {
		AdvancedParams params = new AdvancedParams();
		ConnectOptions connectOptions = new ConnectOptions();
		connectOptions.setConnectTimeout(3000);
		connectOptions.setAcceptTimeout(5000);
		params.setConnectOptions(connectOptions);
		return params;
	}

}
