/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.dicom.result;

/**
 * TCP connection-quality sample taken against the DICOM port: how many connection
 * attempts were made, how many succeeded, and the min / average / max connect time of the
 * successful ones (milliseconds). Surfaces flaky or high-latency links that a single
 * pass/fail probe would hide.
 *
 * @param attempts number of TCP connection attempts performed
 * @param successes number of attempts that connected
 * @param minMs fastest successful connect time, in milliseconds
 * @param avgMs average successful connect time, in milliseconds
 * @param maxMs slowest successful connect time, in milliseconds
 */
public record ConnectionLatency(int attempts, int successes, long minMs, long avgMs, long maxMs) {

	public int packetLossPercent() {
		return (this.attempts == 0) ? 0 : Math.round((this.attempts - this.successes) * 100f / this.attempts);
	}

}