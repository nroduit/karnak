/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.dicom;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.karnak.backend.model.dicom.result.ConnectionLatency;
import org.karnak.backend.model.dicom.result.NetworkCheckResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Low-level reachability check for a DICOM node: resolves the hostname, pings it and
 * tests whether the DICOM port accepts TCP connections. The ping and the TCP port probe
 * are independent blocking operations and run concurrently. Never throws — failures are
 * reported through the returned {@link NetworkCheckResult}.
 */
@Service
@Slf4j
@NullUnmarked
public class NetworkCheckService {

	private static final int PING_OTHER_ERROR = 2;

	private static final int PORT_SAMPLES = 4;

	private final Duration pingTimeout;

	private final Duration portTimeout;

	public NetworkCheckService(@Value("${dicom-tools.network.ping-timeout-ms:3000}") long pingTimeoutMs,
			@Value("${dicom-tools.network.port-timeout-ms:3000}") long portTimeoutMs) {
		this.pingTimeout = Duration.ofMillis(pingTimeoutMs);
		this.portTimeout = Duration.ofMillis(portTimeoutMs);
	}

	public NetworkCheckResult check(String hostname, int port) {
		String hostAddress = null;
		boolean hostnameReachable = false;
		boolean unresolvedHostname = false;
		boolean unexpectedError = false;
		String unexpectedErrorMessage = null;
		ConnectionLatency connectionLatency = null;

		try {
			InetAddress ipAddress = InetAddress.getByName(hostname);
			hostAddress = ipAddress.getHostAddress();

			// Ping and TCP port probe are independent blocking I/O; run them in parallel.
			try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
				Future<Boolean> reachableFuture = executor.submit(() -> executePingCmd(hostname) == 0);
				Future<ConnectionLatency> latencyFuture = executor.submit(() -> probePort(hostname, port));
				hostnameReachable = awaitProbe(reachableFuture, false);
				connectionLatency = awaitProbe(latencyFuture, null);
			}

			if (!hostnameReachable) {
				unresolvedHostname = ipAddress.getHostAddress().equals(ipAddress.getHostName());
			}
		}
		catch (UnknownHostException _) {
			// Unresolvable host: an expected outcome, surfaced in the result.
			log.warn("Cannot resolve host {}", hostname);

			unexpectedError = true;
			unexpectedErrorMessage = "Unknown Host";
		}

		return NetworkCheckResult.builder()
			.hostname(hostname)
			.hostAddress(hostAddress)
			.port(port)
			.hostnameReachable(hostnameReachable)
			.portOpen(connectionLatency != null)
			.unresolvedHostname(unresolvedHostname)
			.unexpectedError(unexpectedError)
			.unexpectedErrorMessage(unexpectedErrorMessage)
			.connectionLatency(connectionLatency)
			.build();
	}

	private static <T> T awaitProbe(Future<T> future, T fallback) {
		try {
			return future.get();
		}
		catch (InterruptedException _) {
			Thread.currentThread().interrupt();
			future.cancel(true);
			return fallback;
		}
		catch (ExecutionException ex) {
			log.error("Network probe failed", ex);
			return fallback;
		}
	}

	/**
	 * Samples the DICOM port a few times to gauge connection quality. Returns
	 * {@code null} as soon as the first attempt fails (the port is closed/filtered) or
	 * when no attempt connects; otherwise reports the min/avg/max connect time and how
	 * many attempts succeeded.
	 */
	private @Nullable ConnectionLatency probePort(String hostname, int port) {
		long minMs = Long.MAX_VALUE;
		long maxMs = 0;
		long sumMs = 0;
		int successes = 0;
		int attempts = 0;

		for (int i = 0; i < PORT_SAMPLES; i++) {
			attempts++;
			long start = System.nanoTime();
			try (Socket socket = new Socket()) {
				socket.connect(new InetSocketAddress(hostname, port), (int) portTimeout.toMillis());
				long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
				successes++;
				minMs = Math.min(minMs, elapsedMs);
				maxMs = Math.max(maxMs, elapsedMs);
				sumMs += elapsedMs;
			}
			catch (IOException _) {
				// A first failed attempt means the port is closed/filtered: stop probing.
				if (i == 0) {
					log.debug("Port {} is not open on {}", port, hostname);
					return null;
				}
			}
		}

		if (successes == 0) {
			return null;
		}

		return new ConnectionLatency(attempts, successes, minMs, sumMs / successes, maxMs);
	}

	private int executePingCmd(String hostname) {
		String countValue = "1";
		String pingExecutable;
		String countArg;
		String timeoutArg;
		String timeoutValue;

		if (System.getProperty("os.name").startsWith("Windows")) {
			pingExecutable = windowsPingExecutable();
			countArg = "-n";
			timeoutArg = "-w";
			timeoutValue = String.valueOf(pingTimeout.toMillis());
		}
		else {
			pingExecutable = unixPingExecutable();
			countArg = "-c";
			timeoutArg = "-W";
			// Linux ping expects the per-reply timeout in whole seconds (at least 1).
			timeoutValue = String.valueOf(Math.max(1, pingTimeout.toSeconds()));
		}

		try {
			ProcessBuilder builder = new ProcessBuilder(pingExecutable, countArg, countValue, timeoutArg, timeoutValue,
					hostname);
			Process process = builder.start();
			return process.waitFor();
		}
		catch (UnsupportedOperationException | IOException ex) {
			log.error("An error occurred while executing the ping command for host {}", hostname, ex);

			return PING_OTHER_ERROR;
		}
		catch (InterruptedException _) {
			log.info("Interrupted while executing a ping command");
			Thread.currentThread().interrupt();
			return PING_OTHER_ERROR;
		}
	}

	/**
	 * Resolves the absolute path of the Windows {@code ping} binary (under
	 * {@code %SystemRoot%\System32}) so the command never relies on {@code PATH}
	 * resolution.
	 */
	private static String windowsPingExecutable() {
		String systemRoot = System.getenv("SystemRoot");
		String base = (systemRoot != null && !systemRoot.isBlank()) ? systemRoot : "C:\\Windows";
		return base + "\\System32\\PING.EXE";
	}

	/**
	 * Resolves the absolute path of the {@code ping} binary on Unix-like systems, which
	 * lives in different directories across Linux distributions and macOS, so the command
	 * never relies on {@code PATH} resolution. Falls back to the conventional
	 * {@code /bin/ping}.
	 */
	private static String unixPingExecutable() {
		for (String candidate : List.of("/bin/ping", "/usr/bin/ping", "/sbin/ping", "/usr/sbin/ping")) {
			if (Files.isExecutable(Path.of(candidate))) {
				return candidate;
			}
		}
		return "/bin/ping";
	}

}