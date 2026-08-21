/*
 * Copyright (c) 2022-2026 Karnak Team and other contributors.
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.NullUnmarked;

@Entity
@Table(name = "secret")
@NullUnmarked
@Getter
@Setter
public class SecretEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	// Public, stable identifier used in the REST API (URL paths) instead of the
	// technical database id.
	@Column(name = "uuid", unique = true, nullable = false, updatable = false)
	@JdbcTypeCode(SqlTypes.UUID)
	private UUID uuid;

	@ManyToOne
	@JoinColumn(name = "project_id")
	private ProjectEntity projectEntity;

	private byte[] secretKey;

	private LocalDateTime creationDate;

	private boolean active;

	public SecretEntity() {
		this.uuid = UUID.randomUUID();
	}

	public SecretEntity(byte[] secretKey) {
		this.uuid = UUID.randomUUID();
		this.secretKey = secretKey;
		this.creationDate = LocalDateTime.now(ZoneId.of("CET"));
	}

	public SecretEntity(ProjectEntity projectEntity, byte[] secretKey) {
		this.uuid = UUID.randomUUID();
		this.projectEntity = projectEntity;
		this.secretKey = secretKey;
		this.creationDate = LocalDateTime.now(ZoneId.of("CET"));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SecretEntity that = (SecretEntity) o;
		return active == that.active && Objects.equals(id, that.id) && Objects.equals(uuid, that.uuid)
				&& Objects.equals(projectEntity, that.projectEntity)
				&& Arrays.equals(secretKey, that.secretKey) && Objects.equals(creationDate, that.creationDate);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(id, uuid, projectEntity, creationDate, active);
		result = 31 * result + Arrays.hashCode(secretKey);
		return result;
	}

}
