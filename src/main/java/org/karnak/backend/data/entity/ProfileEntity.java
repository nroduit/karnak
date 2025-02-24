/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.data.entity;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity(name = "Profile")
@Table(name = "profile")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({ "name", "version", "minimumKarnakVersion", "defaultIssuerOfPatientID", "profileElementEntities",
		"maskEntities" })
public class ProfileEntity implements Serializable {

	private static final long serialVersionUID = -7178858361090900170L;

	private Long id;

	private String name;

	private String version;

	private String minimumKarnakVersion;

	private String defaultIssuerOfPatientId; // not use in db but used in warning msg

	// profile

	private Boolean byDefault;

	private Set<ProfileElementEntity> profileElementEntities = new HashSet<>();

	private Set<MaskEntity> maskEntities = new HashSet<>();

	private List<ProjectEntity> projectEntities;

	public ProfileEntity() {
	}

	public ProfileEntity(String name, String version, String minimumKarnakVersion, String defaultIssuerOfPatientId) {
		this.name = name;
		this.version = version;
		this.minimumKarnakVersion = minimumKarnakVersion;
		this.defaultIssuerOfPatientId = defaultIssuerOfPatientId;
		this.byDefault = false;
	}

	public ProfileEntity(String name, String version, String minimumKarnakVersion, String defaultIssuerOfPatientId,
			Boolean byDefault) {
		this.name = name;
		this.version = version;
		this.minimumKarnakVersion = minimumKarnakVersion;
		this.defaultIssuerOfPatientId = defaultIssuerOfPatientId;
		this.byDefault = byDefault;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@JsonIgnore
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void addProfilePipe(ProfileElementEntity profileElementEntity) {
		this.profileElementEntities.add(profileElementEntity);
	}

	public void addMask(MaskEntity maskEntity) {
		this.maskEntities.add(maskEntity);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	@JsonGetter("minimumKarnakVersion")
	public String getMinimumKarnakVersion() {
		return minimumKarnakVersion;
	}

	@JsonSetter("minimumKarnakVersion")
	public void setMinimumKarnakVersion(String minimumKarnakVersion) {
		this.minimumKarnakVersion = minimumKarnakVersion;
	}

	@JsonGetter("defaultIssuerOfPatientID")
	@Transient // not use in db but used in warning msg profile
	public String getDefaultIssuerOfPatientId() {
		return defaultIssuerOfPatientId;
	}

	@JsonSetter("defaultIssuerOfPatientID")
	public void setDefaultIssuerOfPatientId(String defaultIssuerOfPatientId) {
		this.defaultIssuerOfPatientId = defaultIssuerOfPatientId;
	}

	@JsonGetter("profileElements")
	@OneToMany(mappedBy = "profileEntity", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	public Set<ProfileElementEntity> getProfileElementEntities() {
		return profileElementEntities;
	}

	@JsonSetter("profileElements")
	public void setProfileElementEntities(Set<ProfileElementEntity> profileElementEntities) {
		this.profileElementEntities = profileElementEntities;
	}

	@JsonIgnore
	@Column(name = "bydefault")
	public Boolean getByDefault() {
		return byDefault;
	}

	public void setByDefault(Boolean bydefault) {
		this.byDefault = bydefault;
	}

	@JsonGetter("masks")
	@OneToMany(mappedBy = "profileEntity", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	public Set<MaskEntity> getMaskEntities() {
		return maskEntities;
	}

	@JsonSetter("masks")
	public void setMaskEntities(Set<MaskEntity> maskEntities) {
		this.maskEntities = maskEntities;
	}

	@OneToMany(mappedBy = "profileEntity")
	@LazyCollection(LazyCollectionOption.FALSE)
	@JsonIgnore
	public List<ProjectEntity> getProjectEntities() {
		return projectEntities;
	}

	public void setProjectEntities(List<ProjectEntity> projectEntities) {
		this.projectEntities = projectEntities;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ProfileEntity that = (ProfileEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(version, that.version)
				&& Objects.equals(minimumKarnakVersion, that.minimumKarnakVersion)
				&& Objects.equals(defaultIssuerOfPatientId, that.defaultIssuerOfPatientId)
				&& Objects.equals(byDefault, that.byDefault);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, version, minimumKarnakVersion, defaultIssuerOfPatientId, byDefault);
	}

}
