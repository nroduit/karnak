/*
 * Copyright (c) 2021 Karnak Team and other contributors.
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

		// Fill the list of destinations status
		gatewaySetUpService.getDestinationNode(sourceAet)
			.ifPresent(sourceNode -> fillDestinationsStatus(destinationEchos, sourceNode,
					gatewaySetUpService.getDestinations(sourceNode)));

		return destinationEchos;
	}

	/**
	 * Fill the list of destinations status
	 * @param destinationEchos List to fill
	 * @param sourceNode Source Node
	 * @param destinations Destinations found
	 */
	private void fillDestinationsStatus(List<DestinationEcho> destinationEchos, ForwardDicomNode sourceNode,
			List<ForwardDestination> destinations) {
		destinations.forEach(destination -> {
			// Case DICOM
			if (destination instanceof DicomForwardDestination) {
				DicomNode calledNode = ((DicomForwardDestination) destination).getStreamSCU().getCalledNode();
				// Retrieve the status of the dicom node
				DicomState dicomState = Echo.process(buildEchoProcessParams(3000, 5000), sourceNode, calledNode);
				// Add the destination and its status
				destinationEchos.add(new DestinationEcho(calledNode.getAet(), null, dicomState.getStatus()));
			}
			// Case Stow
			else if (destination instanceof WebForwardDestination) {
				WebForwardDestination d = (WebForwardDestination) destination;
				// Add the destination and its status
				destinationEchos.add(new DestinationEcho(null, d.getRequestURL(), 0));
			}
		});
	}

	/**
	 * Build params for echo process call
	 * @param connectTimeout Connect Timeout
	 * @param acceptTimeout Accept Timeout
	 * @return parameters built
	 */
	private AdvancedParams buildEchoProcessParams(int connectTimeout, int acceptTimeout) {
		AdvancedParams params = new AdvancedParams();
		ConnectOptions connectOptions = new ConnectOptions();
		connectOptions.setConnectTimeout(connectTimeout);
		connectOptions.setAcceptTimeout(acceptTimeout);
		params.setConnectOptions(connectOptions);
		return params;
	}

}
