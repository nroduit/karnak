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
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
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
import org.karnak.backend.dicom.ForwardDestination;
import org.karnak.backend.dicom.ForwardDicomNode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.weasis.dicom.param.AttributeEditor;

/**
 * Performance and saturation characterisation of the gateway forwarding path. Reports
 * throughput as the connection pool grows and proves the fan-out executor degrades
 * gracefully (no dropped instances, no deadlock) when more work is submitted than its
 * bounded queue holds.
 *
 * <p>
 * Tagged {@code performance} and skipped by the default build; run with
 * {@code mvn test -Pperformance}. Throughput numbers are logged for inspection rather
 * than asserted, so the suite does not become flaky on shared CI hardware; only
 * correctness (every instance delivered) is asserted.
 */
@Tag("performance")
@DisplayNameGeneration(ReplaceUnderscores.class)
@Slf4j
class GatewayThroughputTest extends GatewayItTestSupport {

	private ForwardDicomNode fwdNode;

	@BeforeEach
	void setUp() {
		fwdNode = new ForwardDicomNode("SOURCE-IT");
	}

	@Test
	void throughput_is_reported_across_growing_pool_sizes() throws Exception {
		int count = 300;
		int senderThreads = 8;
		ForwardService forwardService = new ForwardService(mock(ApplicationEventPublisher.class), null);
		for (int poolSize : new int[] { 1, 2, 4, 8 }) {
			Scp scp = startScp();
			DicomForwardDestination dest = dicomDestination(fwdNode, scp, poolSize);
			long elapsedNanos = forwardConcurrently(forwardService, dest, count, senderThreads);

			assertEquals(count, receivedSopInstanceUids(scp.storageDir()).size(),
					"every instance must be delivered (pool=" + poolSize + ")");
			double seconds = elapsedNanos / 1_000_000_000.0;
			log.info("pool={} instances={} elapsed={}ms throughput={}/s", poolSize, count, Math.round(seconds * 1000),
					Math.round(count / seconds));
		}
	}

	@Test
	void fan_out_does_not_drop_instances_under_queue_saturation() throws Exception {
		// 2 fan-out threads with a bounded queue, fanning out to many destinations whose
		// editor
		// sleeps: far more sends are submitted than the queue holds, so caller-runs
		// back-pressure
		// kicks in. Nothing may be dropped and it must not deadlock.
		ForwardService forwardService = new ForwardService(mock(ApplicationEventPublisher.class), null);
		ReflectionTestUtils.setField(forwardService, "parallelFanout", true);
		ReflectionTestUtils.setField(forwardService, "fanoutMaxThreads", 2);
		forwardService.initFanoutExecutor();

		int destinationCount = 12;
		List<Scp> scps = new ArrayList<>();
		List<ForwardDestination> dests = new ArrayList<>();
		AttributeEditor slowEditor = (attributes, context) -> sleep(10);
		for (int i = 0; i < destinationCount; i++) {
			Scp scp = startScp();
			scps.add(scp);
			dests.add(new DicomForwardDestination(scp.port() + 0L, advancedParams(), fwdNode, scp.node(), false, null,
					List.of(slowEditor), null, true, 1));
		}

		Set<String> sent = new TreeSet<>();
		try {
			assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
				for (int i = 0; i < 5; i++) {
					String iuid = "1.2.826.0.1.3680043.8.498." + i;
					sent.add(iuid);
					forwardService.storeMultipleDestination(fwdNode, dests, params(iuid, serialize(iuid)));
				}
			});
		}
		finally {
			dests.forEach(ForwardDestination::stop);
			forwardService.shutdownFanoutExecutor();
		}

		for (Scp scp : scps) {
			assertEquals(sent, receivedSopInstanceUids(scp.storageDir()),
					"every destination must receive every instance despite back-pressure");
		}
	}

	/**
	 * Forwards {@code count} instances to {@code dest} from a fixed sender thread pool
	 * and returns the elapsed time including the final drain
	 * ({@link DicomForwardDestination#stop()}), so the measurement covers complete
	 * delivery.
	 */
	private long forwardConcurrently(ForwardService forwardService, DicomForwardDestination dest, int count,
			int senderThreads) throws Exception {
		List<Callable<Void>> jobs = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			String iuid = "1.2.826.0.1.3680043.8.498." + i;
			byte[] object = serialize(iuid);
			jobs.add(() -> {
				forwardService.storeMultipleDestination(fwdNode, List.of(dest), params(iuid, object));
				return null;
			});
		}
		ExecutorService pool = Executors.newFixedThreadPool(senderThreads);
		long start = System.nanoTime();
		try {
			List<Future<Void>> futures = pool.invokeAll(jobs);
			for (Future<Void> future : futures) {
				future.get(60, TimeUnit.SECONDS);
			}
			dest.stop();
		}
		finally {
			pool.shutdownNow();
		}
		return System.nanoTime() - start;
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}