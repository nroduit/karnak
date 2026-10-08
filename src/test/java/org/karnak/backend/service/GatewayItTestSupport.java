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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.karnak.backend.dicom.DicomForwardDestination;
import org.karnak.backend.dicom.ForwardDicomNode;
import org.karnak.backend.dicom.Params;
import org.weasis.dicom.param.AdvancedParams;
import org.weasis.dicom.param.AttributeEditor;
import org.weasis.dicom.param.ConnectOptions;
import org.weasis.dicom.param.DicomNode;
import org.weasis.dicom.param.ListenerParams;
import org.weasis.dicom.tool.DicomListener;

/**
 * Shared scaffolding for gateway forwarding integration tests. Starts real DICOM Store
 * SCPs (the destination PACS) on loopback ports, builds DICOM objects to forward, and
 * reads back what each SCP actually stored, so a test can assert end-to-end delivery, the
 * connection pool, transfer-syntax negotiation and fan-out behaviour over real
 * associations.
 *
 * <p>
 * SCPs started through {@link #startScp()} are stopped automatically after each test.
 */
abstract class GatewayItTestSupport {

	protected static final String TS_EXPLICIT = UID.ExplicitVRLittleEndian;

	protected static final String TS_IMPLICIT = UID.ImplicitVRLittleEndian;

	protected static final String CUID_SC = UID.SecondaryCaptureImageStorage;

	/**
	 * Mirrors production: the gateway always adds at least one editor (a
	 * {@code StreamRegistryEditor}), so the forward goes through the dataset-reading send
	 * path rather than the raw stream pass-through. A no-op editor stands in for it.
	 */
	protected static final AttributeEditor NOOP = (attributes, context) -> {
	};

	@TempDir
	protected Path workDir;

	private final List<DicomListener> listeners = new ArrayList<>();

	private final AtomicInteger scpCounter = new AtomicInteger();

	/**
	 * A running Store SCP: its AE title, port and the directory it writes received
	 * objects to.
	 */
	protected record Scp(String aet, int port, Path storageDir) {
		DicomNode node() {
			return new DicomNode(aet, "localhost", port);
		}
	}

	/** Starts a Store SCP with a generated AE title on a free loopback port. */
	protected Scp startScp() throws Exception {
		return startScp("KARNAK-IT-SCP-" + scpCounter.incrementAndGet());
	}

	protected Scp startScp(String aet) throws Exception {
		int port = freePort();
		Path dir = Files.createDirectories(workDir.resolve("scp-" + port));
		DicomListener listener = new DicomListener(dir);
		listener.start(new DicomNode(aet, null, port), new ListenerParams(true));
		listeners.add(listener);
		return new Scp(aet, port, dir);
	}

	@AfterEach
	void stopScps() {
		listeners.forEach(DicomListener::stop);
		listeners.clear();
	}

	/**
	 * A single-association DICOM destination (no transcoding) pointing at the given SCP.
	 */
	protected DicomForwardDestination dicomDestination(ForwardDicomNode fwdNode, Scp scp) throws IOException {
		return dicomDestination(fwdNode, scp, 1);
	}

	/**
	 * A DICOM destination with the given connection-pool size pointing at the given SCP.
	 */
	protected DicomForwardDestination dicomDestination(ForwardDicomNode fwdNode, Scp scp, int poolSize)
			throws IOException {
		return new DicomForwardDestination(scp.port() + 0L, advancedParams(), fwdNode, scp.node(), false, null,
				List.of(NOOP), null, true, poolSize);
	}

	protected Params params(String iuid, byte[] object) {
		return params(iuid, CUID_SC, TS_EXPLICIT, object);
	}

	protected Params params(String iuid, String cuid, String tsuid, byte[] object) {
		return new Params(iuid, cuid, tsuid, 0, new ByteArrayInputStream(object), null);
	}

	protected static int freePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}

	protected static AdvancedParams advancedParams() {
		AdvancedParams params = new AdvancedParams();
		ConnectOptions connectOptions = new ConnectOptions();
		connectOptions.setConnectTimeout(5000);
		connectOptions.setAcceptTimeout(7000);
		connectOptions.setMaxOpsInvoked(16);
		connectOptions.setMaxOpsPerformed(16);
		params.setConnectOptions(connectOptions);
		return params;
	}

	/**
	 * A minimal Secondary Capture object (no pixel data) in Explicit VR Little Endian.
	 */
	protected static byte[] serialize(String iuid) throws IOException {
		return serialize(iuid, CUID_SC, TS_EXPLICIT);
	}

	/**
	 * A minimal object serialized as a raw dataset (no file-meta) for the given SOP Class
	 * and transfer syntax, as the C-STORE SCP delivers it to the forwarding pipeline
	 * through a {@code PDVInputStream}.
	 */
	protected static byte[] serialize(String iuid, String cuid, String tsuid) throws IOException {
		Attributes data = new Attributes();
		data.setString(Tag.SOPClassUID, VR.UI, cuid);
		data.setString(Tag.SOPInstanceUID, VR.UI, iuid);
		data.setString(Tag.StudyInstanceUID, VR.UI, "1.2.826.0.1.3680043.8.498.100");
		data.setString(Tag.SeriesInstanceUID, VR.UI, "1.2.826.0.1.3680043.8.498.200");
		data.setString(Tag.PatientID, VR.LO, "IT-PATIENT");
		data.setString(Tag.PatientName, VR.PN, "Integration^Test");
		data.setString(Tag.Modality, VR.CS, "OT");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (DicomOutputStream dos = new DicomOutputStream(baos, tsuid)) {
			dos.writeDataset(null, data);
		}
		return baos.toByteArray();
	}

	/**
	 * SOP Instance UIDs of every DICOM file the SCP wrote under its storage directory.
	 */
	protected static Set<String> receivedSopInstanceUids(Path dir) throws IOException {
		Set<String> received = new TreeSet<>();
		try (Stream<Path> files = Files.walk(dir)) {
			for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator) {
				try (DicomInputStream dis = new DicomInputStream(file.toFile())) {
					received.add(dis.readDataset().getString(Tag.SOPInstanceUID));
				}
			}
		}
		return received;
	}

	/** Stored transfer syntax of every received file, keyed by SOP Instance UID. */
	protected static Map<String, String> receivedTransferSyntaxes(Path dir) throws IOException {
		Map<String, String> result = new HashMap<>();
		try (Stream<Path> files = Files.walk(dir)) {
			for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator) {
				try (DicomInputStream dis = new DicomInputStream(file.toFile())) {
					dis.readFileMetaInformation();
					Attributes data = dis.readDataset();
					result.put(data.getString(Tag.SOPInstanceUID), dis.getTransferSyntax());
				}
			}
		}
		return result;
	}

}