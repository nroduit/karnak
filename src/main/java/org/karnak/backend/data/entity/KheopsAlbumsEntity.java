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
import java.io.Serializable;
import java.util.Objects;

@Entity(name = "KheopsAlbums")
@Table(name = "kheops_albums")
public class KheopsAlbumsEntity implements Serializable {

	private static final long serialVersionUID = -3315720301354286325L;

	private Long id;

	private String urlAPI;

	private String authorizationDestination;

	private String authorizationSource;

	private String condition;

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

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUrlAPI() {
		return urlAPI;
	}

	public void setUrlAPI(String urlAPI) {
		this.urlAPI = urlAPI;
	}

	public String getAuthorizationDestination() {
		return authorizationDestination;
	}

	public void setAuthorizationDestination(String authorizationDestination) {
		this.authorizationDestination = authorizationDestination;
	}

	public String getAuthorizationSource() {
		return authorizationSource;
	}

	public void setAuthorizationSource(String authorizationSource) {
		this.authorizationSource = authorizationSource;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	@ManyToOne
	@JoinColumn(name = "destination_id")
	public DestinationEntity getDestinationEntity() {
		return destinationEntity;
	}

	public void setDestinationEntity(DestinationEntity destinationEntity) {
		this.destinationEntity = destinationEntity;
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
