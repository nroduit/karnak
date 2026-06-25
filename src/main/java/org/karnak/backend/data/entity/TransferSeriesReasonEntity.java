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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/**
 * Per-reason error breakdown for a {@link TransferSeriesStatusEntity}: how many instances
 * of the series failed with a given reason. Replaces the per-instance error rows.
 */
@Entity(name = "TransferSeriesReason")
@Table(name = "transfer_series_reason")
public class TransferSeriesReasonEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long id;

	private Long seriesStatusId;

	private String reason;

	private long count;

	public TransferSeriesReasonEntity() {
	}

	public TransferSeriesReasonEntity(Long seriesStatusId, String reason, long count) {
		this.seriesStatusId = seriesStatusId;
		this.reason = reason;
		this.count = count;
	}

	@Id
	@SequenceGenerator(name = "TRANSFER_SERIES_REASON_GEN", sequenceName = "transfer_series_reason_sequence",
			allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "TRANSFER_SERIES_REASON_GEN")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "series_status_id")
	public Long getSeriesStatusId() {
		return seriesStatusId;
	}

	public void setSeriesStatusId(Long seriesStatusId) {
		this.seriesStatusId = seriesStatusId;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TransferSeriesReasonEntity that = (TransferSeriesReasonEntity) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}

}
