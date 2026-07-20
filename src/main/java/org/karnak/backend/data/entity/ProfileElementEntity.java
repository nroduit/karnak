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
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.converter.ArgumentToMapConverter;
import org.karnak.backend.data.converter.TagListToStringListConverter;

@Entity(name = "ProfileElement")
@Table(name = "profile_element")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@NullUnmarked
@Getter
@Setter
public class ProfileElementEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@JsonIgnore
	private Long id;

	private String name;

	private String codename;

	private String condition;

	private String action;

	private String option;

	@JsonIgnore
	private Integer position;

	@ManyToOne
	@JoinColumn(name = "profile_id", nullable = false)
	@JsonIgnore
	private ProfileEntity profileEntity;

	@OneToMany(mappedBy = "profileElementEntity", cascade = CascadeType.ALL, fetch = FetchType.EAGER,
			orphanRemoval = true)
	private List<IncludedTagEntity> includedTagEntities = new ArrayList<>();

	@OneToMany(mappedBy = "profileElementEntity", cascade = CascadeType.ALL, fetch = FetchType.EAGER,
			orphanRemoval = true)
	private List<ExcludedTagEntity> excludedTagEntities = new ArrayList<>();

	@OneToMany(mappedBy = "profileElementEntity", cascade = CascadeType.ALL, fetch = FetchType.EAGER,
			orphanRemoval = true)
	private List<ArgumentEntity> argumentEntities = new ArrayList<>();

	public ProfileElementEntity() {
	}

	public ProfileElementEntity(String name, String codename, String condition, String action, String option,
			Integer position, ProfileEntity profileEntity) {
		this.name = name;
		this.codename = codename;
		this.condition = condition;
		this.action = action;
		this.option = option;
		this.position = position;
		this.profileEntity = profileEntity;
	}

	public ProfileElementEntity(String name, String codename, String condition, String action, String option,
			List<ArgumentEntity> argumentEntities, Integer position, ProfileEntity profileEntity) {
		this.name = name;
		this.codename = codename;
		this.condition = condition;
		this.action = action;
		this.option = option;
		this.argumentEntities = argumentEntities;
		this.position = position;
		this.profileEntity = profileEntity;
	}

	public void addIncludedTag(IncludedTagEntity includedtag) {
		this.includedTagEntities.add(includedtag);
	}

	public void addExceptedtags(ExcludedTagEntity exceptedtags) {
		this.excludedTagEntities.add(exceptedtags);
	}

	public void addArgument(ArgumentEntity argumentEntity) {
		this.argumentEntities.add(argumentEntity);
	}

	@JsonGetter("arguments")
	@JsonSerialize(converter = ArgumentToMapConverter.class)
	public List<ArgumentEntity> getArgumentEntities() {
		return argumentEntities;
	}

	@JsonSetter("arguments")
	public void setArgumentEntities(List<ArgumentEntity> argumentEntities) {
		this.argumentEntities = argumentEntities;
	}

	@JsonGetter("tags")
	@JsonSerialize(converter = TagListToStringListConverter.class)
	public List<IncludedTagEntity> getIncludedTagEntities() {
		return includedTagEntities;
	}

	@JsonSetter("tags")
	public void setIncludedTagEntities(List<IncludedTagEntity> includedTagEntities) {
		this.includedTagEntities = includedTagEntities;
	}

	@JsonGetter("excludedTags")
	@JsonSerialize(converter = TagListToStringListConverter.class)
	public List<ExcludedTagEntity> getExcludedTagEntities() {
		return excludedTagEntities;
	}

	@JsonSetter("excludedTags")
	public void setExcludedTagEntities(List<ExcludedTagEntity> excludedTagEntities) {
		this.excludedTagEntities = excludedTagEntities;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ProfileElementEntity that = (ProfileElementEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(codename, that.codename)
				&& Objects.equals(condition, that.condition) && Objects.equals(action, that.action)
				&& Objects.equals(option, that.option) && Objects.equals(position, that.position);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, codename, condition, action, option, position);
	}

}
