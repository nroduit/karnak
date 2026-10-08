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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.UID;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.pdu.AAssociateRJ;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.PresentationContext;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.model.dicom.ConfigNode;
import org.karnak.backend.model.dicom.result.DicomCapabilitiesResult;
import org.karnak.backend.model.dicom.result.SopClassCapability;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.weasis.dicom.param.AdvancedParams;
import org.weasis.dicom.param.ConnectOptions;
import org.weasis.dicom.param.DeviceOpService;
import org.weasis.dicom.param.DicomNode;

/**
 * Non-invasive DICOM capability probe.
 *
 * <p>
 * Opens a single association proposing a curated set of presentation contexts (SOP Class
 * × transfer syntax) and reads back, from the A-ASSOCIATE-AC, which the peer accepted. No
 * DIMSE message and no pixel data are ever exchanged, so it is safe to run against
 * production nodes: it reports what the node <em>could</em> accept (e.g. whether it will
 * take the SOP classes / transfer syntaxes the gateway forwards), which a bare C-ECHO
 * cannot tell. Never throws — failures are reported through the returned result.
 */
@Service
@Slf4j
@NullUnmarked
public class DicomCapabilitiesCheckService {

	private static final String DEVICE_NAME = "capabilities-scu";

	/** A SOP Class to propose, with the transfer syntaxes to test for it. */
	private record ProbeContext(String category, String sopClassUid, List<String> transferSyntaxes) {
	}

	/** Representative transfer syntaxes proposed for storage SOP Classes. */
	private static final List<String> STORAGE_TRANSFER_SYNTAXES = List.of(UID.ImplicitVRLittleEndian,
			UID.ExplicitVRLittleEndian, UID.JPEGBaseline8Bit, UID.JPEGLosslessSV1, UID.JPEG2000, UID.JPEG2000Lossless,
			UID.RLELossless);

	/** Transfer syntaxes proposed for non-storage (command-only) SOP Classes. */
	private static final List<String> BASIC_TRANSFER_SYNTAXES = List.of(UID.ImplicitVRLittleEndian,
			UID.ExplicitVRLittleEndian);

	private static final List<ProbeContext> PROBE_CONTEXTS = buildProbeContexts();

	private static List<ProbeContext> buildProbeContexts() {
		List<ProbeContext> contexts = new ArrayList<>();

		List<String> storage = List.of(UID.CTImageStorage, UID.MRImageStorage, UID.EnhancedCTImageStorage,
				UID.EnhancedMRImageStorage, UID.UltrasoundImageStorage, UID.ComputedRadiographyImageStorage,
				UID.DigitalXRayImageStorageForPresentation, UID.SecondaryCaptureImageStorage,
				UID.PositronEmissionTomographyImageStorage, UID.NuclearMedicineImageStorage);
		storage.forEach((uid) -> contexts.add(new ProbeContext("Storage", uid, STORAGE_TRANSFER_SYNTAXES)));

		contexts.add(new ProbeContext("Query/Retrieve", UID.StudyRootQueryRetrieveInformationModelFind,
				BASIC_TRANSFER_SYNTAXES));
		contexts.add(new ProbeContext("Query/Retrieve", UID.StudyRootQueryRetrieveInformationModelMove,
				BASIC_TRANSFER_SYNTAXES));
		contexts.add(new ProbeContext("Query/Retrieve", UID.StudyRootQueryRetrieveInformationModelGet,
				BASIC_TRANSFER_SYNTAXES));
		contexts.add(new ProbeContext("Query/Retrieve", UID.PatientRootQueryRetrieveInformationModelFind,
				BASIC_TRANSFER_SYNTAXES));
		contexts.add(new ProbeContext("Worklist", UID.ModalityWorklistInformationModelFind, BASIC_TRANSFER_SYNTAXES));
		contexts.add(new ProbeContext("Storage Commitment", UID.StorageCommitmentPushModel, BASIC_TRANSFER_SYNTAXES));

		return List.copyOf(contexts);
	}

	private final Duration connectTimeout;

	private final Duration acceptTimeout;

	public DicomCapabilitiesCheckService(@Value("${dicom-tools.echo.connect-timeout-ms:3000}") long connectTimeoutMs,
			@Value("${dicom-tools.echo.accept-timeout-ms:5000}") long acceptTimeoutMs) {
		this.connectTimeout = Duration.ofMillis(connectTimeoutMs);
		this.acceptTimeout = Duration.ofMillis(acceptTimeoutMs);
	}

	public DicomCapabilitiesResult probe(String callingAET, ConfigNode calledNode) {
		DicomNode calledDicomNode = calledNode.getCalledNode();

		Device device = new Device(DEVICE_NAME);
		Connection conn = new Connection();
		device.addConnection(conn);
		ApplicationEntity ae = new ApplicationEntity(callingAET);
		device.addApplicationEntity(ae);
		ae.addConnection(conn);

		Connection remote = new Connection();
		AAssociateRQ rq = buildAssociateRq(callingAET);

		AdvancedParams options = buildOptions();
		DeviceOpService service = new DeviceOpService(device);
		try {
			options.configureConnect(rq, remote, calledDicomNode);
			options.configureBind(ae, conn, new DicomNode(callingAET));
			options.configure(conn);
			options.configureTLS(conn, remote);
		}
		catch (IOException ex) {
			log.error("Error preparing capability probe for node ({})", calledNode.getAet(), ex);
			return DicomCapabilitiesResult.builder().unexpectedErrorMessage(ex.getMessage()).build();
		}

		service.start();
		try {
			return readCapabilities(ae, remote, rq);
		}
		catch (AAssociateRJ rj) {
			log.info("Capability probe rejected by node ({}): {}", calledNode.getAet(), rj.getMessage());
			return DicomCapabilitiesResult.builder().rejectionReason(rj.toString()).build();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return DicomCapabilitiesResult.builder().unexpectedErrorMessage(ex.getMessage()).build();
		}
		catch (Exception ex) {
			log.error("Error probing DICOM node ({})", calledNode.getAet(), ex);
			return DicomCapabilitiesResult.builder().unexpectedErrorMessage(ex.getMessage()).build();
		}
		finally {
			service.stop();
		}
	}

	private static AAssociateRQ buildAssociateRq(String callingAET) {
		AAssociateRQ rq = new AAssociateRQ();
		rq.setCallingAET(callingAET);

		int pcid = 1;
		for (ProbeContext probe : PROBE_CONTEXTS) {
			for (String transferSyntax : probe.transferSyntaxes()) {
				rq.addPresentationContext(new PresentationContext(pcid, probe.sopClassUid(), transferSyntax));
				pcid += 2;
			}
		}

		return rq;
	}

	private static DicomCapabilitiesResult readCapabilities(ApplicationEntity ae, Connection remote, AAssociateRQ rq)
			throws Exception {
		Association as = ae.connect(remote, rq);
		try {
			DicomCapabilitiesResult.DicomCapabilitiesResultBuilder result = DicomCapabilitiesResult.builder()
				.associated(true)
				.maxPduLength(as.getAAssociateAC().getMaxPDULength())
				.remoteImplementationVersionName(as.getRemoteImplVersionName())
				.remoteImplementationClassUid(as.getRemoteImplClassUID());

			for (ProbeContext probe : PROBE_CONTEXTS) {
				Set<String> acceptedTransferSyntaxes = as.getTransferSyntaxesFor(probe.sopClassUid());
				if (!acceptedTransferSyntaxes.isEmpty()) {
					List<String> names = acceptedTransferSyntaxes.stream().map(UID::nameOf).sorted().toList();
					result.capability(new SopClassCapability(probe.category(), UID.nameOf(probe.sopClassUid()),
							probe.sopClassUid(), names));
				}
			}

			return result.build();
		}
		finally {
			safeRelease(as);
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

	private static void safeRelease(Association as) {
		try {
			as.release();
		}
		catch (IOException ex) {
			log.debug("Error releasing the capability-probe association", ex);
		}
	}

}