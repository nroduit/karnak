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
 * Aggregated activity of a forward node over the selected period, for the monitoring
 * dashboard. {@code deidentified} / {@code tagMorphed} count the instances whose
 * destination has de-identification / tag-morphing enabled.
 */
public record NodeActivity(Long forwardNodeId, String forwardAet, long studies, long series, long instances, long sent,
		long errors, long deidentified, long tagMorphed) {
}
