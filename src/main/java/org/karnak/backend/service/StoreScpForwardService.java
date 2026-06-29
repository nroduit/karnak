/*
 * Copyright (c) 2009-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.TransferCapability;
import org.dcm4che3.net.service.BasicCEchoSCP;
import org.dcm4che3.net.service.DicomServiceRegistry;
import org.dcm4che3.tool.common.CLIUtils;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.dicom.DicomForwardDestination;
import org.karnak.backend.dicom.ForwardDestination;
import org.karnak.backend.dicom.ForwardDicomNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.weasis.core.util.annotations.Generated;
import org.weasis.dicom.param.AdvancedParams;
import org.weasis.dicom.param.AttributeEditor;
import org.weasis.dicom.param.DicomNode;

@Service
@Slf4j
@Generated()
@NullUnmarked
public class StoreScpForwardService {

	@Getter
	private final Device device;

	private final ApplicationEntity ae;

	private final Connection conn;

	private volatile int priority;

	@Setter
	private volatile int status;

	private Map<ForwardDicomNode, List<ForwardDestination>> destinations;

	private final CStoreSCPService cStoreSCPService;

	@Autowired
	public StoreScpForwardService(final CStoreSCPService cStoreSCPService) {
		this.cStoreSCPService = cStoreSCPService;
		this.destinations = null;
		this.status = 0;
		this.priority = 0;
		this.conn = new Connection();
		this.ae = new ApplicationEntity("*");
		this.device = new Device("storescp");
	}

	/**
	 * Init service
	 * @param forwardParams the optional advanced parameters (proxy, authentication,
	 * connection and TLS) for the final destination
	 * @param fwdNode the calling DICOM node configuration
	 * @param destinationNode the final DICOM node configuration
	 * @param attributesEditor the editor for modifying attributes on the fly (can be
	 * Null)
	 */
	public void init(AdvancedParams forwardParams, ForwardDicomNode fwdNode, DicomNode destinationNode,
			List<AttributeEditor> attributesEditor) throws IOException {
		DicomForwardDestination uniqueDestination = new DicomForwardDestination(forwardParams, fwdNode, destinationNode,
				attributesEditor);
		this.destinations = new HashMap<>();
		destinations.put(fwdNode, List.of(uniqueDestination));
		cStoreSCPService.init(destinations);
		configureDevice();
	}

	public void init(Map<ForwardDicomNode, List<ForwardDestination>> destinations) {
		this.destinations = Objects.requireNonNull(destinations);
		cStoreSCPService.init(destinations);
		configureDevice();
	}

	private void configureDevice() {
		device.setDimseRQHandler(createServiceRegistry());
		device.addConnection(conn);
		device.addApplicationEntity(ae);
		ae.setAssociationAcceptor(true);
		ae.addConnection(conn);
	}

	public final void setPriority(int priority) {
		this.priority = priority;
	}

	private DicomServiceRegistry createServiceRegistry() {
		DicomServiceRegistry serviceRegistry = new DicomServiceRegistry();
		serviceRegistry.addDicomService(new BasicCEchoSCP());
		serviceRegistry.addDicomService(cStoreSCPService);
		return serviceRegistry;
	}

	public void loadDefaultTransferCapability(URL transferCapabilityFile) {
		Properties p = new Properties();

		try (InputStream in = transferCapabilityFile != null ? transferCapabilityFile.openStream()
				: getClass().getResourceAsStream("sop-classes.properties")) {
			p.load(in);
		}
		catch (IOException e) {
			log.error("Cannot read sop-classes", e);
		}

		for (String cuid : p.stringPropertyNames()) {
			String ts = p.getProperty(cuid);
			TransferCapability tc = new TransferCapability(null, CLIUtils.toUID(cuid), TransferCapability.Role.SCP,
					CLIUtils.toUIDs(ts));
			ae.addTransferCapability(tc);
		}
	}

	public ApplicationEntity getApplicationEntity() {
		return ae;
	}

	public Connection getConnection() {
		return conn;
	}

	public void stop() {
		destinations.values().forEach(l -> l.forEach(ForwardDestination::stop));
		cStoreSCPService.getDestinations().values().forEach(l -> l.forEach(ForwardDestination::stop));
	}

	public CStoreSCPService getCstoreSCP() {
		return cStoreSCPService;
	}

}
