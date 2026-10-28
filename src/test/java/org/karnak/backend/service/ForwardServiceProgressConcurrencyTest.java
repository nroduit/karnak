/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.karnak.backend.dicom.ForwardDestination;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.weasis.dicom.param.DicomProgress;
import org.weasis.dicom.param.DicomState;

/**
 * The {@link DicomState} of a destination is shared by every concurrent transfer. Its
 * attributes are not thread-safe: unsynchronized updates used to fail with
 * {@code ArrayIndexOutOfBoundsException} (arraycopy) or {@code NullPointerException}
 * (null VR in {@code Attributes.getInt}).
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ForwardServiceProgressConcurrencyTest {

	private static final String CUID = "1.2.840.10008.5.1.4.1.1.2";

	private static final int THREADS = 32;

	private static final int ITERATIONS = 200;

	@Test
	void concurrent_progress_notifications_on_shared_state_do_not_fail() throws Exception {
		DicomState state = new DicomState(new DicomProgress());
		ForwardDestination destination = mock(ForwardDestination.class);
		when(destination.getState()).thenReturn(state);

		CountDownLatch start = new CountDownLatch(1);
		List<Future<?>> futures = new ArrayList<>();
		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			for (int t = 0; t < THREADS; t++) {
				final int thread = t;
				futures.add(executor.submit(() -> {
					start.await();
					for (int i = 0; i < ITERATIONS; i++) {
						String iuid = "1.2.3." + thread + "." + i;
						ForwardService.progressNotify(destination, iuid, CUID, i % 2 == 0, 0);
					}
					return null;
				}));
			}
			start.countDown();
			executor.shutdown();
			assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS));
		}

		for (Future<?> future : futures) {
			assertDoesNotThrow(() -> future.get());
		}
	}

}

