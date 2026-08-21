/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Executor used by the {@code @Async} event listeners (transfer monitoring, conformance
 * collection). Spring Boot's default async executor has an unbounded queue: when the
 * listeners (DB upserts, validation) are slower than the DICOM ingest rate, the backlog
 * of pending events grows without limit and the heap with it, until the JVM crashes. This
 * executor bounds the queue and runs overflowing tasks on the publishing thread
 * (caller-runs), so a saturated event pipeline throttles the forwarding threads instead
 * of accumulating events in memory.
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

	@Value("${async-events.core-pool-size:8}")
	private int corePoolSize;

	@Value("${async-events.max-pool-size:16}")
	private int maxPoolSize;

	@Value("${async-events.queue-capacity:10000}")
	private int queueCapacity;

	@Bean
	public ThreadPoolTaskExecutor asyncEventExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(maxPoolSize);
		executor.setQueueCapacity(queueCapacity);
		executor.setThreadNamePrefix("karnak-async-");
		executor.setAllowCoreThreadTimeOut(true);
		// Full queue: the publisher runs the listener itself, which slows ingest
		// down to the rate the listeners can sustain instead of dropping events or
		// queuing them without bound
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		// Drain the queued monitoring events on shutdown instead of dropping them
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(30);
		return executor;
	}

	@Override
	public Executor getAsyncExecutor() {
		return asyncEventExecutor();
	}

}