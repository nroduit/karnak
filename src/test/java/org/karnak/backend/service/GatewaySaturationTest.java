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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.karnak.backend.dicom.DicomForwardDestination;
import org.karnak.backend.dicom.ForwardDicomNode;
import org.springframework.context.ApplicationEventPublisher;
import org.weasis.dicom.param.AttributeEditor;

/**
 * Scale-up / saturation characterisation: drives the gateway past the capacity of a fixed
 * connection pool and reports how it reacts. Because the sender threads stand in for the
 * C-STORE source, the latency they observe under load <em>is</em> the gateway's
 * back-pressure to the source — so the throughput/latency tables below show the
 * saturation knee (throughput plateaus, latency climbs) and prove the reaction is
 * graceful: every instance is still delivered, nothing is dropped, and it never
 * deadlocks.
 *
 * <p>
 * Tagged {@code performance} and skipped by the default build; run with
 * {@code mvn test -Pperformance}. The tables are logged for inspection; only the
 * invariants (full delivery, bounded time) are asserted.
 */
@Tag("performance")
@DisplayNameGeneration(ReplaceUnderscores.class)
@Slf4j
class GatewaySaturationTest extends GatewayItTestSupport {

	private ForwardDicomNode fwdNode;

	private ForwardService forwardService;

	@BeforeEach
	void setUp() {
		fwdNode = new ForwardDicomNode("SOURCE-IT");
		forwardService = new ForwardService(mock(ApplicationEventPublisher.class), null);
	}

	@Test
	void offered_load_ramp_reveals_the_saturation_knee_of_a_fixed_pool() throws Exception {
		int poolSize = 4;
		int instancesPerLevel = 480;
		int[] offeredConcurrency = { 1, 2, 4, 8, 16, 32, 64 };

		log.info("--- saturation ramp: pool={}, {} instances per level ---", poolSize, instancesPerLevel);
		log.info("{}", LoadStats.header());
		for (int concurrency : offeredConcurrency) {
			Scp scp = startScp();
			DicomForwardDestination dest = dicomDestination(fwdNode, scp, poolSize);
			LoadStats stats = runLoad(dest, instancesPerLevel, concurrency, null);

			assertEquals(instancesPerLevel, receivedSopInstanceUids(scp.storageDir()).size(),
					"saturation must not drop instances (offered concurrency=" + concurrency + ")");
			log.info("{}", stats);
		}
	}

	@Test
	void sustained_overload_against_a_slow_destination_absorbs_the_burst_without_loss() throws Exception {
		// The destination is artificially slow (editor sleeps), so offered load far
		// exceeds the
		// pool's drain rate. The gateway must absorb the whole burst, back-pressuring the
		// senders
		// (rising latency) instead of dropping anything or failing.
		int poolSize = 4;
		int instances = 240;
		int concurrency = 32;
		AttributeEditor slowEditor = (attributes, context) -> sleep(4);

		Scp scp = startScp();
		DicomForwardDestination dest = new DicomForwardDestination(scp.port() + 0L, advancedParams(), fwdNode,
				scp.node(), false, null, List.of(slowEditor), null, true, poolSize);
		LoadStats stats = runLoad(dest, instances, concurrency, "slow-destination");

		assertEquals(instances, receivedSopInstanceUids(scp.storageDir()).size(),
				"a saturated slow destination must still receive every instance");
		log.info("--- sustained overload (pool={}, concurrency={}, ~4ms/send destination) ---", poolSize, concurrency);
		log.info("{}", LoadStats.header());
		log.info("{}", stats);
	}

	/**
	 * Forwards {@code instances} to {@code dest} from a pool of {@code concurrency}
	 * sender threads, timing each individual send, then drains and stops the destination.
	 * Returns the throughput and the latency distribution observed by the senders (the
	 * back-pressure they felt).
	 */
	private LoadStats runLoad(DicomForwardDestination dest, int instances, int concurrency, String label)
			throws Exception {
		List<Callable<Long>> jobs = new ArrayList<>(instances);
		for (int i = 0; i < instances; i++) {
			String iuid = "1.2.826.0.1.3680043.8.498." + i;
			byte[] object = serialize(iuid);
			jobs.add(() -> {
				long start = System.nanoTime();
				forwardService.storeMultipleDestination(fwdNode, List.of(dest), params(iuid, object));
				return System.nanoTime() - start;
			});
		}

		long[] latencies = new long[instances];
		ExecutorService pool = Executors.newFixedThreadPool(concurrency);
		long wallStart = System.nanoTime();
		try {
			List<Future<Long>> futures = pool.invokeAll(jobs);
			for (int i = 0; i < futures.size(); i++) {
				latencies[i] = futures.get(i).get(120, TimeUnit.SECONDS);
			}
			dest.stop();
		}
		finally {
			pool.shutdownNow();
		}
		long wallNanos = System.nanoTime() - wallStart;
		return LoadStats.of(label != null ? label : Integer.toString(concurrency), concurrency, instances, wallNanos,
				latencies);
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Throughput and latency percentiles for one load level, formatted as an aligned
	 * table row.
	 */
	private record LoadStats(String label, int concurrency, int instances, long throughputPerSec, long p50Ms,
			long p95Ms, long maxMs) {

		static String header() {
			return String.format("%-16s %6s %10s %8s %8s %8s", "level", "conc", "thrpt/s", "p50ms", "p95ms", "maxms");
		}

		static LoadStats of(String label, int concurrency, int instances, long wallNanos, long[] latencyNanos) {
			long[] sorted = latencyNanos.clone();
			Arrays.sort(sorted);
			double seconds = wallNanos / 1_000_000_000.0;
			long throughput = seconds > 0 ? Math.round(instances / seconds) : 0;
			return new LoadStats(label, concurrency, instances, throughput, percentileMs(sorted, 0.50),
					percentileMs(sorted, 0.95), percentileMs(sorted, 1.0));
		}

		private static long percentileMs(long[] sortedAscending, double quantile) {
			if (sortedAscending.length == 0) {
				return 0;
			}
			int index = (int) Math.ceil(quantile * sortedAscending.length) - 1;
			index = Math.max(0, Math.min(sortedAscending.length - 1, index));
			return Math.round(sortedAscending[index] / 1_000_000.0);
		}

		@Override
		public String toString() {
			return String.format("%-16s %6d %10d %8d %8d %8d", label, concurrency, throughputPerSec, p50Ms, p95Ms,
					maxMs);
		}
	}

}