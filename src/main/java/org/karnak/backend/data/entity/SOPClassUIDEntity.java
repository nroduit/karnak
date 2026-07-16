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
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;

@Entity(name = "SOPClassUID")
@Table(name = "sop_class_uid")
@NullUnmarked
@Getter
@Setter
public class SOPClassUIDEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	private String ciod;

	private String uid;

	private String name;

	public SOPClassUIDEntity() {
	}

	public SOPClassUIDEntity(String ciod, String uid, String name) {
		this.ciod = ciod;
		this.uid = uid;
		this.name = name;
	}

	public SOPClassUIDEntity(String ciod) {
		this.ciod = ciod;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SOPClassUIDEntity that = (SOPClassUIDEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(ciod, that.ciod) && Objects.equals(uid, that.uid)
				&& Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, ciod, uid, name);
	}

}
