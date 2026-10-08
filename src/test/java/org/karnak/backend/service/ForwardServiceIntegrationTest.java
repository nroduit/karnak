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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.karnak.backend.dicom.DicomForwardDestination;
import org.karnak.backend.dicom.DicomForwardDestination.ScuLease;
import org.karnak.backend.dicom.ForwardDicomNode;
import org.karnak.backend.dicom.Params;
import org.springframework.context.ApplicationEventPublisher;

/**
 * End-to-end forwarding test: a real DICOM Store SCP (the destination PACS) is started on
 * a loopback port, and instances are forwarded to it through {@link ForwardService} with
 * a multi-association connection pool. It checks that every instance actually arrives —
 * exercising real associations, the lease/return pool, concurrent sends and pool
 * contention.
 */
@Tag("integration")
@DisplayNameGeneration(ReplaceUnderscores.class)
class ForwardServiceIntegrationTest extends GatewayItTestSupport {

	private ForwardDicomNode fwdNode;

	private Scp scp;

	private ForwardService forwardService;

	@BeforeEach
	void setUp() throws Exception {
		scp = startScp();
		fwdNode = new ForwardDicomNode("SOURCE-IT");
		forwardService = new ForwardService(mock(ApplicationEventPublisher.class), null);
	}

	@Test
	void forwards_every_instance_sequentially_over_a_single_connection() throws Exception {
		DicomForwardDestination dest = dicomDestination(fwdNode, scp, 1);
		Set<String> expected = forwardInstances(dest, 25, false);
		assertEquals(expected, receivedSopInstanceUids(scp.storageDir()));
		assertPoolFullyReleased(dest, 1);
	}

	@Test
	void forwards_every_instance_concurrently_through_the_pool() throws Exception {
		DicomForwardDestination dest = dicomDestination(fwdNode, scp, 3);
		Set<String> expected = forwardInstances(dest, 40, true);
		assertEquals(expected, receivedSopInstanceUids(scp.storageDir()));
		assertPoolFullyReleased(dest, 3);
	}

	@Test
	void forwards_every_instance_when_senders_outnumber_pool_slots() throws Exception {
		// 30 concurrent senders contend for a 2-slot pool: exhausted leases fall back to
		// a
		// shared association round-robin, so nothing is dropped or deadlocked.
		DicomForwardDestination dest = dicomDestination(fwdNode, scp, 2);
		Set<String> expected = forwardInstances(dest, 30, true);
		assertEquals(expected, receivedSopInstanceUids(scp.storageDir()));
		assertPoolFullyReleased(dest, 2);
	}

	/**
	 * Forwards {@code count} distinct instances to {@code dest}, either sequentially or
	 * from a thread pool. Returns the SOP Instance UIDs that were sent. The destination
	 * is stopped at the end, which drains outstanding responses and closes the
	 * associations, so all received files are on disk when this returns.
	 */
	private Set<String> forwardInstances(DicomForwardDestination dest, int count, boolean concurrent) throws Exception {
		Set<String> sent = new TreeSet<>();
		try {
			List<Callable<Void>> jobs = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				String iuid = "1.2.826.0.1.3680043.8.498." + i;
				sent.add(iuid);
				byte[] object = serialize(iuid);
				jobs.add(() -> {
					Params p = params(iuid, object);
					forwardService.storeMultipleDestination(fwdNode, List.of(dest), p);
					return null;
				});
			}
			if (concurrent) {
				runConcurrently(jobs);
			}
			else {
				for (Callable<Void> job : jobs) {
					job.call();
				}
			}
		}
		finally {
			// Forces the associations closed and waits for outstanding C-STORE responses.
			dest.stop();
		}
		return sent;
	}

	private static void runConcurrently(List<Callable<Void>> jobs) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(6);
		try {
			List<Future<Void>> futures = pool.invokeAll(jobs);
			for (Future<Void> future : futures) {
				future.get(30, TimeUnit.SECONDS);
			}
		}
		finally {
			pool.shutdownNow();
		}
	}

	/**
	 * Asserts no lease leaked: every slot of the pool can be acquired exclusively, which
	 * only holds when all prior transfers returned their lease.
	 */
	private static void assertPoolFullyReleased(DicomForwardDestination dest, int poolSize) {
		List<ScuLease> leases = new ArrayList<>();
		try {
			for (int i = 0; i < poolSize; i++) {
				ScuLease lease = dest.acquire();
				leases.add(lease);
				assertTrue(lease.exclusive(), "slot " + i + " should be free — a lease leaked");
			}
		}
		finally {
			leases.forEach(dest::release);
		}
	}

}