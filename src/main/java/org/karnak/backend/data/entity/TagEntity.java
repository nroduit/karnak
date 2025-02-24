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

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import org.hibernate.annotations.DiscriminatorOptions;

/*https://stackoverflow.com/questions/14810287/hibernate-inheritance-and-relationship-mapping-generics/14919535*/
/*https://docs.jboss.org/hibernate/orm/5.3/javadocs/org/hibernate/annotations/DiscriminatorOptions.html*/
@Entity(name = "Tag")
@Table(name = "tag")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tag_type")
@DiscriminatorOptions(force = true)
public abstract class TagEntity implements Serializable {

	private static final long serialVersionUID = -1172918773653197764L;

	private Long id;

	private ProfileElementEntity profileElementEntity;

	private String tagValue;

	public TagEntity() {
	}

	public TagEntity(String tagValue, ProfileElementEntity profileElementEntity) {
		this.tagValue = tagValue;
		this.profileElementEntity = profileElementEntity;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne()
	@JoinColumn(name = "profile_element_id", nullable = false)
	public ProfileElementEntity getProfileElementEntity() {
		return profileElementEntity;
	}

	public void setProfileElementEntity(ProfileElementEntity profileElementEntity) {
		this.profileElementEntity = profileElementEntity;
	}

	public String getTagValue() {
		return tagValue;
	}

	public void setTagValue(String tagValue) {
		this.tagValue = tagValue;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TagEntity tagEntity = (TagEntity) o;
		return Objects.equals(id, tagEntity.id) && Objects.equals(tagValue, tagEntity.tagValue);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, tagValue);
	}

}
