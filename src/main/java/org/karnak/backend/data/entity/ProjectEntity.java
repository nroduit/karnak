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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.util.DateFormat;

@Entity(name = "Project")
@Table(name = "project")
public class ProjectEntity implements Serializable {

	private static final long serialVersionUID = 8809562914582842501L;

	private Long id;

	private String name;

	private List<SecretEntity> secretEntities;

	private List<DestinationEntity> destinationEntities;

	private ProfileEntity profileEntity;

	public ProjectEntity() {
		this.destinationEntities = new ArrayList<>();
		this.secretEntities = new ArrayList<>();
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@OneToMany(mappedBy = "projectEntity", cascade = CascadeType.ALL)
	@LazyCollection(LazyCollectionOption.FALSE)
	public List<SecretEntity> getSecretEntities() {
		return secretEntities;
	}

	public void setSecretEntities(List<SecretEntity> secretEntities) {
		this.secretEntities = secretEntities;
	}

	@OneToMany(mappedBy = "deIdentificationProjectEntity")
	@LazyCollection(LazyCollectionOption.FALSE)
	public List<DestinationEntity> getDestinationEntities() {
		return destinationEntities;
	}

	public void setDestinationEntities(List<DestinationEntity> destinationEntities) {
		this.destinationEntities = destinationEntities;
	}

	@ManyToOne
	@JoinColumn(name = "profile_pipe_id")
	public ProfileEntity getProfileEntity() {
		return profileEntity;
	}

	public void setProfileEntity(ProfileEntity profileEntity) {
		this.profileEntity = profileEntity;
	}

	/**
	 * Retrieve the active secret of the project
	 * @return active secret to be used
	 */
	public SecretEntity retrieveActiveSecret() {
		return secretEntities.stream().filter(SecretEntity::isActive).findFirst().orElse(null);
	}

	/**
	 * Set the secret in parameter in the list of secret of the project and activate it
	 * @param secretEntity Secret to add
	 */
	public void addActiveSecretEntity(SecretEntity secretEntity) {
		applyActiveSecret(secretEntity);
		secretEntities.add(secretEntity);
	}

	/**
	 * Activate the secret in parameter and deactivate others
	 * @param secretEntity Secret to activate
	 */
	public void applyActiveSecret(SecretEntity secretEntity) {
		secretEntities.forEach(s -> s.setActive(false));
		secretEntity.setActive(true);
	}

	/**
	 * Build a label for the combobox secret
	 * @param secretEntity Label is build for this secretEntity
	 * @return Label built
	 */
	public static String buildLabelSecret(SecretEntity secretEntity) {
		return "%s  [created: %s]".formatted(HMAC.showHexKey(HMAC.byteToHex(secretEntity.getSecretKey())),
				DateFormat.format(secretEntity.getCreationDate(), DateFormat.FORMAT_DDMMYYYY_SLASH_HHMMSS_2POINTS));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ProjectEntity that = (ProjectEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(name, that.name)
				&& Objects.equals(secretEntities, that.secretEntities)
				&& Objects.equals(destinationEntities, that.destinationEntities)
				&& Objects.equals(profileEntity, that.profileEntity);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, secretEntities, destinationEntities, profileEntity);
	}

}
