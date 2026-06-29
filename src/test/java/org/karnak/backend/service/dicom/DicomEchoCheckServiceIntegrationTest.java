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
import org.dcm4che3.net.service.BasicCEchoSCP;
import org.dcm4che3.net.service.DicomServiceRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.model.dicom.ConfigNode;
import org.karnak.backend.model.dicom.result.DicomEchoResult;
import org.weasis.dicom.param.DicomNode;

/**
 * Drives {@link DicomEchoCheckService} against a real in-process SCP to exercise the
 * association-level diagnostics: the peer implementation identity captured on success,
 * the decoded rejection reason when the called AE Title is not recognized, and the
 * distinct outcome when the peer associates but does not support the Verification SOP
 * Class.
 */
@DisplayNameGeneration(ReplaceUnderscores.class)
class DicomEchoCheckServiceIntegrationTest {

	private static final String SCP_AET = "ECHO_SCP";

	private static final String SCU_AET = "ECHO_SCU";

	private Device scp;

	private ExecutorService executor;

	private ScheduledExecutorService scheduledExecutor;

	@BeforeEach
	void start_executors() {
		executor = Executors.newCachedThreadPool();
		scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
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

	/** Starts the in-process SCP and returns the bound port. */
	private int startScp(boolean withVerification) throws Exception {
		int port = freePort();

		Connection conn = new Connection();
		conn.setHostname("127.0.0.1");
		conn.setPort(port);

		ApplicationEntity ae = new ApplicationEntity(SCP_AET);
		ae.setAssociationAcceptor(true);
		ae.addConnection(conn);
		// Without the Verification capability, the peer still accepts the association but
		// rejects the Verification presentation context.
		String sopClass = withVerification ? UID.Verification : UID.SecondaryCaptureImageStorage;
		ae.addTransferCapability(
				new TransferCapability(null, sopClass, TransferCapability.Role.SCP, UID.ImplicitVRLittleEndian));

		Device device = new Device("echo-scp");
		device.addConnection(conn);
		device.addApplicationEntity(ae);

		DicomServiceRegistry registry = new DicomServiceRegistry();
		registry.addDicomService(new BasicCEchoSCP());
		device.setDimseRQHandler(registry);

		device.setExecutor(executor);
		device.setScheduledExecutor(scheduledExecutor);
		device.bindConnections();

		scp = device;
		return port;
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
	void successful_echo_reports_status_and_peer_identity() throws Exception {
		int port = startScp(true);
		DicomEchoCheckService service = new DicomEchoCheckService(3000, 5000);

		DicomEchoResult result = service.echo(SCU_AET, node(SCP_AET, port));

		assertTrue(result.isSuccessful(), "echo should succeed against a local C-ECHO SCP");
		assertFalse(result.isRejected());
		assertFalse(result.isVerificationUnsupported());
		assertEquals("0000", result.getDicomStatusInHex());
		assertNotNull(result.getConnectionDurationInMs());
		// Implementation Class UID is a mandatory A-ASSOCIATE sub-item, so it is always
		// advertised by the peer.
		assertNotNull(result.getRemoteImplementationClassUid());
	}

	@Test
	void unknown_called_aet_is_reported_as_association_rejection() throws Exception {
		int port = startScp(true);
		DicomEchoCheckService service = new DicomEchoCheckService(3000, 5000);

		DicomEchoResult result = service.echo(SCU_AET, node("WRONG_AET", port));

		assertTrue(result.isRejected(), "an unknown called AE Title should be rejected by the SCP");
		assertFalse(result.isSuccessful());
		assertNotNull(result.getRejectionReason());
	}

	@Test
	void association_without_verification_capability_is_reported_distinctly() throws Exception {
		int port = startScp(false);
		DicomEchoCheckService service = new DicomEchoCheckService(3000, 5000);

		DicomEchoResult result = service.echo(SCU_AET, node(SCP_AET, port));

		assertTrue(result.isVerificationUnsupported(),
				"an associable peer that does not accept Verification should be reported distinctly");
		assertFalse(result.isSuccessful());
		assertFalse(result.isRejected());
		assertNotNull(result.getVerificationUnsupportedMessage());
		// The association still completed, so the peer identity is captured.
		assertNotNull(result.getRemoteImplementationClassUid());
	}

}