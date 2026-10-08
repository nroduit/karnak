/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import com.sun.management.OperatingSystemMXBean;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.Serial;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.PDVInputStream;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicCStoreSCP;
import org.dcm4che3.net.service.DicomServiceException;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.repo.DestinationRepo;
import org.karnak.backend.dicom.ForwardDestination;
import org.karnak.backend.dicom.ForwardDicomNode;
import org.karnak.backend.dicom.Params;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.weasis.core.util.annotations.Generated;
import org.weasis.dicom.param.DicomNode;

@Service
@Slf4j
@Generated()
@NullUnmarked
public class CStoreSCPService extends BasicCStoreSCP {

	// Service
	private final DestinationRepo destinationRepo;

	private final ForwardService forwardService;

	@Setter
	@Getter
	private Map<ForwardDicomNode, List<ForwardDestination>> destinations;

	@Setter
	@Getter
	private volatile int priority;

	@Setter
	@Getter
	private volatile int status;

	// Single-flight guard for the transfer-status window: the C-STORE that wins it
	// schedules the DB writes, every other one skips straight to the transfer
	private final AtomicBoolean transferStatusPending = new AtomicBoolean();

	// Runs the transfer-status DB writes off the C-STORE threads
	private final ScheduledExecutorService executorService;

	// Maximum number of C-STORE transfers processed concurrently; 0 means auto:
	// the limit scales with the resources actually available, between 2 x CPU
	// cores (minimum 8) and 8 x CPU cores depending on the heap headroom. Any
	// positive value fixes the limit and disables the adaptive controller
	// entirely, which is also how it is taken out of the picture when measuring.
	@Value("${gateway.max-concurrent-transfers:0}")
	private int maxConcurrentTransfers;

	// How long an incoming transfer waits for a permit before being refused with
	// A700 (out of resources). While it waits, the sender sees no PDV consumed,
	// and most SCUs abort on their own timeout well before that - the waiter is
	// then parked on a dead association until this expires (its reader thread is
	// the one waiting, so the abort goes unread) - hence a value near the
	// senders' timeouts.
	@Value("${gateway.transfer-permit-timeout-seconds:120}")
	private long transferPermitTimeoutSeconds;

	// Seconds between two memory samples (heap and process RSS) of the adaptive
	// controller. This is a memory-safety control, not a scheduler: it only has to
	// react before memory fills, which takes seconds, not microseconds.
	@Value("${gateway.transfer-permit-sample-seconds:5}")
	private long transferPermitSampleSeconds;

	// Fair semaphore: waiting associations get permits in arrival order instead of
	// letting a busy sender starve the others. Fairness routes every acquisition
	// through the AQS queue even when a permit is free, which is a global
	// serialisation point at high object rates; set to false to measure its cost.
	@Value("${gateway.transfer-permit-fair:true}")
	private boolean transferPermitFair;

	// Admission control: bounds how many C-STOREs are processed at the same time.
	// The listener device serves every association with its own thread (virtual by
	// default, see GatewayDeviceListenerService), and a transfer that de-identifies
	// or transcodes holds the whole pixel data of the object in heap, so without a
	// bound the peak heap grows linearly with the number of concurrent clients.
	// Blocking in store() applies backpressure through the DICOM protocol itself:
	// the PDVs of the association are simply not consumed until a permit is
	// available. The permit count is not
	// fixed: it is adjusted between minTransferPermits and maxTransferPermits
	// according to the heap headroom, so the gateway scales up when memory is
	// available and only throttles under actual memory pressure.
	private AdjustableSemaphore transferPermits;

	// Heap usage fractions steering the adaptive permit count: below the grow
	// threshold the limit increases, above the shrink threshold it decreases.
	// They are compared against occupancy *after* collection, never against live
	// occupancy - see sampleHeapAndAdjust.
	private static final double GROW_HEAP_USAGE = 0.60;

	private static final double SHRINK_HEAP_USAGE = 0.80;

	// Post-collection heap occupancy past which the limit drops straight to the
	// floor: by the time several ordinary shrink steps would have run, the
	// heap is full.
	private static final double EMERGENCY_HEAP_USAGE = 0.92;

	// Process-RSS fractions of the memory limit steering the native-memory
	// signal. De-identification and transcoding allocate off-heap (OpenCV mats,
	// direct buffers) and a container OOM-kill acts on RSS, none of which any
	// heap reading can see.
	private static final double SHRINK_RSS_USAGE = 0.90;

	private static final double EMERGENCY_RSS_USAGE = 0.95;

	// Consecutive samples on the same side of a threshold before the limit moves.
	// Growing needs more agreement than shrinking: the controller must not be
	// able to complete a grow/shrink cycle inside one GC cycle (that turns
	// ordinary sawtooth into permit flapping, and a fair semaphore whose permit
	// count keeps collapsing stalls every association at once), while a shrink is
	// the memory-safety action - shedding permits reclaims nothing from admitted
	// transfers, so it has to outrun the pressure it reacts to.
	private static final int GROW_HYSTERESIS_SAMPLES = 3;

	private static final int SHRINK_HYSTERESIS_SAMPLES = 2;

	// Mutated only by the single-threaded sampler; volatile for readers.
	private volatile int permitLimit;

	private int minTransferPermits;

	// Ceiling of the adaptive range (or the fixed limit); also the basis for the
	// auto association cap in StoreScpForwardService
	@Getter
	private int maxTransferPermits;

	private int permitStep;

	// Sampler state, confined to the sampler thread
	private int consecutiveGrowSamples;

	private int consecutiveShrinkSamples;

	private long lastCollectionCount = -1;

	private boolean collectionUsageUnavailableLogged;

	private boolean procRssUnavailable;

	// Cgroup-aware memory limit provider for the RSS signal: getTotalMemorySize
	// is the container limit when one is set, the physical RAM otherwise. Null on
	// a JVM that does not expose the com.sun.management interface.
	private final OperatingSystemMXBean osBean = platformOsBean();

	private static final Path PROC_SELF_STATUS = Path.of("/proc/self/status");

	// Dedicated one-shot scheduler for the heap sampler. Not the executorService
	// above: that one runs the transfer-status writes, and a slow DB round trip
	// there must not delay the memory-safety control.
	private ScheduledExecutorService permitSampler;

	@Autowired
	public CStoreSCPService(DestinationRepo destinationRepo, ForwardService forwardService) {
		super("*");
		this.destinationRepo = destinationRepo;
		this.forwardService = forwardService;
		this.destinations = null;
		this.executorService = Executors.newSingleThreadScheduledExecutor();
		this.status = 0;
		this.priority = 0;
	}

	public void init(Map<ForwardDicomNode, List<ForwardDestination>> destinations) {
		this.destinations = destinations;
	}

	@PostConstruct
	void initTransferPermits() {
		int cores = Runtime.getRuntime().availableProcessors();
		if (maxConcurrentTransfers > 0) {
			// Explicit configuration: fixed limit, no adaptation
			this.minTransferPermits = maxConcurrentTransfers;
			this.maxTransferPermits = maxConcurrentTransfers;
		}
		else {
			this.minTransferPermits = Math.max(8, 2 * cores);
			this.maxTransferPermits = Math.max(minTransferPermits, 8 * cores);
		}
		this.permitStep = Math.max(1, cores / 2);
		this.permitLimit = minTransferPermits;
		this.transferPermits = new AdjustableSemaphore(permitLimit, transferPermitFair);

		if (minTransferPermits == maxTransferPermits) {
			log.info("C-STORE transfer permits fixed at {} (fair={})", permitLimit, transferPermitFair);
			return;
		}
		// The limit is adapted off the C-STORE path, on its own thread. Doing it per
		// object meant a global lock plus two Runtime calls on the hottest path in
		// the gateway, for a value that can only meaningfully change once per GC.
		this.permitSampler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "karnak-permit-sampler");
			t.setDaemon(true);
			return t;
		});
		long period = Math.max(1, transferPermitSampleSeconds);
		this.permitSampler.scheduleWithFixedDelay(this::sampleHeapAndAdjust, period, period, TimeUnit.SECONDS);
		log.info("C-STORE transfer permits adaptive between {} and {}, step {}, sampled every {}s (fair={})",
				minTransferPermits, maxTransferPermits, permitStep, period, transferPermitFair);
	}

	@PreDestroy
	void stopPermitSampler() {
		if (permitSampler != null) {
			permitSampler.shutdownNow();
		}
	}

	@Override
	protected void store(Association as, PresentationContext pc, Attributes rq, PDVInputStream data, Attributes rsp)
			throws IOException {
		ForwardDicomNode fwdNode = findForwardNode(as.getCalledAET());
		List<ForwardDestination> destList = destinations.get(fwdNode);
		if (destList == null || destList.isEmpty()) {
			throw new IllegalStateException("No DICOM destinations for " + fwdNode);
		}

		DicomNode callingNode = DicomNode.buildRemoteDicomNode(as);
		Set<DicomNode> srcNodes = fwdNode.getAcceptedSourceNodes();
		boolean valid = srcNodes.isEmpty() || srcNodes.stream()
			.anyMatch(n -> n.getAet().equals(callingNode.getAet())
					&& (!n.isValidateHostname() || n.equalsHostname(callingNode.getHostname())));
		if (!valid) {
			rsp.setInt(Tag.Status, VR.US, Status.NotAuthorized);
			log.error("Refused: not authorized (124H). Source node: {}. SopUID: {}", callingNode,
					rq.getString(Tag.AffectedSOPInstanceUID));
			forwardService.monitorRejected(fwdNode, destList, rq, rq.getString(Tag.AffectedSOPClassUID),
					"Source not authorized: " + callingNode.getAet());
			return;
		}

		rsp.setInt(Tag.Status, VR.US, status);

		acquireTransferPermit(rq);
		try {
			Params p = new Params(rq.getString(Tag.AffectedSOPInstanceUID), rq.getString(Tag.AffectedSOPClassUID),
					pc.getTransferSyntax(), priority, data, as);

			// Update transfer status of destinations
			updateTransferStatus(destList);

			forwardService.storeMultipleDestination(fwdNode, destList, p);

		}
		catch (Exception e) {
			throw new DicomServiceException(Status.ProcessingFailure, e);
		}
		finally {
			transferPermits.release();
		}
	}

	/**
	 * Resolves the forward node addressed by the called AET of the association. Runs on
	 * every C-STORE; a plain loop over the live key set, kept deliberately index-free
	 * because the routing map is mutated in place by configuration reloads and an index
	 * would have to chase it.
	 */
	private ForwardDicomNode findForwardNode(String calledAet) {
		for (ForwardDicomNode node : destinations.keySet()) {
			if (node.getForwardAETitle().equals(calledAet)) {
				return node;
			}
		}
		throw new IllegalStateException("Cannot find the forward AeTitle " + calledAet);
	}

	/**
	 * Blocks until a transfer permit is available (backpressure on the sending
	 * association) or the timeout elapses, in which case the C-STORE is refused with A700
	 * (out of resources) so the sender can retry later.
	 */
	private void acquireTransferPermit(Attributes rq) throws DicomServiceException {
		try {
			if (!transferPermits.tryAcquire(transferPermitTimeoutSeconds, TimeUnit.SECONDS)) {
				log.error("Refused: too many concurrent transfers (A700). SopUID: {}",
						rq.getString(Tag.AffectedSOPInstanceUID));
				throw new DicomServiceException(Status.OutOfResources,
						"No transfer permit within " + transferPermitTimeoutSeconds + "s");
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DicomServiceException(Status.ProcessingFailure, e);
		}
	}

	/**
	 * Adapts the concurrent transfer limit to the memory actually in use. Runs on the
	 * sampler thread, never on a C-STORE. Two signals are watched:
	 * <p>
	 * <b>Process RSS</b> against the memory limit (cgroup-aware), checked first and not
	 * gated on garbage collection: de-identification and transcoding allocate off-heap
	 * (OpenCV mats, direct buffers), a container OOM-kill acts on RSS, and none of that
	 * needs a GC to change. The signal is Linux-only; where it is unavailable the heap
	 * signal alone drives the controller.
	 * <p>
	 * <b>Heap surviving collection</b>: {@link MemoryPoolMXBean#getCollectionUsage()}
	 * summed over the heap pools. Live occupancy ({@code totalMemory - freeMemory}) is
	 * the wrong signal here: it includes everything about to be collected, so under a
	 * generational collector it sweeps through both thresholds on every GC cycle whatever
	 * the real pressure. Reacting to that made the limit oscillate between floor and
	 * ceiling at GC frequency, and because the semaphore can be fair, every shrink queues
	 * the associations behind the permit deficit - all of them stalling together, several
	 * times a second. A heap sample is taken only once the collectors have actually run
	 * since the previous one, so the controller cannot complete a cycle inside one GC
	 * cycle. The one exception is growth on a quiet heap: an interval without any
	 * collection still counts as a grow sample when the live occupancy - an upper bound
	 * of what a collection would leave - is below the grow threshold, otherwise a
	 * throttled gateway that allocates too little to trigger collections could never earn
	 * more permits.
	 * <p>
	 * The response is asymmetric: growing needs {@value #GROW_HYSTERESIS_SAMPLES}
	 * consecutive samples and moves one additive step, shrinking needs
	 * {@value #SHRINK_HYSTERESIS_SAMPLES} and halves the distance to the floor, and past
	 * the emergency thresholds the limit drops to the floor in one move. A shrink never
	 * interrupts admitted transfers; it only reduces the permits available to new ones.
	 */
	private void sampleHeapAndAdjust() {
		try {
			double rssUsage = processRssFraction();
			if (rssUsage > EMERGENCY_RSS_USAGE) {
				dropPermitLimitToFloor("process RSS", rssUsage);
				return;
			}
			if (rssUsage > SHRINK_RSS_USAGE) {
				consecutiveGrowSamples = 0;
				consecutiveShrinkSamples++;
				shrinkPermitLimit("process RSS", rssUsage);
				return;
			}

			long collections = totalCollectionCount();
			if (collections == lastCollectionCount) {
				// No GC since the last sample: getCollectionUsage would return the
				// same reading, and three copies of one measurement are not three
				// samples. It is not "no information" either: a heap that did not
				// need collecting is a heap under no pressure. Growth must not wait
				// for a GC, because a throttled gateway allocates little, collects
				// rarely, and would otherwise never earn the permits that let it
				// allocate more (the deadlock seen as a load test that never ramps
				// past the floor). Live occupancy is an upper bound of what a
				// collection would leave, so below the grow threshold it is safe to
				// grow on; it is never used to shrink, the signal that flaps.
				growOnQuietHeap();
				return;
			}
			lastCollectionCount = collections;

			long liveHeap = liveHeapAfterCollection();
			if (liveHeap < 0) {
				if (!collectionUsageUnavailableLogged) {
					log.warn("No heap pool reports collection usage; the transfer permit limit stays at {}",
							permitLimit);
					collectionUsageUnavailableLogged = true;
				}
				return;
			}
			double heapUsage = (double) liveHeap / Runtime.getRuntime().maxMemory();

			if (heapUsage > EMERGENCY_HEAP_USAGE) {
				dropPermitLimitToFloor("live heap", heapUsage);
			}
			else if (heapUsage < GROW_HEAP_USAGE) {
				consecutiveShrinkSamples = 0;
				consecutiveGrowSamples++;
				growPermitLimit(heapUsage);
			}
			else if (heapUsage > SHRINK_HEAP_USAGE) {
				consecutiveGrowSamples = 0;
				consecutiveShrinkSamples++;
				shrinkPermitLimit("live heap", heapUsage);
			}
			else {
				// Between the thresholds: the current limit is the right one.
				consecutiveGrowSamples = 0;
				consecutiveShrinkSamples = 0;
			}
		}
		catch (RuntimeException e) {
			// The sampler is scheduled with a fixed delay: letting an exception out
			// would cancel it silently and freeze the limit for the process lifetime.
			log.warn("Transfer permit sampling failed, limit left at {}", permitLimit, e);
		}
	}

	/**
	 * Grow decision for a sample interval without any collection. The live occupancy
	 * ({@code totalMemory - freeMemory}) includes garbage not yet collected, so it can
	 * only overstate the pressure: when even that is below the grow threshold, growing is
	 * safe. Above it nothing is known - the garbage may or may not be live - so the
	 * sample is skipped rather than counted either way.
	 */
	private void growOnQuietHeap() {
		Runtime runtime = Runtime.getRuntime();
		double liveUsage = (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory();
		if (liveUsage < GROW_HEAP_USAGE) {
			consecutiveShrinkSamples = 0;
			consecutiveGrowSamples++;
			growPermitLimit(liveUsage);
		}
	}

	private void growPermitLimit(double heapUsage) {
		if (consecutiveGrowSamples >= GROW_HYSTERESIS_SAMPLES && permitLimit < maxTransferPermits) {
			consecutiveGrowSamples = 0;
			int step = Math.min(permitStep, maxTransferPermits - permitLimit);
			permitLimit += step; // NOSONAR single-writer sampler thread
			transferPermits.release(step);
			log.debug("Transfer permit limit raised to {} (live heap {}%)", permitLimit, Math.round(heapUsage * 100));
		}
	}

	/**
	 * Multiplicative decrease: halves the distance to the floor (never less than one
	 * additive step), so a shrink episode outruns the growth it reacts to.
	 */
	private void shrinkPermitLimit(String signal, double usage) {
		if (consecutiveShrinkSamples >= SHRINK_HYSTERESIS_SAMPLES && permitLimit > minTransferPermits) {
			consecutiveShrinkSamples = 0;
			int headroom = permitLimit - minTransferPermits;
			int step = Math.min(headroom, Math.max(permitStep, headroom / 2));
			permitLimit -= step; // NOSONAR single-writer sampler thread
			transferPermits.reduce(step);
			log.debug("Transfer permit limit lowered to {} ({} {}%)", permitLimit, signal, Math.round(usage * 100));
		}
	}

	/**
	 * Emergency shed: back to the floor in one move, because by the time several ordinary
	 * shrink steps would have run the memory is gone. Admitted transfers are never
	 * interrupted; the gateway just stops admitting past the floor.
	 */
	private void dropPermitLimitToFloor(String signal, double usage) {
		consecutiveGrowSamples = 0;
		consecutiveShrinkSamples = 0;
		int deficit = permitLimit - minTransferPermits;
		if (deficit > 0) {
			permitLimit = minTransferPermits;
			transferPermits.reduce(deficit);
			log.warn("Transfer permit limit dropped to floor {} ({} at {}%)", permitLimit, signal,
					Math.round(usage * 100));
		}
	}

	/**
	 * Fraction of the memory limit (container limit when one is set, physical RAM
	 * otherwise) currently resident for this process, or {@code -1} when the signal is
	 * unavailable. RSS is used rather than the OS/cgroup used-memory counters because
	 * those include the page cache, which Karnak's bulk-data spooling fills routinely -
	 * reclaimable memory that would read as permanent pressure.
	 */
	private double processRssFraction() {
		if (procRssUnavailable || osBean == null) {
			return -1;
		}
		long limit = osBean.getTotalMemorySize();
		long rss = readVmRssBytes();
		if (limit <= 0 || rss < 0) {
			return -1;
		}
		return (double) rss / limit;
	}

	/**
	 * Resident set size of this process, from {@code /proc/self/status} (Linux only). The
	 * first failure marks the signal unavailable for the process lifetime instead of
	 * retrying - and logging - on every sample.
	 */
	private long readVmRssBytes() {
		try {
			for (String line : Files.readAllLines(PROC_SELF_STATUS)) {
				if (line.startsWith("VmRSS:")) {
					String[] fields = line.split("\\s+");
					return Long.parseLong(fields[1]) * 1024L;
				}
			}
		}
		catch (IOException | RuntimeException e) {
			// Fall through to the unavailable marking below
		}
		procRssUnavailable = true;
		log.info("Process RSS is unavailable; the transfer permit controller uses heap occupancy only");
		return -1;
	}

	private static OperatingSystemMXBean platformOsBean() {
		try {
			return ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
		}
		catch (RuntimeException e) {
			return null;
		}
	}

	/**
	 * Heap still live after the last collection, summed over the heap pools that report
	 * it. Returns {@code -1} when no pool does, which is the signal to leave the limit
	 * alone rather than fall back to a measurement known to be misleading.
	 */
	private static long liveHeapAfterCollection() {
		long total = -1;
		for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
			if (pool.getType() != MemoryType.HEAP) {
				continue;
			}
			MemoryUsage usage = pool.getCollectionUsage();
			if (usage != null) {
				total = (total < 0 ? 0 : total) + usage.getUsed();
			}
		}
		return total;
	}

	private static long totalCollectionCount() {
		long total = 0;
		for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
			long count = gc.getCollectionCount();
			if (count > 0) {
				total += count;
			}
		}
		return total;
	}

	/**
	 * Flags the destinations as transferring, then schedules clearing the flag after a
	 * delay. Single-flight per window, and both DB round trips run on the status
	 * executor: the calling C-STORE thread holds a transfer permit and must not spend it
	 * on repository calls.
	 */
	private void updateTransferStatus(List<ForwardDestination> destinations) {
		if (transferStatusPending.compareAndSet(false, true)) {
			// The executor is single-threaded, so the set always runs before the clear
			executorService.execute(() -> setTransferStatus(destinations, true));
			executorService.schedule(() -> {
				setTransferStatus(destinations, false);
				transferStatusPending.set(false);
			}, 5, TimeUnit.SECONDS);
		}
	}

	/**
	 * Persists the flag for every destination. Never lets an exception out: the status is
	 * telemetry, and the guard reset scheduled after it must always run.
	 */
	private void setTransferStatus(List<ForwardDestination> destinations, boolean status) {
		try {
			destinations.forEach(d -> updateTransferStatus(d, status));
		}
		catch (RuntimeException e) {
			log.warn("Cannot persist the transfer-in-progress status", e);
		}
	}

	/**
	 * Persists the transfer-in-progress flag for an active destination when it changes.
	 */
	private void updateTransferStatus(ForwardDestination destination, boolean status) {
		destinationRepo.findById(destination.getId()).ifPresent(destinationEntity -> {
			if (destinationEntity.isActivate() && destinationEntity.isTransferInProgress() != status) {
				destinationEntity.setTransferInProgress(status);
				destinationEntity.setLastTransfer(LocalDateTime.now(ZoneId.of("CET")));
				destinationRepo.save(destinationEntity);
			}
		});
	}

	// Semaphore.reducePermits is protected; expose it so the adaptive controller can
	// shrink the limit without interrupting the transfers already admitted
	private static final class AdjustableSemaphore extends Semaphore {

		@Serial
		private static final long serialVersionUID = 1L;

		AdjustableSemaphore(int permits, boolean fair) {
			super(permits, fair);
		}

		void reduce(int reduction) {
			super.reducePermits(reduction);
		}

	}

}
