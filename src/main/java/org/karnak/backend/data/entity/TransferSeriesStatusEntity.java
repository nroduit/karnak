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
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.util.DateFormat;

/**
 * Aggregated monitoring row: one per (forward node, destination, series). Counters
 * ({@code instances}/{@code sent}/{@code errors}) are incremented as instances are
 * transferred; the study/series context is captured once on first occurrence. The
 * per-reason error breakdown lives in {@link TransferSeriesReasonEntity}.
 */
@NullUnmarked
@Entity(name = "TransferSeriesStatus")
@Table(name = "transfer_series_status")
@Getter
@Setter
public class TransferSeriesStatusEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name = "TRANSFER_SERIES_STATUS_GEN", sequenceName = "transfer_series_status_sequence",
			allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "TRANSFER_SERIES_STATUS_GEN")
	private Long id;

	@ManyToOne(targetEntity = ForwardNodeEntity.class)
	@JoinColumn(name = "forward_node_id", nullable = false, insertable = false, updatable = false)
	@CsvRecurse
	private ForwardNodeEntity forwardNodeEntity;

	@Column(name = "forward_node_id")
	private Long forwardNodeId;

	@ManyToOne(targetEntity = DestinationEntity.class)
	@JoinColumn(name = "destination_id", nullable = false, insertable = false, updatable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@CsvRecurse
	private DestinationEntity destinationEntity;

	@Column(name = "destination_id")
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
