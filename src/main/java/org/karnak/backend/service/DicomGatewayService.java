/*
 * Copyright (c) 2009-2019 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.TransferCapability;
import org.karnak.backend.dicom.ForwardDestination;
import org.karnak.backend.dicom.ForwardDicomNode;
import org.karnak.backend.dicom.GatewayParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.weasis.dicom.param.AdvancedParams;
import org.weasis.dicom.param.AttributeEditor;
import org.weasis.dicom.param.DeviceListenerService;
import org.weasis.dicom.param.DicomNode;

@Service
public class DicomGatewayService {

	private final StoreScpForwardService storeScpForwardService;

	private DeviceListenerService deviceService;

	@Autowired
	public DicomGatewayService(final StoreScpForwardService storeScpForwardService) {
		this.storeScpForwardService = storeScpForwardService;
		this.deviceService = null;
	}

	/**
	 * Init a DICOM Gateway with one final destination
	 * @param forwardParams the optional advanced parameters (proxy, authentication,
	 * connection and TLS) for the final destination
	 * @param fwdNode the calling DICOM node configuration
	 * @param destinationNode the final DICOM node configuration
	 * @throws IOException
	 */
	public void init(AdvancedParams forwardParams, ForwardDicomNode fwdNode, DicomNode destinationNode)
			throws IOException {
		init(forwardParams, fwdNode, destinationNode, null);
	}

	/**
	 * Init a DICOM Gateway with one final destination
	 * @param forwardParams the optional advanced parameters (proxy, authentication,
	 * connection and TLS) for the final destination
	 * @param fwdNode the calling DICOM node configuration
	 * @param destinationNode the final DICOM node configuration
	 * @param editors the list of editor for modifying attributes on the fly (can be Null)
	 * @throws IOException
	 */
	public void init(AdvancedParams forwardParams, ForwardDicomNode fwdNode, DicomNode destinationNode,
			List<AttributeEditor> editors) throws IOException {
		storeScpForwardService.init(forwardParams, fwdNode, destinationNode, editors);
		this.deviceService = new DeviceListenerService(storeScpForwardService.getDevice());
	}

	public void init(Map<ForwardDicomNode, List<ForwardDestination>> destinations) throws IOException {
		storeScpForwardService.init(destinations);
		this.deviceService = new DeviceListenerService(storeScpForwardService.getDevice());
	}

	public boolean isRunning() {
		return storeScpForwardService.getConnection().isListening();
	}

	public StoreScpForwardService getStoreScpForward() {
		return storeScpForwardService;
	}

	public void start(DicomNode scpNode) throws Exception {
		start(scpNode, new GatewayParams(false));
	}

	public synchronized void start(DicomNode scpNode, GatewayParams params) throws Exception {
		if (isRunning()) {
			throw new IOException("Cannot start a DICOM Gateway because it is already running.");
		}
		storeScpForwardService.setStatus(0);
		storeScpForwardService.getCstoreSCP().setStatus(0);

		AdvancedParams options = Objects.requireNonNull(params).getParams();
		Connection conn = storeScpForwardService.getConnection();
		if (params.isBindCallingAet()) {
			options.configureBind(storeScpForwardService.getApplicationEntity(), conn, scpNode);
		}
		else {
			options.configureBind(conn, scpNode);
		}
		// configure
		options.configure(conn);
		options.configureTLS(conn, null);

		// Limit the calling AETs
		storeScpForwardService.getApplicationEntity().setAcceptedCallingAETitles(params.getAcceptedCallingAETitles());

		URL transferCapabilityFile = params.getTransferCapabilityFile();
		if (transferCapabilityFile != null) {
			storeScpForwardService.loadDefaultTransferCapability(transferCapabilityFile);
		}
		else {
			storeScpForwardService.getApplicationEntity()
				.addTransferCapability(new TransferCapability(null, "*", TransferCapability.Role.SCP, "*"));
		}

		deviceService.start();
	}

	public synchronized void stop() {
		deviceService.stop();
		storeScpForwardService.stop();
	}

}
