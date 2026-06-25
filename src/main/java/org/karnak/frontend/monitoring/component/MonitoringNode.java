/*
 * Copyright (c) 2022-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.monitoring.component;

import java.time.LocalDateTime;

/**
 * Row model for the monitoring hierarchy tree. A node is a destination, a study, a series
 * or — only under a series that has errors — a distinct error reason. Identity is the
 * stable {@link #key()} so the {@code TreeGrid} can track rows across rebuilds. Nodes
 * also carry the fields shown in the selection detail panel.
 */
public sealed interface MonitoringNode {

	String key();

	/** Whether this node (or its subtree) involves errors — drives highlighting. */
	boolean hasErrors();

	/**
	 * A destination with aggregated counts. {@code forwardAet} is the forward node
	 * prefix.
	 */
	record DestinationNode(Long destinationId, String forwardAet, String destinationLabel, long studies, long series,
			long instances, long sent, long errors) implements MonitoringNode {
		@Override
		public String key() {
			return "d:" + destinationId;
		}

		@Override
		public boolean hasErrors() {
			return errors > 0;
		}

		public String displayName() {
			return forwardAet == null || forwardAet.isBlank() ? destinationLabel
					: forwardAet + " → " + destinationLabel;
		}
	}

	/** A study under a destination, identified by its original Study Instance UID. */
	record StudyNode(Long destinationId, String studyUid, String studyUidToSend, String description,
			String descriptionToSend, String patientIdOriginal, String patientIdToSend, String accessionNumberOriginal,
			String accessionNumberToSend, LocalDateTime studyDateOriginal, LocalDateTime studyDateToSend, long series,
			long instances, long sent, long errors, LocalDateTime firstSeen,
			LocalDateTime lastSeen) implements MonitoringNode {
		@Override
		public String key() {
			return "st:" + destinationId + ":" + studyUid;
		}

		@Override
		public boolean hasErrors() {
			return errors > 0;
		}
	}

	/**
	 * A series under a study, identified by its original Series Instance UID. It also
	 * carries the patient/study context of its parent study so the detail panel can show
	 * the full collected information (and the de-identification original/final values) at
	 * series level.
	 */
	record SeriesNode(Long destinationId, String studyUid, String studyUidToSend, String patientIdOriginal,
			String patientIdToSend, String accessionNumberOriginal, String accessionNumberToSend,
			String studyDescriptionOriginal, String studyDescriptionToSend, LocalDateTime studyDateOriginal,
			LocalDateTime studyDateToSend, String serieUid, String serieUidToSend, String description,
			String descriptionToSend, String modality, String sopClassUids, LocalDateTime serieDateOriginal,
			LocalDateTime serieDateToSend, long instances, long sent, long errors, LocalDateTime firstSeen,
			LocalDateTime lastSeen) implements MonitoringNode {
		@Override
		public String key() {
			return "se:" + destinationId + ":" + serieUid;
		}

		@Override
		public boolean hasErrors() {
			return errors > 0;
		}
	}

	/** A distinct error reason with the number of instances concerned, under a series. */
	record ErrorNode(String parentKey, String reason, long instances) implements MonitoringNode {
		@Override
		public String key() {
			return parentKey + "|err:" + reason;
		}

		@Override
		public boolean hasErrors() {
			return true;
		}
	}

}
