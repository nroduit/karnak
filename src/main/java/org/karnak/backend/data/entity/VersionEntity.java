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
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Entity(name = "Version")
@Table(name = "version")
public class VersionEntity implements Serializable {

	private Long id;

	private long gatewaySetup;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public long getGatewaySetup() {
		return gatewaySetup;
	}

	public void setGatewaySetup(long gatewaySetup) {
		this.gatewaySetup = gatewaySetup;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		VersionEntity that = (VersionEntity) o;
		return gatewaySetup == that.gatewaySetup && Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, gatewaySetup);
	}

}
