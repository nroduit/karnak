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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.karnak.backend.dicom.DicomForwardDestination;
import org.karnak.backend.dicom.ForwardDestination;
import org.karnak.backend.dicom.ForwardDicomNode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Scale-up matrix: characterises the gateway while sweeping the three structural
 * dimensions of a forwarding deployment independently — connection-pool size, number of
 * sending sources (fan-in), and number of destinations per send (fan-out).
 *
 * <p>
 * Each <em>source</em> is modelled as Karnak configures it: its own
 * {@link ForwardDicomNode} with its own pooled {@link DicomForwardDestination}s
 * converging on shared destination SCPs. Every scenario logs throughput and
 * source-observed latency percentiles; only the invariant (no dropped instances) is
 * asserted, so the numbers don't make CI flaky.
 *
 * <p>
 * Tagged {@code performance}; run with {@code mvn test -Pperformance}.
 */
@Tag("performance")
@DisplayNameGeneration(ReplaceUnderscores.class)
@Slf4j
class GatewayScaleMatrixTest extends GatewayItTestSupport {

	@BeforeAll
	static void table() {
		log.info("--- gateway scale matrix (objects = sends x destinations) ---");
		log.info("{}", ScaleResult.header());
	}

	@ParameterizedTest(name = "poolSize={0}")
	@ValueSource(ints = { 1, 2, 4, 8, 16 })
	void pool_size_sweep(int poolSize) throws Exception {
		log.info("{}", runScenario(poolSize, 1, 1, 400, 16));
	}

	@ParameterizedTest(name = "sources={0}")
	@ValueSource(ints = { 1, 2, 4, 8 })
	void sending_source_sweep(int sources) throws Exception {
		log.info("{}", runScenario(4, sources, 1, 150, 8));
	}

	@ParameterizedTest(name = "destinations={0}")
	@ValueSource(ints = { 1, 2, 4, 8 })
	void destination_count_sweep(int destinations) throws Exception {
		log.info("{}", runScenario(4, 2, destinations, 150, 8));
	}

	/**
	 * Runs one matrix cell: {@code sources} independent forwarding sources, each with a
	 * pool of {@code poolSize} associations to every one of {@code destinations} shared
	 * SCPs, each pushing {@code instancesPerSource} objects from
	 * {@code concurrencyPerSource} threads. Returns the aggregate throughput and the
	 * per-send latency distribution.
	 */
	private ScaleResult runScenario(int poolSize, int sources, int destinations, int instancesPerSource,
			int concurrencyPerSource) throws Exception {
		List<Scp> scps = new ArrayList<>();
		for (int d = 0; d < destinations; d++) {
			scps.add(startScp());
		}

		ForwardService forwardService = parallelForwardService();
		List<DicomForwardDestination> allDestinations = new ArrayList<>();
		List<Callable<Long>> jobs = new ArrayList<>();
		for (int s = 0; s < sources; s++) {
			ForwardDicomNode source = new ForwardDicomNode("SOURCE-IT-" + s);
			List<ForwardDestination> sourceDestinations = new ArrayList<>();
			for (Scp scp : scps) {
				DicomForwardDestination dest = dicomDestination(source, scp, poolSize);
				allDestinations.add(dest);
				sourceDestinations.add(dest);
			}
			for (int i = 0; i < instancesPerSource; i++) {
				String iuid = "1.2.826.0.1.3680043.8.498." + s + "." + i;
				byte[] object = serialize(iuid);
				jobs.add(() -> {
					long start = System.nanoTime();
					forwardService.storeMultipleDestination(source, sourceDestinations, params(iuid, object));
					return System.nanoTime() - start;
				});
			}
		}

		long[] latencies = new long[jobs.size()];
		ExecutorService senders = Executors.newFixedThreadPool(Math.max(1, sources * concurrencyPerSource));
		long wallStart = System.nanoTime();
		try {
			List<Future<Long>> futures = senders.invokeAll(jobs);
			for (int i = 0; i < futures.size(); i++) {
				latencies[i] = futures.get(i).get(120, TimeUnit.SECONDS);
			}
			allDestinations.forEach(DicomForwardDestination::stop);
		}
		finally {
			senders.shutdownNow();
			forwardService.shutdownFanoutExecutor();
		}
		long wallNanos = System.nanoTime() - wallStart;

		for (Scp scp : scps) {
			assertEquals(sources * instancesPerSource, receivedSopInstanceUids(scp.storageDir()).size(),
					"no instance may be dropped (pool=" + poolSize + ", sources=" + sources + ", dst=" + destinations
							+ ")");
		}

		long objects = (long) sources * instancesPerSource * destinations;
		return ScaleResult.of(poolSize, sources, destinations, sources * concurrencyPerSource, objects, wallNanos,
				latencies);
	}

	private static ForwardService parallelForwardService() {
		ForwardService forwardService = new ForwardService(mock(ApplicationEventPublisher.class), null);
		ReflectionTestUtils.setField(forwardService, "parallelFanout", true);
		forwardService.initFanoutExecutor();
		return forwardService;
	}

	/**
	 * One matrix cell: dimensions, aggregate object throughput and per-send latency
	 * percentiles.
	 */
	private record ScaleResult(int poolSize, int sources, int destinations, int concurrency, long objects,
			long objectsPerSec, long p50Ms, long p95Ms, long maxMs) {

		static String header() {
			return String.format("%5s %5s %5s %6s %8s %10s %7s %7s %7s", "pool", "src", "dst", "conc", "objects",
					"objs/s", "p50ms", "p95ms", "maxms");
		}

		static ScaleResult of(int poolSize, int sources, int destinations, int concurrency, long objects,
				long wallNanos, long[] latencyNanos) {
			long[] sorted = latencyNanos.clone();
			Arrays.sort(sorted);
			double seconds = wallNanos / 1_000_000_000.0;
			long throughput = seconds > 0 ? Math.round(objects / seconds) : 0;
			return new ScaleResult(poolSize, sources, destinations, concurrency, objects, throughput,
					percentileMs(sorted, 0.50), percentileMs(sorted, 0.95), percentileMs(sorted, 1.0));
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
			return String.format("%5d %5d %5d %6d %8d %10d %7d %7d %7d", poolSize, sources, destinations, concurrency,
					objects, objectsPerSec, p50Ms, p95Ms, maxMs);
		}
	}

}