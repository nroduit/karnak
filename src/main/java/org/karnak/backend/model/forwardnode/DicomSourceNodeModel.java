/*
 * Copyright (c) 2024-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.forwardnode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * API model exposed by the REST layer for a DICOM source node. Decouples the wire
 * format from {@link org.karnak.backend.data.entity.DicomSourceNodeEntity}. The
 * technical database id is not exposed: the public {@code uuid} is the identifier
 * conveyed by the URL path.
 */
@Getter
@Setter
@NoArgsConstructor
public class DicomSourceNodeModel implements Serializable {

	@Serial
	private static final long serialVersionUID = -3853017591096085699L;

	private UUID uuid;

	private String description;

	// AETitle of the source node.
	@NotBlank(message = "AETitle is mandatory")
	@Size(max = 16, message = "AETitle has more than 16 characters")
	private String aeTitle;

	private String hostname;

	private Boolean checkHostname;

}
