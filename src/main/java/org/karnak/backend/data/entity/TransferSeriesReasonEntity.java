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
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;

/**
 * Per-reason breakdown for a {@link TransferSeriesStatusEntity}: how many outcomes of the
 * series carried a given reason, counted in the same buckets as the series row —
 * {@code errorCount} for hard transfer errors, {@code excludedCount} for excluded
 * (aborted / filtered) outcomes, and {@code retryCount} for those that hit an
 * already-seen instance (the novelty axis, orthogonal to error/excluded). A bare 409
 * retry carries no reason.
 *
 * <p>
 * The {@code notifiedErrorCount} / {@code notifiedExcludedCount} columns snapshot the two
 * delivery counters at the last email notification, so the notification path can report
 * only reasons with a non-zero delta since — mirroring the per-series {@code notified*}
 * counters on {@link TransferSeriesStatusEntity}. Without this snapshot a reason recorded
 * in an earlier run would keep appearing in later notifications whose session had no such
 * outcome. ({@code retryCount} needs no snapshot: a reason is only ever booked on an
 * error/excluded outcome, which the delivery-axis delta already captures.)
 */
@NullUnmarked
@Entity(name = "TransferSeriesReason")
@Table(name = "transfer_series_reason")
@Getter
@Setter
public class TransferSeriesReasonEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name = "TRANSFER_SERIES_REASON_GEN", sequenceName = "transfer_series_reason_sequence",
			allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "TRANSFER_SERIES_REASON_GEN")
	private Long id;

	@Column(name = "series_status_id")
	private Long seriesStatusId;

	private String reason;

	@Column(name = "error_count")
	private long errorCount;

	@Column(name = "excluded_count")
	private long excludedCount;

	@Column(name = "retry_count")
	private long retryCount;

	@Column(name = "notified_error_count")
	private long notifiedErrorCount;

	@Column(name = "notified_excluded_count")
	private long notifiedExcludedCount;

	public TransferSeriesReasonEntity() {
	}

	public TransferSeriesReasonEntity(Long seriesStatusId, String reason, long errorCount, long excludedCount,
			long retryCount) {
		this.seriesStatusId = seriesStatusId;
		this.reason = reason;
		this.errorCount = errorCount;
		this.excludedCount = excludedCount;
		this.retryCount = retryCount;
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
