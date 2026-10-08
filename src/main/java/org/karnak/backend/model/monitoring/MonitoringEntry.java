/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.monitoring;

import java.time.LocalDateTime;
import java.time.ZoneId;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.img.util.DicomObjectUtil;

/**
 * Immutable payload carried by a transfer-monitoring event: the outcome of forwarding one
 * SOP instance to one destination, with the study/series context (original and to-send).
 * It is folded into the aggregated {@code transfer_series_status} row by the listener.
 *
 * <p>
 * {@code sopInstanceUidOriginal} is the stable identity used to tell a first send of an
 * instance apart from a re-send; {@code duplicate} marks an outcome that is inherently a
 * re-send (the destination reported the object was already present, HTTP 409).
 */
public record MonitoringEntry(Long forwardNodeId, Long destinationId, LocalDateTime timestamp, boolean sent,
		boolean error, boolean duplicate, String reason, String patientIdOriginal, String patientIdToSend,
		String accessionNumberOriginal, String accessionNumberToSend, String studyDescriptionOriginal,
		String studyDescriptionToSend, LocalDateTime studyDateOriginal, LocalDateTime studyDateToSend,
		String studyUidOriginal, String studyUidToSend, String serieDescriptionOriginal, String serieDescriptionToSend,
		LocalDateTime serieDateOriginal, LocalDateTime serieDateToSend, String serieUidOriginal, String serieUidToSend,
		String sopInstanceUidOriginal, String modality, String sopClassUid) {

	/** Builds an entry from the original and de-identified DICOM attributes. */
	public static MonitoringEntry of(Long forwardNodeId, Long destinationId, Attributes attributesOriginal,
			Attributes attributesToSend, boolean sent, boolean error, boolean duplicate, String reason, String modality,
			String sopClassUid) {
		return new MonitoringEntry(forwardNodeId, destinationId, LocalDateTime.now(ZoneId.of("CET")), sent, error,
				duplicate, reason, attributesOriginal.getString(Tag.PatientID),
				attributesToSend.getString(Tag.PatientID), attributesOriginal.getString(Tag.AccessionNumber),
				attributesToSend.getString(Tag.AccessionNumber), attributesOriginal.getString(Tag.StudyDescription),
				attributesToSend.getString(Tag.StudyDescription),
				DicomObjectUtil.dateTime(attributesOriginal, Tag.StudyDate, Tag.StudyTime),
				DicomObjectUtil.dateTime(attributesToSend, Tag.StudyDate, Tag.StudyTime),
				attributesOriginal.getString(Tag.StudyInstanceUID), attributesToSend.getString(Tag.StudyInstanceUID),
				attributesOriginal.getString(Tag.SeriesDescription), attributesToSend.getString(Tag.SeriesDescription),
				DicomObjectUtil.dateTime(attributesOriginal, Tag.SeriesDate, Tag.SeriesTime),
				DicomObjectUtil.dateTime(attributesToSend, Tag.SeriesDate, Tag.SeriesTime),
				attributesOriginal.getString(Tag.SeriesInstanceUID), attributesToSend.getString(Tag.SeriesInstanceUID),
				attributesOriginal.getString(Tag.SOPInstanceUID), modality, sopClassUid);
	}

}
