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

import java.time.LocalDateTime;

/**
 * Aggregated transfer activity for a single series under a study (third level of the
 * monitoring hierarchy). The series is identified by its original Series Instance UID;
 * the remaining fields are representative values for the detail panel.
 */
public record SeriesActivity(String serieUid, String serieUidToSend, String serieDescription,
		String serieDescriptionToSend, String modality, String sopClassUids, LocalDateTime serieDateOriginal,
		LocalDateTime serieDateToSend, long instances, long sent, long errors, long retries, long excluded,
		LocalDateTime firstSeen, LocalDateTime lastSeen) {
}
