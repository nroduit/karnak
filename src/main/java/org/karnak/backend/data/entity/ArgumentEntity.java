/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.data.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;

@Entity(name = "Arguments")
@Table(name = "arguments")
@NullUnmarked
@Getter
@Setter
public class ArgumentEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@JsonIgnore
	private Long id;

	@ManyToOne
	@JoinColumn(name = "profile_element_id", nullable = false)
	private ProfileElementEntity profileElementEntity;

	private String argumentKey;

	@Column(columnDefinition = "text")
	private String argumentValue;

	public ArgumentEntity() {
	}

	public ArgumentEntity(String argumentKey, String argumentValue) {
		this.argumentKey = argumentKey;
		this.argumentValue = argumentValue;
	}

	public ArgumentEntity(String argumentKey, String argumentValue, ProfileElementEntity profileElementEntity) {
		this.argumentKey = argumentKey;
		this.argumentValue = argumentValue;
		this.profileElementEntity = profileElementEntity;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ArgumentEntity that = (ArgumentEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(argumentKey, that.argumentKey)
				&& Objects.equals(argumentValue, that.argumentValue);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, argumentKey, argumentValue);
	}

}
