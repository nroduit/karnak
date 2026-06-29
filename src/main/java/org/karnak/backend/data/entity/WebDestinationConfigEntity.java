/*
 * Copyright (c) 2026 Karnak Team and other contributors.
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
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.Objects;
import org.jspecify.annotations.NullUnmarked;

/**
 * A persisted DICOMweb endpoint: a base URL and the comma-separated DICOMweb services to
 * probe (empty meaning "all"), plus an optional organizational group. The web equivalent
 * of {@link DicomNodeConfigEntity}.
 */
@Entity(name = "WebDestinationConfig")
@Table(name = "web_destination")
@NullUnmarked
public class WebDestinationConfigEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long id;

	private String description;

	private String url;

	private String services = "";

	private String groupName;

	public WebDestinationConfigEntity() {
	}

	public WebDestinationConfigEntity(String description, String url, String services, String groupName) {
		this.description = description;
		this.url = url;
		this.services = services;
		this.groupName = groupName;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@NotBlank(message = "URL is mandatory")
	@Size(max = 1024, message = "URL has more than 1024 characters")
	@Column(name = "url")
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Column(name = "services")
	public String getServices() {
		return services;
	}

	public void setServices(String services) {
		this.services = services;
	}

	@Column(name = "group_name")
	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		WebDestinationConfigEntity that = (WebDestinationConfigEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(description, that.description)
				&& Objects.equals(url, that.url) && Objects.equals(services, that.services)
				&& Objects.equals(groupName, that.groupName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, description, url, services, groupName);
	}

	@Override
	public String toString() {
		return "WebDestinationConfig [id=" + id + ", description=" + description + ", url=" + url + ", services="
				+ services + ", groupName=" + groupName + "]";
	}

}
