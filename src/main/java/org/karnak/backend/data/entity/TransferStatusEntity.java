/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.data.entity;

import com.opencsv.bean.CsvDate;
import com.opencsv.bean.CsvRecurse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.img.util.DicomObjectUtil;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.karnak.backend.util.DateFormat;

@Entity(name = "TransferStatus")
@Table(name = "transfer_status")
public class TransferStatusEntity implements Serializable {

	private static final long serialVersionUID = -1542928573652195764L;

	private Long id;

	@CsvRecurse
	private ForwardNodeEntity forwardNodeEntity;

	private Long forwardNodeId;

	@CsvRecurse
	private DestinationEntity destinationEntity;

	private Long destinationId;

	@CsvDate(DateFormat.FORMAT_DDMMYYYY_SLASH_HHMMSS_2POINTS_SSSSSS_POINT)
	private LocalDateTime transferDate;

	private boolean sent;

	private boolean error;

	private String reason;

	// Original
	private String patientIdOriginal;

	private String accessionNumberOriginal;

	private String studyDescriptionOriginal;

	@CsvDate(DateFormat.FORMAT_DDMMYYYY_SLASH_HHMMSS_2POINTS_SSSSSS_POINT)
	private LocalDateTime studyDateOriginal;

	private String studyUidOriginal;

	private String serieDescriptionOriginal;

	@CsvDate(DateFormat.FORMAT_DDMMYYYY_SLASH_HHMMSS_2POINTS_SSSSSS_POINT)
	private LocalDateTime serieDateOriginal;

	private String serieUidOriginal;

	private String sopInstanceUidOriginal;

	// To send
	private String patientIdToSend;

	private String accessionNumberToSend;

	private String studyDescriptionToSend;

	@CsvDate(DateFormat.FORMAT_DDMMYYYY_SLASH_HHMMSS_2POINTS_SSSSSS_POINT)
	private LocalDateTime studyDateToSend;

	private String studyUidToSend;

	private String serieDescriptionToSend;

	@CsvDate(DateFormat.FORMAT_DDMMYYYY_SLASH_HHMMSS_2POINTS_SSSSSS_POINT)
	private LocalDateTime serieDateToSend;

	private String serieUidToSend;

	private String sopInstanceUidToSend;

	private String modality;

	private String sopClassUid;

	public TransferStatusEntity() {
	}

	public TransferStatusEntity(Long forwardNodeId, Long destinationId, LocalDateTime transferDate, boolean sent,
			boolean error, String reason, String patientIdOriginal, String accessionNumberOriginal,
			String studyDescriptionOriginal, LocalDateTime studyDateOriginal, String studyUidOriginal,
			String serieDescriptionOriginal, LocalDateTime serieDateOriginal, String serieUidOriginal,
			String sopInstanceUidOriginal, String patientIdToSend, String accessionNumberToSend,
			String studyDescriptionToSend, LocalDateTime studyDateToSend, String studyUidToSend,
			String serieDescriptionToSend, LocalDateTime serieDateToSend, String serieUidToSend,
			String sopInstanceUidToSend, String modality, String sopClassUid) {
		this.forwardNodeId = forwardNodeId;
		this.destinationId = destinationId;
		this.transferDate = transferDate;
		this.sent = sent;
		this.error = error;
		this.reason = reason;
		this.patientIdOriginal = patientIdOriginal;
		this.accessionNumberOriginal = accessionNumberOriginal;
		this.studyDescriptionOriginal = studyDescriptionOriginal;
		this.studyDateOriginal = studyDateOriginal;
		this.studyUidOriginal = studyUidOriginal;
		this.serieDescriptionOriginal = serieDescriptionOriginal;
		this.serieDateOriginal = serieDateOriginal;
		this.serieUidOriginal = serieUidOriginal;
		this.sopInstanceUidOriginal = sopInstanceUidOriginal;
		this.patientIdToSend = patientIdToSend;
		this.accessionNumberToSend = accessionNumberToSend;
		this.studyDescriptionToSend = studyDescriptionToSend;
		this.studyDateToSend = studyDateToSend;
		this.studyUidToSend = studyUidToSend;
		this.serieDescriptionToSend = serieDescriptionToSend;
		this.serieDateToSend = serieDateToSend;
		this.serieUidToSend = serieUidToSend;
		this.sopInstanceUidToSend = sopInstanceUidToSend;
		this.modality = modality;
		this.sopClassUid = sopClassUid;
	}

	public static TransferStatusEntity buildTransferStatusEntity(Long forwardNodeId, Long destinationId,
			Attributes attributesOriginal, Attributes attributesToSend, boolean sent, boolean error, String reason,
			String modality, String sopClassUid) {
		return new TransferStatusEntity(forwardNodeId, destinationId, LocalDateTime.now(ZoneId.of("CET")), sent, error,
				reason, attributesOriginal.getString(Tag.PatientID), attributesOriginal.getString(Tag.AccessionNumber),
				attributesOriginal.getString(Tag.StudyDescription),
				DicomObjectUtil.dateTime(attributesOriginal, Tag.StudyDate, Tag.StudyTime),
				attributesOriginal.getString(Tag.StudyInstanceUID), attributesOriginal.getString(Tag.SeriesDescription),
				DicomObjectUtil.dateTime(attributesOriginal, Tag.SeriesDate, Tag.SeriesTime),
				attributesOriginal.getString(Tag.SeriesInstanceUID), attributesOriginal.getString(Tag.SOPInstanceUID),
				attributesToSend.getString(Tag.PatientID), attributesToSend.getString(Tag.AccessionNumber),
				attributesToSend.getString(Tag.StudyDescription),
				DicomObjectUtil.dateTime(attributesToSend, Tag.StudyDate, Tag.StudyTime),
				attributesToSend.getString(Tag.StudyInstanceUID), attributesToSend.getString(Tag.SeriesDescription),
				DicomObjectUtil.dateTime(attributesToSend, Tag.SeriesDate, Tag.SeriesTime),
				attributesToSend.getString(Tag.SeriesInstanceUID), attributesToSend.getString(Tag.SOPInstanceUID),
				modality, sopClassUid);
	}

	@Id
	@SequenceGenerator(name = "TRANSFER_STATUS_GEN", sequenceName = "transfer_status_sequence", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "TRANSFER_STATUS_GEN")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(targetEntity = ForwardNodeEntity.class)
	@JoinColumn(name = "forward_node_id", nullable = false, insertable = false, updatable = false)
	public ForwardNodeEntity getForwardNodeEntity() {
		return forwardNodeEntity;
	}

	public void setForwardNodeEntity(ForwardNodeEntity forwardNodeEntity) {
		this.forwardNodeEntity = forwardNodeEntity;
	}

	@Column(name = "forward_node_id")
	public Long getForwardNodeId() {
		return forwardNodeId;
	}

	public void setForwardNodeId(Long forwardNodeId) {
		this.forwardNodeId = forwardNodeId;
	}

	@ManyToOne(targetEntity = DestinationEntity.class)
	@JoinColumn(name = "destination_id", nullable = false, insertable = false, updatable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	public DestinationEntity getDestinationEntity() {
		return destinationEntity;
	}

	public void setDestinationEntity(DestinationEntity destinationEntity) {
		this.destinationEntity = destinationEntity;
	}

	@Column(name = "destination_id")
	public Long getDestinationId() {
		return destinationId;
	}

	public void setDestinationId(Long destinationId) {
		this.destinationId = destinationId;
	}

	public LocalDateTime getTransferDate() {
		return transferDate;
	}

	public void setTransferDate(LocalDateTime transferDate) {
		this.transferDate = transferDate;
	}

	public boolean isSent() {
		return sent;
	}

	public void setSent(boolean sent) {
		this.sent = sent;
	}

	public boolean isError() {
		return error;
	}

	public void setError(boolean error) {
		this.error = error;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getPatientIdOriginal() {
		return patientIdOriginal;
	}

	public void setPatientIdOriginal(String patientIdOriginal) {
		this.patientIdOriginal = patientIdOriginal;
	}

	public String getAccessionNumberOriginal() {
		return accessionNumberOriginal;
	}

	public void setAccessionNumberOriginal(String accessionNumberOriginal) {
		this.accessionNumberOriginal = accessionNumberOriginal;
	}

	public String getStudyDescriptionOriginal() {
		return studyDescriptionOriginal;
	}

	public void setStudyDescriptionOriginal(String studyDescriptionOriginal) {
		this.studyDescriptionOriginal = studyDescriptionOriginal;
	}

	public LocalDateTime getStudyDateOriginal() {
		return studyDateOriginal;
	}

	public void setStudyDateOriginal(LocalDateTime studyDateOriginal) {
		this.studyDateOriginal = studyDateOriginal;
	}

	public String getStudyUidOriginal() {
		return studyUidOriginal;
	}

	public void setStudyUidOriginal(String studyUidOriginal) {
		this.studyUidOriginal = studyUidOriginal;
	}

	public String getSerieDescriptionOriginal() {
		return serieDescriptionOriginal;
	}

	public void setSerieDescriptionOriginal(String serieDescriptionOriginal) {
		this.serieDescriptionOriginal = serieDescriptionOriginal;
	}

	public LocalDateTime getSerieDateOriginal() {
		return serieDateOriginal;
	}

	public void setSerieDateOriginal(LocalDateTime serieDateOriginal) {
		this.serieDateOriginal = serieDateOriginal;
	}

	public String getSerieUidOriginal() {
		return serieUidOriginal;
	}

	public void setSerieUidOriginal(String serieUidOriginal) {
		this.serieUidOriginal = serieUidOriginal;
	}

	public String getSopInstanceUidOriginal() {
		return sopInstanceUidOriginal;
	}

	public void setSopInstanceUidOriginal(String sopInstanceUidOriginal) {
		this.sopInstanceUidOriginal = sopInstanceUidOriginal;
	}

	public String getPatientIdToSend() {
		return patientIdToSend;
	}

	public void setPatientIdToSend(String patientIdToSend) {
		this.patientIdToSend = patientIdToSend;
	}

	public String getAccessionNumberToSend() {
		return accessionNumberToSend;
	}

	public void setAccessionNumberToSend(String accessionNumberToSend) {
		this.accessionNumberToSend = accessionNumberToSend;
	}

	public String getStudyDescriptionToSend() {
		return studyDescriptionToSend;
	}

	public void setStudyDescriptionToSend(String studyDescriptionToSend) {
		this.studyDescriptionToSend = studyDescriptionToSend;
	}

	public LocalDateTime getStudyDateToSend() {
		return studyDateToSend;
	}

	public void setStudyDateToSend(LocalDateTime studyDateToSend) {
		this.studyDateToSend = studyDateToSend;
	}

	public String getStudyUidToSend() {
		return studyUidToSend;
	}

	public void setStudyUidToSend(String studyUidToSend) {
		this.studyUidToSend = studyUidToSend;
	}

	public String getSerieDescriptionToSend() {
		return serieDescriptionToSend;
	}

	public void setSerieDescriptionToSend(String serieDescriptionToSend) {
		this.serieDescriptionToSend = serieDescriptionToSend;
	}

	public LocalDateTime getSerieDateToSend() {
		return serieDateToSend;
	}

	public void setSerieDateToSend(LocalDateTime serieDateToSend) {
		this.serieDateToSend = serieDateToSend;
	}

	public String getSerieUidToSend() {
		return serieUidToSend;
	}

	public void setSerieUidToSend(String serieUidToSend) {
		this.serieUidToSend = serieUidToSend;
	}

	public String getSopInstanceUidToSend() {
		return sopInstanceUidToSend;
	}

	public void setSopInstanceUidToSend(String sopInstanceUidToSend) {
		this.sopInstanceUidToSend = sopInstanceUidToSend;
	}

	public String getModality() {
		return modality;
	}

	public void setModality(String modality) {
		this.modality = modality;
	}

	public String getSopClassUid() {
		return sopClassUid;
	}

	public void setSopClassUid(String sopClassUid) {
		this.sopClassUid = sopClassUid;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TransferStatusEntity that = (TransferStatusEntity) o;
		return sent == that.sent && Objects.equals(id, that.id)
				&& Objects.equals(forwardNodeEntity, that.forwardNodeEntity)
				&& Objects.equals(forwardNodeId, that.forwardNodeId)
				&& Objects.equals(destinationEntity, that.destinationEntity)
				&& Objects.equals(destinationId, that.destinationId) && Objects.equals(transferDate, that.transferDate)
				&& Objects.equals(reason, that.reason) && Objects.equals(patientIdOriginal, that.patientIdOriginal)
				&& Objects.equals(accessionNumberOriginal, that.accessionNumberOriginal)
				&& Objects.equals(studyDescriptionOriginal, that.studyDescriptionOriginal)
				&& Objects.equals(studyDateOriginal, that.studyDateOriginal)
				&& Objects.equals(studyUidOriginal, that.studyUidOriginal)
				&& Objects.equals(serieDescriptionOriginal, that.serieDescriptionOriginal)
				&& Objects.equals(serieDateOriginal, that.serieDateOriginal)
				&& Objects.equals(serieUidOriginal, that.serieUidOriginal)
				&& Objects.equals(sopInstanceUidOriginal, that.sopInstanceUidOriginal)
				&& Objects.equals(patientIdToSend, that.patientIdToSend)
				&& Objects.equals(accessionNumberToSend, that.accessionNumberToSend)
				&& Objects.equals(studyDescriptionToSend, that.studyDescriptionToSend)
				&& Objects.equals(studyDateToSend, that.studyDateToSend)
				&& Objects.equals(studyUidToSend, that.studyUidToSend)
				&& Objects.equals(serieDescriptionToSend, that.serieDescriptionToSend)
				&& Objects.equals(serieDateToSend, that.serieDateToSend)
				&& Objects.equals(serieUidToSend, that.serieUidToSend)
				&& Objects.equals(sopInstanceUidToSend, that.sopInstanceUidToSend)
				&& Objects.equals(modality, that.modality) && Objects.equals(sopClassUid, that.sopClassUid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, forwardNodeEntity, forwardNodeId, destinationEntity, destinationId, transferDate, sent,
				reason, patientIdOriginal, accessionNumberOriginal, studyDescriptionOriginal, studyDateOriginal,
				studyUidOriginal, serieDescriptionOriginal, serieDateOriginal, serieUidOriginal, sopInstanceUidOriginal,
				patientIdToSend, accessionNumberToSend, studyDescriptionToSend, studyDateToSend, studyUidToSend,
				serieDescriptionToSend, serieDateToSend, serieUidToSend, sopInstanceUidToSend, modality, sopClassUid);
	}

	@Override
	public String toString() {
		return "TransferStatusEntity{" + "id=" + id + ", forwardNodeEntity=" + forwardNodeEntity + ", forwardNodeId="
				+ forwardNodeId + ", destinationEntity=" + destinationEntity + ", destinationId=" + destinationId
				+ ", transferDate=" + transferDate + ", sent=" + sent + ", reason='" + reason + '\''
				+ ", patientIdOriginal='" + patientIdOriginal + '\'' + ", accessionNumberOriginal='"
				+ accessionNumberOriginal + '\'' + ", studyDescriptionOriginal='" + studyDescriptionOriginal + '\''
				+ ", studyDateOriginal=" + studyDateOriginal + ", studyUidOriginal='" + studyUidOriginal + '\''
				+ ", serieDescriptionOriginal='" + serieDescriptionOriginal + '\'' + ", serieDateOriginal="
				+ serieDateOriginal + ", serieUidOriginal='" + serieUidOriginal + '\'' + ", sopInstanceUidOriginal='"
				+ sopInstanceUidOriginal + '\'' + ", patientIdToSend='" + patientIdToSend + '\''
				+ ", accessionNumberToSend='" + accessionNumberToSend + '\'' + ", studyDescriptionToSend='"
				+ studyDescriptionToSend + '\'' + ", studyDateToSend=" + studyDateToSend + ", studyUidToSend='"
				+ studyUidToSend + '\'' + ", serieDescriptionToSend='" + serieDescriptionToSend + '\''
				+ ", serieDateToSend=" + serieDateToSend + ", serieUidToSend='" + serieUidToSend + '\''
				+ ", sopInstanceUidToSend='" + sopInstanceUidToSend + '\'' + ", modality='" + modality + '\''
				+ ", sopClassUid='" + sopClassUid + '\'' + '}';
	}

}
