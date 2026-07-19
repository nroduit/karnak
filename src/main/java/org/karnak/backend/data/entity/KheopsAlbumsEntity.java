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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;

@Entity(name = "KheopsAlbums")
@Table(name = "kheops_albums")
@NullUnmarked
@Getter
@Setter
public class KheopsAlbumsEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	private String urlAPI;

	private String authorizationDestination;

	private String authorizationSource;

	private String condition;

	@ManyToOne
	@JoinColumn(name = "destination_id")
	@JsonIgnore
	private DestinationEntity destinationEntity = new DestinationEntity();

	public KheopsAlbumsEntity() {
	}

	public KheopsAlbumsEntity(String urlAPI, String authorizationDestination, String authorizationSource,
			String condition) {
		this.urlAPI = urlAPI;
		this.authorizationDestination = authorizationDestination;
		this.authorizationSource = authorizationSource;
		this.condition = condition;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		KheopsAlbumsEntity that = (KheopsAlbumsEntity) o;
		return Objects.equals(urlAPI, that.urlAPI)
				&& Objects.equals(authorizationDestination, that.authorizationDestination)
				&& Objects.equals(authorizationSource, that.authorizationSource)
				&& Objects.equals(condition, that.condition);
	}

	@Override
	public int hashCode() {
		return Objects.hash(urlAPI, authorizationDestination, authorizationSource, condition);
	}

	@Override
	public String toString() {
		return "KheopsAlbumsEntity{" + "id=" + id + ", urlAPI='" + urlAPI + '\'' + ", authorizationDestination='"
				+ authorizationDestination + '\'' + ", authorizationSource='" + authorizationSource + '\''
				+ ", condition='" + condition + '\'' + ", destinationEntity=" + destinationEntity + '}';
	}

}
