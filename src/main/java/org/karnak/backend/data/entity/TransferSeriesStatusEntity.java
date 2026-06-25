/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
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
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.karnak.backend.util.DateFormat;

/**
 * Aggregated monitoring row: one per (forward node, destination, series). Counters
 * ({@code instances}/{@code sent}/{@code errors}) are incremented as instances are
 * transferred; the study/series context is captured once on first occurrence. The
 * per-reason error breakdown lives in {@link TransferSeriesReasonEntity}.
 */
@Entity(name = "TransferSeriesStatus")
@Table(name = "transfer_series_status")
public class TransferSeriesStatusEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long id;

	@CsvRecurse
	private ForwardNodeEntity forwardNodeEntity;

	private Long forwardNodeId;

	@CsvRecurse
	private DestinationEntity destinationEntity;

	private Long destinationId;

	private String patientIdOriginal;

	private String patientIdToSend;

	private String accessionNumberOriginal;

	private String accessionNumberToSend;

	private String studyDescriptionOriginal;

	private String studyDescriptionToSend;

	@CsvDate(DateFormat.FORMAT_DDMMYYYY_SLASH_HHMMSS_2POINTS_SSSSSS_POINT)
	private LocalDateTime studyDateOriginal;

	@CsvDate(DateFormat.FORMAT_DDMMYYYY_SLASH_HHMMSS_2POINTS_SSSSSS_POINT)
	private LocalDateTime studyDateToSend;

	private String studyUidOriginal;

	private String studyUidToSend;

	private String serieDescriptionOriginal;

	private String serieDescriptionToSend;

	@CsvDate(DateFormat.FORMAT_DDMMYYYY_SLASH_HHMMSS_2POINTS_SSSSSS_POINT)
	private LocalDateTime serieDateOriginal;

	@CsvDate(DateFormat.FORMAT_DDMMYYYY_SLASH_HHMMSS_2POINTS_SSSSSS_POINT)
	private LocalDateTime serieDateToSend;

	private String serieUidOriginal;

	private String serieUidToSend;

	private String modality;

	private String sopClassUids;

	private long instances;

	private long sent;

	private long errors;

	@CsvDate(DateFormat.FORMAT_DDMMYYYY_SLASH_HHMMSS_2POINTS_SSSSSS_POINT)
	private LocalDateTime firstSeen;

	@CsvDate(DateFormat.FORMAT_DDMMYYYY_SLASH_HHMMSS_2POINTS_SSSSSS_POINT)
	private LocalDateTime lastSeen;

	// Filled only for the CSV export (joined distinct reasons), not persisted
	@Transient
	private String reasons;

	public TransferSeriesStatusEntity() {
	}

	@Id
	@SequenceGenerator(name = "TRANSFER_SERIES_STATUS_GEN", sequenceName = "transfer_series_status_sequence",
			allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "TRANSFER_SERIES_STATUS_GEN")
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

	public String getPatientIdOriginal() {
		return patientIdOriginal;
	}

	public void setPatientIdOriginal(String patientIdOriginal) {
		this.patientIdOriginal = patientIdOriginal;
	}

	public String getPatientIdToSend() {
		return patientIdToSend;
	}

	public void setPatientIdToSend(String patientIdToSend) {
		this.patientIdToSend = patientIdToSend;
	}

	public String getAccessionNumberOriginal() {
		return accessionNumberOriginal;
	}

	public void setAccessionNumberOriginal(String accessionNumberOriginal) {
		this.accessionNumberOriginal = accessionNumberOriginal;
	}

	public String getAccessionNumberToSend() {
		return accessionNumberToSend;
	}

	public void setAccessionNumberToSend(String accessionNumberToSend) {
		this.accessionNumberToSend = accessionNumberToSend;
	}

	public String getStudyDescriptionOriginal() {
		return studyDescriptionOriginal;
	}

	public void setStudyDescriptionOriginal(String studyDescriptionOriginal) {
		this.studyDescriptionOriginal = studyDescriptionOriginal;
	}

	public String getStudyDescriptionToSend() {
		return studyDescriptionToSend;
	}

	public void setStudyDescriptionToSend(String studyDescriptionToSend) {
		this.studyDescriptionToSend = studyDescriptionToSend;
	}

	public LocalDateTime getStudyDateOriginal() {
		return studyDateOriginal;
	}

	public void setStudyDateOriginal(LocalDateTime studyDateOriginal) {
		this.studyDateOriginal = studyDateOriginal;
	}

	public LocalDateTime getStudyDateToSend() {
		return studyDateToSend;
	}

	public void setStudyDateToSend(LocalDateTime studyDateToSend) {
		this.studyDateToSend = studyDateToSend;
	}

	public String getStudyUidOriginal() {
		return studyUidOriginal;
	}

	public void setStudyUidOriginal(String studyUidOriginal) {
		this.studyUidOriginal = studyUidOriginal;
	}

	public String getStudyUidToSend() {
		return studyUidToSend;
	}

	public void setStudyUidToSend(String studyUidToSend) {
		this.studyUidToSend = studyUidToSend;
	}

	public String getSerieDescriptionOriginal() {
		return serieDescriptionOriginal;
	}

	public void setSerieDescriptionOriginal(String serieDescriptionOriginal) {
		this.serieDescriptionOriginal = serieDescriptionOriginal;
	}

	public String getSerieDescriptionToSend() {
		return serieDescriptionToSend;
	}

	public void setSerieDescriptionToSend(String serieDescriptionToSend) {
		this.serieDescriptionToSend = serieDescriptionToSend;
	}

	public LocalDateTime getSerieDateOriginal() {
		return serieDateOriginal;
	}

	public void setSerieDateOriginal(LocalDateTime serieDateOriginal) {
		this.serieDateOriginal = serieDateOriginal;
	}

	public LocalDateTime getSerieDateToSend() {
		return serieDateToSend;
	}

	public void setSerieDateToSend(LocalDateTime serieDateToSend) {
		this.serieDateToSend = serieDateToSend;
	}

	public String getSerieUidOriginal() {
		return serieUidOriginal;
	}

	public void setSerieUidOriginal(String serieUidOriginal) {
		this.serieUidOriginal = serieUidOriginal;
	}

	public String getSerieUidToSend() {
		return serieUidToSend;
	}

	public void setSerieUidToSend(String serieUidToSend) {
		this.serieUidToSend = serieUidToSend;
	}

	public String getModality() {
		return modality;
	}

	public void setModality(String modality) {
		this.modality = modality;
	}

	public String getSopClassUids() {
		return sopClassUids;
	}

	public void setSopClassUids(String sopClassUids) {
		this.sopClassUids = sopClassUids;
	}

	public long getInstances() {
		return instances;
	}

	public void setInstances(long instances) {
		this.instances = instances;
	}

	public long getSent() {
		return sent;
	}

	public void setSent(long sent) {
		this.sent = sent;
	}

	public long getErrors() {
		return errors;
	}

	public void setErrors(long errors) {
		this.errors = errors;
	}

	public LocalDateTime getFirstSeen() {
		return firstSeen;
	}

	public void setFirstSeen(LocalDateTime firstSeen) {
		this.firstSeen = firstSeen;
	}

	public LocalDateTime getLastSeen() {
		return lastSeen;
	}

	public void setLastSeen(LocalDateTime lastSeen) {
		this.lastSeen = lastSeen;
	}

	@Transient
	public String getReasons() {
		return reasons;
	}

	public void setReasons(String reasons) {
		this.reasons = reasons;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TransferSeriesStatusEntity that = (TransferSeriesStatusEntity) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}

}
