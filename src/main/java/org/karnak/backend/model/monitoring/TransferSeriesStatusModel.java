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
import java.util.UUID;

/**
 * One aggregated row per (forward node, destination, series) matching the search criteria.
 * Exposed to the API as a plain model: the internal database ids are not leaked, only the
 * stable {@code forwardNodeUuid} / {@code destinationUuid} identifiers.
 */
public record TransferSeriesStatusModel(UUID forwardNodeUuid, UUID destinationUuid,
										String forwardAeTitle, String forwardDescription,
										String destinationDescription,
										String patientIdOriginal, String patientIdToSend,
										String accessionNumberOriginal, String accessionNumberToSend,
										String studyDescriptionOriginal, String studyDescriptionToSend,
										LocalDateTime studyDateOriginal, LocalDateTime studyDateToSend,
										String studyUidOriginal, String studyUidToSend,
										String serieDescriptionOriginal, String serieDescriptionToSend,
										LocalDateTime serieDateOriginal, LocalDateTime serieDateToSend,
										String serieUidOriginal, String serieUidToSend,
										String modality, String sopClassUids,
										long instances, long retries, long sent, long errors, long excluded,
										LocalDateTime firstSeen, LocalDateTime lastSeen) {
}
