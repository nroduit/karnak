/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.util;

import java.util.concurrent.atomic.AtomicInteger;

public class Counter {

	private final AtomicInteger count = new AtomicInteger(0);

	private final int interval;

	public Counter(int interval) {
		this.interval = interval;
	}

	public int getCount() {
		return count.get();
	}

	public void resetCount() {
		this.count.set(0);
	}

	public int increment() {
		return this.count.addAndGet(interval);
	}

}
