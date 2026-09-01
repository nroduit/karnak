/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.dicom;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.net.Device;

/**
 * Manages the listener device's executors and connection lifecycle, replacing
 * {@code org.weasis.dicom.param.DeviceListenerService} for the gateway. The difference is
 * the executor serving the associations: virtual threads by default instead of a cached
 * platform-thread pool.
 * <p>
 * The gateway parks association threads deliberately - a C-STORE blocks in
 * {@code store()} while it waits for a transfer permit, and blocks again on the sender
 * whenever the PDVs arrive slower than they are consumed. A parked virtual thread costs
 * kilobytes where a platform thread reserves a stack around 1 MB, so waiting senders stop
 * being a memory concern and the open-association cap can be raised for deployments with
 * many mostly-idle associations. Native work (OpenCV transcoding, masking) pins an OS
 * carrier thread while it runs, which is acceptable: that work is CPU-bound and already
 * bounded by the transfer permits.
 */
@Slf4j
public class GatewayDeviceListenerService {

	private static final int SHUTDOWN_TIMEOUT_SECONDS = 10;

	private final Device device;

	private final boolean virtualThreads;

	private ExecutorService executor;

	private ScheduledExecutorService scheduledExecutor;

	public GatewayDeviceListenerService(Device device, boolean virtualThreads) {
		this.device = Objects.requireNonNull(device, "Device must not be null");
		this.virtualThreads = virtualThreads;
	}

	public Device getDevice() {
		return device;
	}

	public synchronized boolean isRunning() {
		return executor != null && !executor.isShutdown();
	}

	/**
	 * Creates the executors, attaches them to the device and binds its connections. On a
	 * bind failure the executors are torn down again before the exception propagates.
	 */
	public synchronized void start() throws IOException, GeneralSecurityException {
		if (isRunning()) {
			throw new IllegalStateException("Service is already running");
		}
		// The device executor runs the accept loop of each bound connection and one
		// task per accepted association; both just block on I/O, the model virtual
		// threads are made for. The scheduled executor only arms dcm4che timeouts
		// and stays a platform thread.
		this.executor = virtualThreads
				? Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("dicom-listener-", 0).factory())
				: Executors.newCachedThreadPool();
		this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
		device.setExecutor(executor);
		device.setScheduledExecutor(scheduledExecutor);
		try {
			device.bindConnections();
		}
		catch (IOException | GeneralSecurityException e) {
			stop();
			throw e;
		}
		log.info("DICOM listener started ({} association threads)", virtualThreads ? "virtual" : "platform");
	}

	/**
	 * Unbinds the device connections and shuts the executors down. Safe to call multiple
	 * times and on a service that never started.
	 */
	public synchronized void stop() {
		try {
			device.unbindConnections();
		}
		catch (RuntimeException e) {
			// stop() must always release the executors, whatever unbinding does
			log.warn("Error while unbinding the listener connections", e);
		}
		shutdown(scheduledExecutor);
		shutdown(executor);
		this.scheduledExecutor = null;
		this.executor = null;
	}

	private static void shutdown(ExecutorService executorService) {
		if (executorService != null && !executorService.isShutdown()) {
			executorService.shutdown();
			try {
				if (!executorService.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
					executorService.shutdownNow();
				}
			}
			catch (InterruptedException e) {
				executorService.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
	}

}
