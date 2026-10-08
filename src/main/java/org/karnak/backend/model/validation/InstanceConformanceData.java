/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.validation;

import org.dcm4che3.data.Tag;

/**
 * Per-instance data collected while forwarding, input of the conformance report pipeline.
 * All DICOM values come from the de-identified dataset actually sent — never from the
 * original one.
 */
public record InstanceConformanceData(Long forwardNodeId, Long destinationId, String sourceAet, String studyUid,
		String seriesUid, String sopClassUid, String sopInstanceUid, String modality, String transferSyntaxUid,
		boolean sent, String failureReason, boolean checkValueConformity, boolean deepSequenceValidation,
		boolean deidentified, MetadataSnapshot snapshot, ImageIdentityCheckInput imageIdentityCheckInput) {

	public static InstanceConformanceData of(Long forwardNodeId, Long destinationId, String sourceAet,
			String transferSyntaxUid, boolean sent, String failureReason, boolean checkValueConformity,
			boolean deepSequenceValidation, boolean deidentified, MetadataSnapshot snapshot) {
		return of(forwardNodeId, destinationId, sourceAet, transferSyntaxUid, sent, failureReason, checkValueConformity,
				deepSequenceValidation, deidentified, snapshot, null);
	}

	public static InstanceConformanceData of(Long forwardNodeId, Long destinationId, String sourceAet,
			String transferSyntaxUid, boolean sent, String failureReason, boolean checkValueConformity,
			boolean deepSequenceValidation, boolean deidentified, MetadataSnapshot snapshot,
			ImageIdentityCheckInput imageIdentityCheckInput) {
		var metadata = snapshot.metadata();
		return new InstanceConformanceData(forwardNodeId, destinationId, sourceAet,
				metadata.getString(Tag.StudyInstanceUID, ""), metadata.getString(Tag.SeriesInstanceUID, ""),
				metadata.getString(Tag.SOPClassUID, ""), metadata.getString(Tag.SOPInstanceUID, ""),
				metadata.getString(Tag.Modality, ""), transferSyntaxUid, sent, failureReason, checkValueConformity,
				deepSequenceValidation, deidentified, snapshot, imageIdentityCheckInput);
	}

	public StudyKey studyKey() {
		return new StudyKey(forwardNodeId, destinationId, studyUid);
	}

}
