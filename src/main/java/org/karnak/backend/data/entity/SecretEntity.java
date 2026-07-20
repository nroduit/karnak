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
import lombok.Getter;
import lombok.Setter;
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

	@ManyToOne
	@JoinColumn(name = "project_id")
	private ProjectEntity projectEntity;

	private byte[] secretKey;

	private LocalDateTime creationDate;

	private boolean active;

	public SecretEntity() {
	}

	public SecretEntity(byte[] secretKey) {
		this.secretKey = secretKey;
		this.creationDate = LocalDateTime.now(ZoneId.of("CET"));
	}

	public SecretEntity(ProjectEntity projectEntity, byte[] secretKey) {
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
		return active == that.active && Objects.equals(id, that.id) && Objects.equals(projectEntity, that.projectEntity)
				&& Arrays.equals(secretKey, that.secretKey) && Objects.equals(creationDate, that.creationDate);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(id, projectEntity, creationDate, active);
		result = 31 * result + Arrays.hashCode(secretKey);
		return result;
	}

}
