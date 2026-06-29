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

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Objects;
import org.jspecify.annotations.NullUnmarked;

/**
 * Organizational group for {@link ProfileEntity}. A profile may belong to zero or one
 * group; ungrouped profiles keep showing at the root of the list.
 */
@Entity(name = "ProfileGroup")
@Table(name = "profile_group")
@NullUnmarked
public class ProfileGroupEntity implements NamedGroupEntity, Serializable {

	private static final long serialVersionUID = 1L;

	private Long id;

	private String name;

	public ProfileGroupEntity() {
	}

	public ProfileGroupEntity(String name) {
		this.name = name;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Override
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@NotBlank(message = "Group name is mandatory")
	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ProfileGroupEntity that = (ProfileGroupEntity) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}

}
