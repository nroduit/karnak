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
 * Aggregated monitoring row: one per (forward node, destination, series). Counters are
 * incremented as instances are transferred; the study/series context is captured once on
 * first occurrence. Each outcome carries one delivery bucket — {@code sent},
 * {@code errors} or {@code excluded} (neither sent nor errored, e.g. filtered or aborted)
 * — and, when it can be attributed to an instance, one novelty bucket: {@code instances}
 * (distinct SOP instances, by original SOP Instance UID) or {@code retries} (re-sends of
 * an already-seen instance, including HTTP 409 "already present"). An outcome with no
 * original SOP Instance UID cannot be attributed, so it updates only its delivery bucket
 * and neither novelty bucket. The identities that back the novelty split live in
 * {@link TransferSeriesInstanceEntity} and the per-reason error breakdown in
 * {@link TransferSeriesReasonEntity}.
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

	private long retries;

	private long sent;

	private long errors;

	private long excluded;

	// Snapshot of sent / errors / excluded at the last email notification, so a
	// notification reports only the delta since it was last sent (not the cumulative
	// totals, which would re-report outcomes already notified when the row is touched
	// again). Not part of the CSV export (columns are defined explicitly elsewhere).
	@Column(name = "notified_sent")
	private long notifiedSent;

	@Column(name = "notified_errors")
	private long notifiedErrors;

	@Column(name = "notified_excluded")
	private long notifiedExcluded;

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
