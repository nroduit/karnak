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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.jspecify.annotations.NullUnmarked;

@Entity(name = "ForwardNode")
@Table(name = "forward_node")
@NullUnmarked
public class ForwardNodeEntity implements Serializable {

	private static final long serialVersionUID = 2095439136652046994L;

	private Long id;

	private String fwdDescription;

	// AETitle which defined a mapping of the gateway. This AETitle is configured as
	// a destination in the DICOM component that sends images to the gateway.
	private String fwdAeTitle;

	// Specification of a DICOM source node (the one which sends images to the
	// gateway). When no source node is defined all the DICOM nodes are accepted by
	// the gateway.
	private Set<DicomSourceNodeEntity> sourceNodes = new HashSet<>();

	// Specification of a final DICOM destination node. Multiple destinations can be
	// defined either as a DICOM or DICOMWeb type.
	private Set<DestinationEntity> destinationEntities = new HashSet<>();

	// Optional organizational group (null = shown at the root of the list)
	private ForwardNodeGroupEntity group;

	public ForwardNodeEntity() {
		this.fwdAeTitle = "";
		this.fwdDescription = "";
	}

	public ForwardNodeEntity(String fwdAeTitle) {
		this.fwdAeTitle = fwdAeTitle;
		this.fwdDescription = "";
	}

	public static ForwardNodeEntity ofEmpty() {
		return new ForwardNodeEntity();
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "description")
	public String getFwdDescription() {
		return this.fwdDescription;
	}

	public void setFwdDescription(String fwdDescription) {
		this.fwdDescription = fwdDescription;
	}

	@NotBlank(message = "Forward AETitle is mandatory")
	@Size(max = 16, message = "Forward AETitle has more than 16 characters")
	public String getFwdAeTitle() {
		return this.fwdAeTitle;
	}

	public void setFwdAeTitle(String fwdAeTitle) {
		this.fwdAeTitle = fwdAeTitle;
	}

	@OneToMany(mappedBy = "forwardNodeEntity", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	public Set<DicomSourceNodeEntity> getSourceNodes() {
		return this.sourceNodes;
	}

	public void setSourceNodes(Set<DicomSourceNodeEntity> sourceNodes) {
		this.sourceNodes = sourceNodes;
	}

	public void addSourceNode(DicomSourceNodeEntity sourceNode) {
		sourceNode.setForwardNodeEntity(this);
		this.sourceNodes.add(sourceNode);
	}

	public void removeSourceNode(DicomSourceNodeEntity sourceNode) {
		this.sourceNodes.remove(sourceNode);
	}

	@OneToMany(mappedBy = "forwardNodeEntity", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	public Set<DestinationEntity> getDestinationEntities() {
		return destinationEntities;
	}

	public void setDestinationEntities(Set<DestinationEntity> destinationEntities) {
		this.destinationEntities = destinationEntities;
	}

	public void addDestination(DestinationEntity destinationEntity) {
		destinationEntity.setForwardNodeEntity(this);
		this.destinationEntities.add(destinationEntity);
	}

	public void removeDestination(DestinationEntity destinationEntity) {
		this.destinationEntities.remove(destinationEntity);
	}

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "forward_node_group_id")
	public ForwardNodeGroupEntity getGroup() {
		return group;
	}

	public void setGroup(ForwardNodeGroupEntity group) {
		this.group = group;
	}

	/**
	 * Informs if this object matches with the filter as text.
	 * @param filterText the filter as text.
	 * @return true if this object matches with the filter as text; false otherwise.
	 */
	public boolean matchesFilter(String filterText) {
		if (contains(fwdAeTitle, filterText) //
				|| contains(fwdDescription, filterText)) {
			return true;
		}

		for (DicomSourceNodeEntity sourceNode : sourceNodes) {
			if (sourceNode.matchesFilter(filterText)) {
				return true;
			}
		}

		for (DestinationEntity destinationEntity : destinationEntities) {
			if (destinationEntity.matchesFilter(filterText)) {
				return true;
			}
		}

		return false;
	}

	private boolean contains(String value, String filterText) {
		return value != null && value.contains(filterText);
	}

	@Override
	public String toString() {
		return "ForwardNode [id=" + id + ", description=" + fwdDescription + ", fwdAeTitle=" + fwdAeTitle
				+ ", sourceNodes=" + sourceNodes + ", destinations=" + destinationEntities + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ForwardNodeEntity other = (ForwardNodeEntity) obj;
		if (id == null) {
			return other.id == null;
		}
		else {
			return id.equals(other.id);
		}
	}

}
