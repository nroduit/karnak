/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.dicom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.dcm4che3.data.UID;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.TransferCapability;
import org.dcm4che3.net.service.DicomServiceRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.model.dicom.ConfigNode;
import org.karnak.backend.model.dicom.result.DicomCapabilitiesResult;
import org.karnak.backend.model.dicom.result.SopClassCapability;
import org.weasis.dicom.param.DicomNode;

/**
 * Drives {@link DicomCapabilitiesCheckService} against a real in-process SCP advertising
 * a known set of transfer capabilities, asserting the non-invasive probe reports exactly
 * the SOP Classes and transfer syntaxes the peer accepts.
 */
@DisplayNameGeneration(ReplaceUnderscores.class)
class DicomCapabilitiesCheckServiceIntegrationTest {

	private static final String SCP_AET = "CAP_SCP";

	private static final String SCU_AET = "CAP_SCU";

	private Device scp;

	private ExecutorService executor;

	private ScheduledExecutorService scheduledExecutor;

	private int port;

	@BeforeEach
	void start_scp() throws Exception {
		port = freePort();

		Connection conn = new Connection();
		conn.setHostname("127.0.0.1");
		conn.setPort(port);

		ApplicationEntity ae = new ApplicationEntity(SCP_AET);
		ae.setAssociationAcceptor(true);
		ae.addConnection(conn);
		ae.addTransferCapability(new TransferCapability(null, UID.CTImageStorage, TransferCapability.Role.SCP,
				UID.ImplicitVRLittleEndian, UID.ExplicitVRLittleEndian, UID.JPEG2000Lossless));
		ae.addTransferCapability(new TransferCapability(null, UID.StudyRootQueryRetrieveInformationModelFind,
				TransferCapability.Role.SCP, UID.ImplicitVRLittleEndian));

		Device device = new Device("capabilities-scp");
		device.addConnection(conn);
		device.addApplicationEntity(ae);
		device.setDimseRQHandler(new DicomServiceRegistry());

		executor = Executors.newCachedThreadPool();
		scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
		device.setExecutor(executor);
		device.setScheduledExecutor(scheduledExecutor);
		device.bindConnections();

		scp = device;
	}

	@AfterEach
	void stop_scp() {
		if (scp != null) {
			scp.unbindConnections();
		}
		if (scheduledExecutor != null) {
			scheduledExecutor.shutdownNow();
		}
		if (executor != null) {
			executor.shutdownNow();
		}
	}

	private static int freePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}

	private static ConfigNode node(String calledAet, int port) {
		return new ConfigNode("test-node", new DicomNode(calledAet, "127.0.0.1", port));
	}

	@Test
	void probe_reports_accepted_sop_classes_and_transfer_syntaxes() {
		DicomCapabilitiesCheckService service = new DicomCapabilitiesCheckService(3000, 5000);

		DicomCapabilitiesResult result = service.probe(SCU_AET, node(SCP_AET, port));

		assertTrue(result.isAssociated());
		assertFalse(result.isRejected());
		assertTrue(result.getMaxPduLength() > 0);
		assertNotNull(result.getRemoteImplementationClassUid());

		SopClassCapability ct = result.getCapabilities()
			.stream()
			.filter((capability) -> capability.sopClassUid().equals(UID.CTImageStorage))
			.findFirst()
			.orElseThrow();
		assertEquals("Storage", ct.category());
		assertTrue(ct.transferSyntaxes().contains(UID.nameOf(UID.ImplicitVRLittleEndian)));
		assertTrue(ct.transferSyntaxes().contains(UID.nameOf(UID.JPEG2000Lossless)));
		// A transfer syntax the peer does not accept must not be reported.
		assertFalse(ct.transferSyntaxes().contains(UID.nameOf(UID.RLELossless)));

		assertTrue(result.getCapabilities()
			.stream()
			.anyMatch((capability) -> capability.sopClassUid().equals(UID.StudyRootQueryRetrieveInformationModelFind)));
	}

	@Test
	void unknown_called_aet_is_reported_as_rejection() {
		DicomCapabilitiesCheckService service = new DicomCapabilitiesCheckService(3000, 5000);

		DicomCapabilitiesResult result = service.probe(SCU_AET, node("WRONG_AET", port));

		assertTrue(result.isRejected());
		assertFalse(result.isAssociated());
		assertNotNull(result.getRejectionReason());
	}

}