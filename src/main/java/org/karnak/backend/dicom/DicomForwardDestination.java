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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
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

	/**
	 * One or more SCUs (each its own DICOM association) to the same destination, all
	 * calling with the same AE Title.
	 */
	private static final class ScuPool {

		private final List<StoreFromStreamSCU> scus;

		private final List<DeviceOpService> services;

		private final boolean[] inUse;

		// Leases handed out and not yet returned, shared ones included
		private int leases;

		private int roundRobin;

		private ScuPool(AdvancedParams forwardParams, DicomNode callingNode, DicomNode destinationNode,
				DicomProgress progress, int size) throws IOException {
			this.scus = new ArrayList<>(size);
			this.services = new ArrayList<>(size);
			this.inUse = new boolean[size];
			for (int i = 0; i < size; i++) {
				// Only the first SCU keeps the supplied progress handler; the others get
				// their own state.
				StoreFromStreamSCU scu = new StoreFromStreamSCU(forwardParams, callingNode, destinationNode,
						i == 0 ? progress : null);
				this.scus.add(scu);
				this.services.add(new DeviceOpService(scu.getDevice()));
			}
		}

		private ScuLease acquire() {
			this.leases++;
			for (int i = 0; i < this.inUse.length; i++) {
				if (!this.inUse[i]) {
					this.inUse[i] = true;
					return new ScuLease(this.scus.get(i), this.services.get(i), i, true);
				}
			}
			int i = this.roundRobin;
			this.roundRobin = (this.roundRobin + 1) % this.inUse.length;
			return new ScuLease(this.scus.get(i), this.services.get(i), i, false);
		}

		private void release(ScuLease lease) {
			this.leases--;
			if (lease.exclusive()) {
				this.inUse[lease.slot()] = false;
			}
		}

		private boolean owns(ScuLease lease) {
			return lease.slot() < this.scus.size() && this.scus.get(lease.slot()) == lease.scu();
		}

		private boolean isIdle() {
			return this.leases == 0;
		}

		private void stop() {
			for (StoreFromStreamSCU scu : this.scus) {
				scu.close(true);
			}
			for (DeviceOpService service : this.services) {
				service.stop();
			}
		}

	}

	// Pool calling with the forward node AE Title
	private final ScuPool defaultPool;

	/**
	 * Upper bound of the pools kept for source AE Titles. Beyond it, the least recently
	 * used idle pool is closed when a new source shows up, so a destination fed by many
	 * sources does not keep one set of associations per source forever.
	 */
	static final int MAX_SOURCE_POOLS = 32;

	// Pools calling with the source AE Title, created on first use for each source, in
	// access order for the eviction
	private final Map<String, ScuPool> sourcePools = new LinkedHashMap<>(16, 0.75f, true);

	private final AdvancedParams forwardParams;

	private final int poolSize;

	@Getter
	private boolean useSourceAet;

	/**
	 * How long a forwarded C-STORE waits for this destination's C-STORE-RSP, in seconds.
	 * Null when the destination has no value of its own: the global
	 * {@code forward.cstore-response-timeout-seconds} then applies. Some archives (e.g.
	 * Carestream Vue PACS) allow a single outstanding operation per association and
	 * answer only once the object is fully processed, which can exceed the default.
	 */
	@Getter
	@Setter
	private Integer cstoreResponseTimeoutSeconds;

	// Applied to every SCU, including those of the source pools created later
	private Consumer<StoreFromStreamSCU> scuInitializer = _ -> {
	};

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
		this(forwardParams, fwdNode, destinationNode, null, editors);
	}

	/**
	 * @param forwardParams optional advanced parameters (proxy, authentication,
	 * connection and TLS)
	 * @param fwdNode the DICOM forwarding node. Cannot be null.
	 * @param destinationNode the DICOM destination node. Cannot be null.
	 * @param progress optional progress handler for the transfer
	 * @param editors optional attribute editors applied before forwarding
	 * @throws IOException if the association with the destination cannot be set up
	 */
	public DicomForwardDestination(AdvancedParams forwardParams, ForwardDicomNode fwdNode, DicomNode destinationNode,
			DicomProgress progress, List<AttributeEditor> editors) throws IOException {
		this(null, forwardParams, fwdNode, destinationNode, progress, editors, null, true);
	}

	public DicomForwardDestination(Long id, AdvancedParams forwardParams, ForwardDicomNode fwdNode,
			DicomNode destinationNode, DicomProgress progress, List<AttributeEditor> editors,
			String outputTransferSyntax, boolean transcodeOnlyUncompressed) throws IOException {
		this(id, forwardParams, fwdNode, destinationNode, progress, editors, outputTransferSyntax,
				transcodeOnlyUncompressed, 1);
	}

	/**
	 * @param poolSize number of DICOM associations (SCUs) kept for this destination.
	 * {@code 1} preserves the historical single-association behavior; a larger value lets
	 * independent transfers to this destination run in parallel. Note the destination
	 * PACS must allow that many concurrent associations from this calling AE.
	 */
	public DicomForwardDestination(Long id, AdvancedParams forwardParams, ForwardDicomNode fwdNode,
			DicomNode destinationNode, DicomProgress progress, List<AttributeEditor> editors,
			String outputTransferSyntax, boolean transcodeOnlyUncompressed, int poolSize) throws IOException {
		super(id, editors);
		this.callingNode = fwdNode;
		this.destinationNode = destinationNode;
		this.forwardParams = forwardParams;
		this.poolSize = Math.max(1, poolSize);
		this.defaultPool = new ScuPool(forwardParams, fwdNode, destinationNode, progress, this.poolSize);
		setOutputTransferSyntax(outputTransferSyntax);
		setTranscodeOnlyUncompressed(transcodeOnlyUncompressed);
	}

	/**
	 * Leases an SCU calling with the forward node AE Title. Returns a free association
	 * exclusively when one is available; otherwise (pool exhausted) shares one
	 * round-robin so a transfer is never blocked. Always pair with
	 * {@link #release(ScuLease)} in a finally block.
	 */
	public synchronized ScuLease acquire() {
		return this.defaultPool.acquire();
	}

	/**
	 * Leases an SCU for a transfer received from {@code sourceAet}. When
	 * {@link #isUseSourceAet()} is set, the association calls the destination with the
	 * source AE Title (each source gets its own pool of associations); otherwise, or when
	 * the source AE Title is unknown, it behaves like {@link #acquire()}.
	 * @throws IOException if the associations for a new source cannot be set up
	 */
	public synchronized ScuLease acquire(String sourceAet) throws IOException {
		if (!this.useSourceAet || sourceAet == null || sourceAet.isBlank()) {
			return this.defaultPool.acquire();
		}
		ScuPool pool = this.sourcePools.get(sourceAet);
		if (pool == null) {
			evictIdleSourcePools();
			pool = new ScuPool(this.forwardParams, new DicomNode(sourceAet), this.destinationNode, null, this.poolSize);
			pool.scus.forEach(this.scuInitializer);
			this.sourcePools.put(sourceAet, pool);
		}
		return pool.acquire();
	}

	/**
	 * Makes room for a new source pool: closes the least recently used pools that have no
	 * lease out until the bound is respected. A pool still in use is never closed, so the
	 * bound may be exceeded under load.
	 */
	private void evictIdleSourcePools() {
		Iterator<ScuPool> pools = this.sourcePools.values().iterator();
		while (this.sourcePools.size() >= MAX_SOURCE_POOLS && pools.hasNext()) {
			ScuPool pool = pools.next();
			if (pool.isIdle()) {
				pools.remove();
				pool.stop();
			}
		}
	}

	/** Returns a leased SCU to its pool. */
	public synchronized void release(ScuLease lease) {
		if (lease == null) {
			return;
		}
		if (this.defaultPool.owns(lease)) {
			this.defaultPool.release(lease);
			return;
		}
		for (ScuPool pool : this.sourcePools.values()) {
			if (pool.owns(lease)) {
				pool.release(lease);
				return;
			}
		}
	}

	public synchronized void setUseSourceAet(boolean useSourceAet) {
		this.useSourceAet = useSourceAet;
	}

	/**
	 * Sets an action applied to every SCU of the destination (e.g. to pre-negotiate
	 * presentation contexts on every association): immediately to the existing ones, and
	 * to those created later for a new source AE Title.
	 */
	public synchronized void setScuInitializer(Consumer<StoreFromStreamSCU> scuInitializer) {
		this.scuInitializer = scuInitializer;
		this.defaultPool.scus.forEach(scuInitializer);
		this.sourcePools.values().forEach(pool -> pool.scus.forEach(scuInitializer));
	}

	/** All SCUs of the pool calling with the forward node AE Title. */
	public List<StoreFromStreamSCU> getStreamSCUs() {
		return this.defaultPool.scus;
	}

	/** A representative SCU (the first of the pool); use {@link #acquire()} to send. */
	public StoreFromStreamSCU getStreamSCU() {
		return this.defaultPool.scus.getFirst();
	}

	public DeviceOpService getStreamSCUService() {
		return this.defaultPool.services.getFirst();
	}

	@Override
	public ForwardDicomNode getForwardDicomNode() {
		return this.callingNode;
	}

	@Override
	public synchronized void stop() {
		this.defaultPool.stop();
		this.sourcePools.values().forEach(ScuPool::stop);
	}

	@Override
	public DicomState getState() {
		return this.defaultPool.scus.getFirst().getState();
	}

	@Override
	public String toString() {
		return this.destinationNode.toString();
	}

}
