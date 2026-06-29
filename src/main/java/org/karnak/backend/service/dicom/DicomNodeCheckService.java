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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.model.dicom.ConfigNode;
import org.karnak.backend.model.dicom.result.DicomEchoResult;
import org.karnak.backend.model.dicom.result.DicomNodeCheckResult;
import org.karnak.backend.model.dicom.result.NetworkCheckResult;
import org.springframework.stereotype.Service;

/**
 * Composes the DICOM echo and the network check for one or many DICOM nodes.
 *
 * <p>
 * The single-node {@link #check(String, ConfigNode)} is the unit of work; the multi-node
 * {@link #check(String, List)} runs those units concurrently on virtual threads (one per
 * node) so a list of mostly-idle, I/O-bound checks completes in roughly the time of the
 * slowest node rather than their sum. Each unit is failure-safe (the underlying services
 * never throw), and a global timeout guards against an unresponsive node.
 */
@Service
@Slf4j
@NullUnmarked
public class DicomNodeCheckService {

	private static final Duration CHECK_TIMEOUT = Duration.ofSeconds(30);

	private final DicomEchoCheckService dicomEchoCheckService;

	private final NetworkCheckService networkCheckService;

	public DicomNodeCheckService(DicomEchoCheckService dicomEchoCheckService, NetworkCheckService networkCheckService) {
		this.dicomEchoCheckService = dicomEchoCheckService;
		this.networkCheckService = networkCheckService;
	}

	public DicomNodeCheckResult check(String callingAET, ConfigNode calledNode) {
		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			return check(callingAET, calledNode, executor);
		}
	}

	public List<DicomNodeCheckResult> check(String callingAET, List<ConfigNode> calledNodes) {
		if (calledNodes == null || calledNodes.isEmpty()) {
			return List.of();
		}

		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			List<Callable<DicomNodeCheckResult>> tasks = calledNodes.stream()
				.map((node) -> (Callable<DicomNodeCheckResult>) () -> check(callingAET, node, executor))
				.toList();

			// invokeAll preserves the task order, so future[i] maps to calledNodes[i].
			List<Future<DicomNodeCheckResult>> futures = executor.invokeAll(tasks, CHECK_TIMEOUT.toSeconds(),
					TimeUnit.SECONDS);

			List<DicomNodeCheckResult> results = new ArrayList<>(futures.size());
			for (int i = 0; i < futures.size(); i++) {
				results.add(resolve(callingAET, calledNodes.get(i), futures.get(i)));
			}

			return results;
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while checking DICOM nodes", ex);

			return List.of();
		}
	}

	/**
	 * Runs the DICOM echo and the network check for a single node concurrently: the
	 * network check runs on its own virtual thread while the (equally blocking,
	 * independent) echo runs on the calling thread, so the node's latency is the slower
	 * of the two rather than their sum.
	 */
	private DicomNodeCheckResult check(String callingAET, ConfigNode calledNode, ExecutorService executor) {
		Future<NetworkCheckResult> networkFuture = executor
			.submit(() -> this.networkCheckService.check(calledNode.getHostname(), calledNode.getPort()));

		DicomEchoResult dicomEchoResult = this.dicomEchoCheckService.echo(callingAET, calledNode);
		NetworkCheckResult networkCheckResult = awaitNetworkCheck(networkFuture, calledNode);

		return new DicomNodeCheckResult(callingAET, calledNode, dicomEchoResult, networkCheckResult);
	}

	private NetworkCheckResult awaitNetworkCheck(Future<NetworkCheckResult> future, ConfigNode calledNode) {
		try {
			return future.get();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			future.cancel(true);
			return failedNetworkCheck(calledNode, "Network check was interrupted");
		}
		catch (ExecutionException ex) {
			log.error("Network check failed for {}: {}", calledNode.getAet(), ex.getMessage());
			return failedNetworkCheck(calledNode, ex.getMessage());
		}
	}

	private DicomNodeCheckResult resolve(String callingAET, ConfigNode calledNode,
			Future<DicomNodeCheckResult> future) {
		try {
			return future.get();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return timedOut(callingAET, calledNode);
		}
		catch (Exception ex) {
			// Includes CancellationException when the node exceeded the global timeout.
			log.error("DICOM node check did not complete for {}: {}", calledNode.getAet(), ex.getMessage());
			return timedOut(callingAET, calledNode);
		}
	}

	private static DicomNodeCheckResult timedOut(String callingAET, ConfigNode calledNode) {
		String message = "Check did not complete within " + CHECK_TIMEOUT.toSeconds() + "s";

		DicomEchoResult dicomEchoResult = DicomEchoResult.builder()
			.unexpectedError(true)
			.unexpectedErrorMessage(message)
			.build();

		return new DicomNodeCheckResult(callingAET, calledNode, dicomEchoResult,
				failedNetworkCheck(calledNode, message));
	}

	private static NetworkCheckResult failedNetworkCheck(ConfigNode calledNode, String message) {
		return NetworkCheckResult.builder()
			.hostname(calledNode.getHostname())
			.port(calledNode.getPort())
			.unexpectedError(true)
			.unexpectedErrorMessage(message)
			.build();
	}

}