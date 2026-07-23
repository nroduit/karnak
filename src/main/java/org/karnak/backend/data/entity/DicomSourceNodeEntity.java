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

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;

@Entity(name = "DicomSourceNode")
@Table(name = "dicom_source_node")
@NullUnmarked
@Getter
@Setter
public class DicomSourceNodeEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	private String description;

	// AETitle of the source node.
	@NotBlank(message = "AETitle is mandatory")
	@Size(max = 16, message = "AETitle has more than 16 characters")
	private String aeTitle;

	// the host or IP of the source node. If the hostname exists then it is checked
	// (allows a restriction on the host not only in the AETitle).
	private String hostname;

	// if "true" check the hostname during the DICOM association and if not match
	// the connection is abort
	private Boolean checkHostname;

	@ManyToOne
	@JoinColumn(name = "forward_node_id")
	@JsonIgnore
	private ForwardNodeEntity forwardNodeEntity;

	public DicomSourceNodeEntity() {
		this.description = "";
		this.aeTitle = "";
		this.hostname = "";
		this.checkHostname = Boolean.FALSE;
	}

	public static DicomSourceNodeEntity ofEmpty() {
		return new DicomSourceNodeEntity();
	}

	/**
	 * Informs if this object matches with the filter as text.
	 * @param filterText the filter as text.
	 * @return true if this object matches with the filter as text; false otherwise.
	 */
	public boolean matchesFilter(String filterText) {
		return contains(description, filterText) //
				|| contains(aeTitle, filterText) //
				|| contains(hostname, filterText);
	}

	private boolean contains(String value, String filterText) {
		return value != null && value.contains(filterText);
	}

	@Override
	public String toString() {
		return "DicomSourceNode [id=" + id + ", description=" + description + ", aeTitle=" + aeTitle + ", hostname="
				+ hostname + ", checkHostname=" + checkHostname + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DicomSourceNodeEntity that = (DicomSourceNodeEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(description, that.description)
				&& Objects.equals(aeTitle, that.aeTitle) && Objects.equals(hostname, that.hostname)
				&& Objects.equals(checkHostname, that.checkHostname);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, description, aeTitle, hostname, checkHostname);
	}

}
