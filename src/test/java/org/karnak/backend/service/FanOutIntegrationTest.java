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

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.karnak.backend.dicom.DicomForwardDestination;
import org.karnak.backend.dicom.ForwardDestination;
import org.karnak.backend.dicom.ForwardDicomNode;
import org.karnak.backend.dicom.Params;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.weasis.dicom.param.DicomNode;

/**
 * Fan-out forwarding: one source object delivered to several destinations in a single
 * {@code storeMultipleDestination} call. Covers both the sequential and the
 * parallel-executor fan-out paths, and that one failing destination does not stop the
 * others.
 */
@Tag("integration")
@DisplayNameGeneration(ReplaceUnderscores.class)
class FanOutIntegrationTest extends GatewayItTestSupport {

	private ForwardDicomNode fwdNode;

	@BeforeEach
	void setUp() {
		fwdNode = new ForwardDicomNode("SOURCE-IT");
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void forwards_every_instance_to_every_destination(boolean parallel) throws Exception {
		Scp a = startScp();
		Scp b = startScp();
		Scp c = startScp();
		ForwardService forwardService = forwardService(parallel);
		List<ForwardDestination> dests = List.of(dicomDestination(fwdNode, a), dicomDestination(fwdNode, b),
				dicomDestination(fwdNode, c));

		Set<String> sent = new TreeSet<>();
		try {
			for (int i = 0; i < 15; i++) {
				String iuid = "1.2.826.0.1.3680043.8.498." + i;
				sent.add(iuid);
				Params p = params(iuid, serialize(iuid));
				forwardService.storeMultipleDestination(fwdNode, dests, p);
			}
		}
		finally {
			dests.forEach(ForwardDestination::stop);
			forwardService.shutdownFanoutExecutor();
		}

		assertEquals(sent, receivedSopInstanceUids(a.storageDir()));
		assertEquals(sent, receivedSopInstanceUids(b.storageDir()));
		assertEquals(sent, receivedSopInstanceUids(c.storageDir()));
	}

	@Test
	void a_dead_destination_does_not_stop_the_live_ones() throws Exception {
		Scp live1 = startScp();
		Scp live2 = startScp();
		DicomNode dead = new DicomNode("DEAD-SCP", "localhost", freePort()); // nothing
																				// listening
		ForwardService forwardService = forwardService(true);

		// The dead destination is not first: the first destination buffers the dataset
		// that the
		// remaining destinations are served from, so the live ones must still receive it.
		DicomForwardDestination deadDest = new DicomForwardDestination(99L, advancedParams(), fwdNode, dead, false,
				null, List.of(NOOP), null, true, 1);
		List<ForwardDestination> dests = List.of(dicomDestination(fwdNode, live1), deadDest,
				dicomDestination(fwdNode, live2));

		Set<String> sent = new TreeSet<>();
		int failures = 0;
		try {
			for (int i = 0; i < 5; i++) {
				String iuid = "1.2.826.0.1.3680043.8.498." + i;
				sent.add(iuid);
				Params p = params(iuid, serialize(iuid));
				try {
					forwardService.storeMultipleDestination(fwdNode, dests, p);
				}
				catch (IOException expected) {
					failures++;
				}
			}
		}
		finally {
			dests.forEach(ForwardDestination::stop);
			forwardService.shutdownFanoutExecutor();
		}

		assertEquals(5, failures, "the dead destination should surface a forwarding error every time");
		assertEquals(sent, receivedSopInstanceUids(live1.storageDir()));
		assertEquals(sent, receivedSopInstanceUids(live2.storageDir()));
	}

	private static ForwardService forwardService(boolean parallel) {
		ForwardService service = new ForwardService(mock(ApplicationEventPublisher.class), null);
		if (parallel) {
			ReflectionTestUtils.setField(service, "parallelFanout", true);
			ReflectionTestUtils.setField(service, "fanoutMaxThreads", 4);
			service.initFanoutExecutor();
		}
		return service;
	}

}