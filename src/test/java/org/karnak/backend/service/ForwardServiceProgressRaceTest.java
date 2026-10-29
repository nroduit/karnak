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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.net.Status;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.karnak.backend.dicom.ForwardDestination;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.weasis.dicom.param.DicomProgress;
import org.weasis.dicom.param.DicomState;
import org.weasis.dicom.util.ServiceUtil;
import org.weasis.dicom.util.ServiceUtil.ProgressStatus;

/**
 * The progress attributes of a {@link DicomState} are shared by every transfer sent on a
 * pooled association and by the dcm4che reader thread. Unsynchronized in-place updates
 * used to fail with {@code ArrayIndexOutOfBoundsException} (arraycopy while another
 * thread grows the arrays) or {@code NullPointerException} (null VR read during an
 * insert). This test models that: a reader thread publishes fresh C-STORE-RSP command
 * sets, as {@code StoreFromStreamSCU} does, while forwarding threads notify their
 * progress.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ForwardServiceProgressRaceTest {

	private static final String CUID = "1.2.840.10008.5.1.4.1.1.2";

	private static final int FORWARD_THREADS = 32;

	private static final int ITERATIONS = 2000;

	@Test
	void concurrent_progress_updates_from_reader_and_forward_threads_do_not_fail() throws Exception {
		DicomState state = new DicomState(new DicomProgress());
		ForwardDestination destination = mock(ForwardDestination.class);
		when(destination.getState()).thenReturn(state);

		AtomicInteger forwardErrors = new AtomicInteger();
		AtomicInteger readerErrors = new AtomicInteger();
		ConcurrentLinkedQueue<String> samples = new ConcurrentLinkedQueue<>();
		AtomicBoolean stop = new AtomicBoolean();
		CountDownLatch start = new CountDownLatch(1);
		List<Future<?>> forwarders = new ArrayList<>();
		Future<?> reader;
		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			reader = executor.submit(() -> {
				start.await();
				int messageId = 0;
				while (!stop.get()) {
					try {
						onCStoreResponse(state, cstoreResponse(++messageId));
					}
					catch (RuntimeException e) {
						if (readerErrors.incrementAndGet() <= 3) {
							samples.add("reader: " + e);
						}
					}
				}
				return null;
			});
			for (int t = 0; t < FORWARD_THREADS; t++) {
				final int thread = t;
				forwarders.add(executor.submit(() -> {
					start.await();
					for (int i = 0; i < ITERATIONS; i++) {
						try {
							ForwardService.progressNotify(destination, "1.2.3." + thread + "." + i, CUID, i % 2 == 0,
									0);
						}
						catch (RuntimeException e) {
							if (forwardErrors.incrementAndGet() <= 3) {
								samples.add("forward: " + e);
							}
						}
					}
					return null;
				}));
			}
			start.countDown();
			for (Future<?> forwarder : forwarders) {
				forwarder.get(60, TimeUnit.SECONDS);
			}
			stop.set(true);
			reader.get(10, TimeUnit.SECONDS);
		}

		assertEquals(0, forwardErrors.get() + readerErrors.get(),
				() -> "forwardErrors=" + forwardErrors + " readerErrors=" + readerErrors + " " + samples);
	}

	/**
	 * What dcm4che hands to the RSP handler: a command set allocated with a capacity of 9
	 * (see {@code DicomInputStream.readCommand}) and no Affected SOP Instance UID, which
	 * some SCPs omit. The counters added below fill the capacity, so the first progress
	 * notification that adds the instance UID has to grow the arrays.
	 */
	private static Attributes cstoreResponse(int messageId) {
		Attributes cmd = new Attributes(9);
		cmd.setInt(Tag.CommandField, VR.US, 0x8001);
		cmd.setString(Tag.AffectedSOPClassUID, VR.UI, CUID);
		cmd.setInt(Tag.MessageIDBeingRespondedTo, VR.US, messageId);
		cmd.setInt(Tag.CommandDataSetType, VR.US, 0x0101);
		cmd.setInt(Tag.Status, VR.US, Status.Success);
		return cmd;
	}

	/**
	 * Mirrors {@code StoreFromStreamSCU.handleCStoreResponse} and {@code updateProgress}.
	 */
	private static void onCStoreResponse(DicomState state, Attributes cmd) {
		state.setStatus(cmd.getInt(Tag.Status, -1));
		ServiceUtil.notifyProgression(state.getProgress(), cmd, ProgressStatus.COMPLETED, 0);
		state.getProgress().setAttributes(cmd);
	}

}
