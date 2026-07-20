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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;

@Entity(name = "Profile")
@Table(name = "profile")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({ "name", "version", "minimumKarnakVersion", "defaultIssuerOfPatientID", "profileElementEntities",
		"maskEntities" })
@NullUnmarked
@Getter
@Setter
public class ProfileEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	private String name;

	private String version;

	private String minimumKarnakVersion;

	@Transient // not use in db but used in warning msg profile
	private String defaultIssuerOfPatientId; // not use in db but used in warning msg

	// profile

	@Column(name = "bydefault")
	private Boolean byDefault;

	@OneToMany(mappedBy = "profileEntity", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	private Set<ProfileElementEntity> profileElementEntities = new HashSet<>();

	@OneToMany(mappedBy = "profileEntity", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	private Set<MaskEntity> maskEntities = new HashSet<>();

	@OneToMany(mappedBy = "profileEntity", fetch = FetchType.EAGER)
	private List<ProjectEntity> projectEntities;

	// Optional organizational group (null = shown at the root of the list)
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "profile_group_id")
	private ProfileGroupEntity group;

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

	@JsonIgnore
	public Long getId() {
		return id;
	}

	public void addProfilePipe(ProfileElementEntity profileElementEntity) {
		this.profileElementEntities.add(profileElementEntity);
	}

	public void addMask(MaskEntity maskEntity) {
		this.maskEntities.add(maskEntity);
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
	public String getDefaultIssuerOfPatientId() {
		return defaultIssuerOfPatientId;
	}

	@JsonSetter("defaultIssuerOfPatientID")
	public void setDefaultIssuerOfPatientId(String defaultIssuerOfPatientId) {
		this.defaultIssuerOfPatientId = defaultIssuerOfPatientId;
	}

	@JsonGetter("profileElements")
	public Set<ProfileElementEntity> getProfileElementEntities() {
		return profileElementEntities;
	}

	@JsonSetter("profileElements")
	public void setProfileElementEntities(Set<ProfileElementEntity> profileElementEntities) {
		this.profileElementEntities = profileElementEntities;
	}

	@JsonIgnore
	public Boolean getByDefault() {
		return byDefault;
	}

	@JsonGetter("masks")
	public Set<MaskEntity> getMaskEntities() {
		return maskEntities;
	}

	@JsonSetter("masks")
	public void setMaskEntities(Set<MaskEntity> maskEntities) {
		this.maskEntities = maskEntities;
	}

	@JsonIgnore
	public List<ProjectEntity> getProjectEntities() {
		return projectEntities;
	}

	@JsonIgnore
	public ProfileGroupEntity getGroup() {
		return group;
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
