/*
 * Copyright (c) 2009-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.dicom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.jspecify.annotations.NullUnmarked;
import org.weasis.core.util.annotations.Generated;
import org.weasis.dicom.param.AdvancedParams;
import org.weasis.dicom.param.AttributeEditor;
import org.weasis.dicom.param.DeviceOpService;
import org.weasis.dicom.param.DicomNode;
import org.weasis.dicom.param.DicomProgress;
import org.weasis.dicom.param.DicomState;
import org.weasis.dicom.util.StoreFromStreamSCU;

@Generated()
@NullUnmarked
public class DicomForwardDestination extends ForwardDestination {

	/**
	 * A leased connection from the pool: the SCU to send on, its device service, and the
	 * bookkeeping needed to return it. {@code exclusive} is {@code false} when the pool
	 * was exhausted and the SCU is shared with other in-flight transfers (degrading to
	 * the single-association behaviour).
	 */
	public record ScuLease(StoreFromStreamSCU scu, DeviceOpService service, int slot, boolean exclusive) {
	}

	// One or more SCUs (each its own DICOM association) to the same destination.
	private final List<StoreFromStreamSCU> scuPool;

	private final List<DeviceOpService> servicePool;

	private final boolean[] inUse;

	private int roundRobin;

	@Getter
	private final boolean useDestinationAetForKeyMap;

	private final ForwardDicomNode callingNode;

	@Getter
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
	 * @param useDestinationAetForKeyMap whether the destination AET is used as the
	 * key-map key
	 * @param progress optional progress handler for the transfer
	 * @param editors optional attribute editors applied before forwarding
	 * @throws IOException if the association with the destination cannot be set up
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
		this(id, forwardParams, fwdNode, destinationNode, useDestinationAetForKeyMap, progress, editors,
				outputTransferSyntax, transcodeOnlyUncompressed, 1);
	}

	/**
	 * @param poolSize number of DICOM associations (SCUs) kept for this destination.
	 * {@code 1} preserves the historical single-association behavior; a larger value lets
	 * independent transfers to this destination run in parallel. Note the destination
	 * PACS must allow that many concurrent associations from this calling AE.
	 */
	public DicomForwardDestination(Long id, AdvancedParams forwardParams, ForwardDicomNode fwdNode,
			DicomNode destinationNode, boolean useDestinationAetForKeyMap, DicomProgress progress,
			List<AttributeEditor> editors, String outputTransferSyntax, boolean transcodeOnlyUncompressed, int poolSize)
			throws IOException {
		super(id, editors);
		this.callingNode = fwdNode;
		this.destinationNode = destinationNode;
		int size = Math.max(1, poolSize);
		this.scuPool = new ArrayList<>(size);
		this.servicePool = new ArrayList<>(size);
		this.inUse = new boolean[size];
		for (int i = 0; i < size; i++) {
			// Only the first SCU keeps the supplied progress handler; the others get
			// their own state.
			StoreFromStreamSCU scu = new StoreFromStreamSCU(forwardParams, fwdNode, destinationNode,
					i == 0 ? progress : null);
			scuPool.add(scu);
			servicePool.add(new DeviceOpService(scu.getDevice()));
		}
		this.useDestinationAetForKeyMap = useDestinationAetForKeyMap;
		setOutputTransferSyntax(outputTransferSyntax);
		setTranscodeOnlyUncompressed(transcodeOnlyUncompressed);
	}

	/**
	 * Leases an SCU for one transfer. Returns a free association exclusively when one is
	 * available; otherwise (pool exhausted) shares one round-robin so a transfer is never
	 * blocked. Always pair with {@link #release(ScuLease)} in a finally block.
	 */
	public synchronized ScuLease acquire() {
		for (int i = 0; i < inUse.length; i++) {
			if (!inUse[i]) {
				inUse[i] = true;
				return new ScuLease(scuPool.get(i), servicePool.get(i), i, true);
			}
		}
		int i = roundRobin;
		roundRobin = (roundRobin + 1) % inUse.length;
		return new ScuLease(scuPool.get(i), servicePool.get(i), i, false);
	}

	/** Returns a leased SCU to the pool. No-op for a shared (non-exclusive) lease. */
	public synchronized void release(ScuLease lease) {
		if (lease != null && lease.exclusive()) {
			inUse[lease.slot()] = false;
		}
	}

	/**
	 * All SCUs of the pool (e.g. to pre-negotiate presentation contexts on every
	 * association).
	 */
	public List<StoreFromStreamSCU> getStreamSCUs() {
		return scuPool;
	}

	/** A representative SCU (the first of the pool); use {@link #acquire()} to send. */
	public StoreFromStreamSCU getStreamSCU() {
		return scuPool.getFirst();
	}

	public DeviceOpService getStreamSCUService() {
		return servicePool.getFirst();
	}

	@Override
	public ForwardDicomNode getForwardDicomNode() {
		return callingNode;
	}

	@Override
	public void stop() {
		for (StoreFromStreamSCU scu : scuPool) {
			scu.close(true);
		}
		for (DeviceOpService service : servicePool) {
			service.stop();
		}
	}

	@Override
	public DicomState getState() {
		return scuPool.get(0).getState();
	}

	@Override
	public String toString() {
		return destinationNode.toString();
	}

}
