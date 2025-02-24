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
		List<Future<String>> threadResult = null;
		try {
			ExecutorService executorService = Executors.newFixedThreadPool(nodes.size());

			List<DicomEchoThread> threads = new ArrayList<>();

			for (ConfigNode node : nodes) {
				DicomEchoThread dicomEchoThread = new DicomEchoThread(node);
				threads.add(dicomEchoThread);
			}

			threadResult = executorService.invokeAll(threads);

			return threadResult;
		}
		catch (InterruptedException e) {
			throw e;
		}
	}

}
