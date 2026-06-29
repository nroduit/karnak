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

import java.io.IOException;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.DimseRSP;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.pdu.AAssociateRJ;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.PresentationContext;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.model.dicom.ConfigNode;
import org.karnak.backend.model.dicom.result.DicomEchoResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.weasis.dicom.param.AdvancedParams;
import org.weasis.dicom.param.ConnectOptions;
import org.weasis.dicom.param.DeviceOpService;
import org.weasis.dicom.param.DicomNode;
import org.weasis.dicom.param.DicomState;

/**
 * Performs a single DICOM C-ECHO and turns the outcome into a structured
 * {@link DicomEchoResult}.
 *
 * <p>
 * Rather than the {@code weasis} {@code Echo.process} convenience wrapper, this drives
 * the association directly through {@link ApplicationEntity#connect} so it can keep the
 * live {@link Association}: that exposes the peer's advertised implementation identity on
 * success, and a decoded {@link AAssociateRJ} reason when the peer rejects the
 * association. Never throws — failures are reported through the returned result.
 */
@Service
@Slf4j
@NullUnmarked
public class DicomEchoCheckService {

	private static final String DEVICE_NAME = "echo-scu";

	private final Duration connectTimeout;

	private final Duration acceptTimeout;

	public DicomEchoCheckService(@Value("${dicom-tools.echo.connect-timeout-ms:3000}") long connectTimeoutMs,
			@Value("${dicom-tools.echo.accept-timeout-ms:5000}") long acceptTimeoutMs) {
		this.connectTimeout = Duration.ofMillis(connectTimeoutMs);
		this.acceptTimeout = Duration.ofMillis(acceptTimeoutMs);
	}

	public DicomEchoResult echo(String callingAET, ConfigNode calledNode) {
		DicomNode calledDicomNode = calledNode.getCalledNode();

		Device device = new Device(DEVICE_NAME);
		Connection conn = new Connection();
		device.addConnection(conn);
		ApplicationEntity ae = new ApplicationEntity(callingAET);
		device.addApplicationEntity(ae);
		ae.addConnection(conn);

		Connection remote = new Connection();
		AAssociateRQ rq = new AAssociateRQ();
		rq.setCallingAET(callingAET);
		rq.addPresentationContext(new PresentationContext(1, UID.Verification, UID.ImplicitVRLittleEndian));

		AdvancedParams options = buildOptions();
		DeviceOpService service = new DeviceOpService(device);
		try {
			options.configureConnect(rq, remote, calledDicomNode);
			options.configureBind(ae, conn, new DicomNode(callingAET));
			options.configure(conn);
			options.configureTLS(conn, remote);
		}
		catch (IOException ex) {
			log.error("Error preparing DICOM echo for node ({})", calledNode.getAet(), ex);
			return DicomEchoResult.builder().unexpectedError(true).unexpectedErrorMessage(ex.getMessage()).build();
		}

		service.start();
		try {
			return performEcho(ae, remote, rq);
		}
		catch (AAssociateRJ rj) {
			log.info("Association rejected by node ({}): {}", calledNode.getAet(), rj.getMessage());
			return DicomEchoResult.builder().rejectionReason(rj.toString()).build();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return DicomEchoResult.builder().unexpectedError(true).unexpectedErrorMessage(ex.getMessage()).build();
		}
		catch (Exception ex) {
			log.error("Error checking DICOM node ({})", calledNode.getAet(), ex);
			return DicomEchoResult.builder().unexpectedError(true).unexpectedErrorMessage(ex.getMessage()).build();
		}
		finally {
			service.stop();
		}
	}

	private AdvancedParams buildOptions() {
		ConnectOptions connectOptions = new ConnectOptions();
		connectOptions.setConnectTimeout((int) connectTimeout.toMillis());
		connectOptions.setAcceptTimeout((int) acceptTimeout.toMillis());

		AdvancedParams options = new AdvancedParams();
		options.setConnectOptions(connectOptions);
		return options;
	}

	private static DicomEchoResult performEcho(ApplicationEntity ae, Connection remote, AAssociateRQ rq)
			throws Exception {
		long connectStart = System.nanoTime();
		Association as = ae.connect(remote, rq);
		long connected = System.nanoTime();
		try {
			// The association is established: the peer's identity is known regardless of
			// the
			// echo outcome.
			DicomEchoResult.DicomEchoResultBuilder result = DicomEchoResult.builder()
				.connectionDuration(Duration.ofNanos(connected - connectStart))
				.remoteImplementationVersionName(as.getRemoteImplVersionName())
				.remoteImplementationClassUid(as.getRemoteImplClassUID());

			if (as.getTransferSyntaxesFor(UID.Verification).isEmpty()) {
				// Association accepted, but the Verification SOP Class was not, so no
				// C-ECHO
				// is possible: the node is reachable and associable but lacks that
				// capability.
				return result.verificationUnsupportedMessage(
						"Association established, but the peer does not support the Verification SOP Class (C-ECHO)")
					.build();
			}

			DimseRSP response = as.cecho();
			response.next();
			long echoed = System.nanoTime();

			int status = response.getCommand().getInt(Tag.Status, Status.UnableToProcess);
			return result.dicomState(new DicomState(status, null, null))
				.executionDuration(Duration.ofNanos(echoed - connected))
				.build();
		}
		finally {
			safeRelease(as);
		}
	}

	private static void safeRelease(Association as) {
		try {
			as.release();
		}
		catch (IOException ex) {
			log.debug("Error releasing the echo association", ex);
		}
	}

}