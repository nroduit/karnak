/*
 * Copyright (c) 2009-2019 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.dicom;

import java.io.IOException;
import java.util.List;
import org.weasis.dicom.param.AdvancedParams;
import org.weasis.dicom.param.AttributeEditor;
import org.weasis.dicom.param.DeviceOpService;
import org.weasis.dicom.param.DicomNode;
import org.weasis.dicom.param.DicomProgress;
import org.weasis.dicom.param.DicomState;
import org.weasis.dicom.util.StoreFromStreamSCU;

public class DicomForwardDestination extends ForwardDestination {

	private final StoreFromStreamSCU streamSCU;

	private final DeviceOpService streamSCUService;

	private final boolean useDestinationAetForKeyMap;

	private final ForwardDicomNode callingNode;

	private final DicomNode destinationNode;

	public DicomForwardDestination(ForwardDicomNode fwdNode, DicomNode destinationNode) throws IOException {
		this(null, fwdNode, destinationNode, null);
	}

	public DicomForwardDestination(AdvancedParams forwardParams, ForwardDicomNode fwdNode, DicomNode destinationNode)
			throws IOException {
		this(forwardParams, fwdNode, destinationNode, null);
	}

	public DicomForwardDestination(AdvancedParams forwardParams, ForwardDicomNode fwdNode, DicomNode destinationNode,
			List<AttributeEditor> editors) throws IOException {
		this(forwardParams, fwdNode, destinationNode, false, null, editors);
	}

	/**
	 * @param forwardParams optional advanced parameters (proxy, authentication,
	 * connection and TLS)
	 * @param fwdNode the DICOM forwarding node. Cannot be null.
	 * @param destinationNode the DICOM destination node. Cannot be null.
	 * @param useDestinationAetForKeyMap
	 * @param progress
	 * @param editors
	 * @throws IOException
	 */
	public DicomForwardDestination(AdvancedParams forwardParams, ForwardDicomNode fwdNode, DicomNode destinationNode,
			boolean useDestinationAetForKeyMap, DicomProgress progress, List<AttributeEditor> editors)
			throws IOException {
		this(null, forwardParams, fwdNode, destinationNode, useDestinationAetForKeyMap, progress, editors, null, true);
	}

	public DicomForwardDestination(Long id, AdvancedParams forwardParams, ForwardDicomNode fwdNode,
			DicomNode destinationNode, boolean useDestinationAetForKeyMap, DicomProgress progress,
			List<AttributeEditor> editors, String outputTransferSyntax, boolean transcodeOnlyUncompressed)
			throws IOException {
		super(id, editors);
		this.callingNode = fwdNode;
		this.destinationNode = destinationNode;
		this.streamSCU = new StoreFromStreamSCU(forwardParams, fwdNode, destinationNode, progress);
		this.streamSCUService = new DeviceOpService(streamSCU.getDevice());
		this.useDestinationAetForKeyMap = useDestinationAetForKeyMap;
		setOutputTransferSyntax(outputTransferSyntax);
		setTranscodeOnlyUncompressed(transcodeOnlyUncompressed);
	}

	public StoreFromStreamSCU getStreamSCU() {
		return streamSCU;
	}

	public DeviceOpService getStreamSCUService() {
		return streamSCUService;
	}

	public boolean isUseDestinationAetForKeyMap() {
		return useDestinationAetForKeyMap;
	}

	@Override
	public ForwardDicomNode getForwardDicomNode() {
		return callingNode;
	}

	public DicomNode getDestinationNode() {
		return destinationNode;
	}

	@Override
	public void stop() {
		// Association as = streamSCU.getAssociation();
		// if (as != null && as.isReadyForDataTransfer()) {
		// as.abort();
		// }
		streamSCU.close(true);
		streamSCUService.stop();
	}

	@Override
	public DicomState getState() {
		return streamSCU.getState();
	}

	@Override
	public String toString() {
		return destinationNode.toString();
	}

}
