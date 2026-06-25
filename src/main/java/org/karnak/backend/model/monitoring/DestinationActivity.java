/*
 * Copyright (c) 2022-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.monitoring;

/**
 * Aggregated transfer activity for a single destination over the selected period (top
 * level of the monitoring hierarchy). {@code forwardAet} is the forward node AE Title
 * used as a prefix in the UI; {@code destinationLabel} is the destination display string.
 */
public record DestinationActivity(Long destinationId, String forwardAet, String destinationLabel, long studies,
		long series, long instances, long sent, long errors) {
}
