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
 * Identity of a distinct SOP instance already accounted for in a
 * {@link TransferSeriesStatusEntity}: one row per (series, original SOP Instance UID).
 * Its only purpose is to let the write path tell a first send of an instance apart from a
 * re-send (which increments {@code retries} instead of {@code instances}). Rows are
 * purged with their series through the {@code ON DELETE CASCADE} foreign key.
 */
@NullUnmarked
@Entity(name = "TransferSeriesInstance")
@Table(name = "transfer_series_instance")
@Getter
@Setter
public class TransferSeriesInstanceEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name = "TRANSFER_SERIES_INSTANCE_GEN", sequenceName = "transfer_series_instance_sequence",
			allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "TRANSFER_SERIES_INSTANCE_GEN")
	private Long id;

	@Column(name = "series_status_id")
	private Long seriesStatusId;

	@Column(name = "sop_instance_uid")
	private String sopInstanceUid;

	public TransferSeriesInstanceEntity() {
	}

	public TransferSeriesInstanceEntity(Long seriesStatusId, String sopInstanceUid) {
		this.seriesStatusId = seriesStatusId;
		this.sopInstanceUid = sopInstanceUid;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TransferSeriesInstanceEntity that = (TransferSeriesInstanceEntity) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}

}