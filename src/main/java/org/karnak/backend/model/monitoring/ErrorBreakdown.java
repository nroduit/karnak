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
 * Per-reason breakdown at the leaf level of the monitoring hierarchy: a distinct reason
 * with how many outcomes of the series carried it, counted in the series buckets —
 * {@code errors} for hard transfer errors, {@code excluded} for excluded (aborted /
 * filtered) outcomes, and {@code retries} for those that hit an already-seen instance.
 */
public record ErrorBreakdown(String reason, long errors, long excluded, long retries) {
}
