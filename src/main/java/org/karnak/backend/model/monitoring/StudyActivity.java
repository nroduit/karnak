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
 * Aggregated transfer activity for a single study under a destination (second level of
 * the monitoring hierarchy). The study is identified by its original Study Instance UID;
 * the remaining fields are representative values for the detail panel.
 */
public record StudyActivity(String studyUid, String studyUidToSend, String description, String descriptionToSend,
		String patientIdOriginal, String patientIdToSend, String accessionNumberOriginal, String accessionNumberToSend,
		LocalDateTime studyDateOriginal, LocalDateTime studyDateToSend, long series, long instances, long sent,
		long errors, LocalDateTime firstSeen, LocalDateTime lastSeen) {
}
