/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.dicom;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.karnak.backend.model.dicom.ConfigNode;
import org.karnak.backend.service.thread.DicomEchoThread;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DicomEchoService {

	@Autowired
	public DicomEchoService() {
	}

	public String dicomEcho(List<ConfigNode> nodes) throws InterruptedException, ExecutionException {
		StringBuilder result = new StringBuilder();

		List<Future<String>> threadsResult = createThreadsResult(nodes);
		for (Future<String> threadResult : threadsResult) {
			result.append(threadResult.get());
		}

		return result.toString();
	}

	private List<Future<String>> createThreadsResult(List<ConfigNode> nodes) throws InterruptedException {
		List<Future<String>> threadResult = new ArrayList<>();
		int poolSize = Math.min(nodes.size(), Runtime.getRuntime().availableProcessors());

		try (ExecutorService executorService = Executors.newFixedThreadPool(poolSize)) {
			List<DicomEchoThread> threads = createThreads(nodes); // Refactored helper
			threadResult = executorService.invokeAll(threads, 30, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw e;
		}
		return threadResult;
	}

	private List<DicomEchoThread> createThreads(List<ConfigNode> nodes) {
		List<DicomEchoThread> threads = new ArrayList<>();
		for (ConfigNode node : nodes) {
			threads.add(new DicomEchoThread(node));
		}
		return threads;
	}

}
