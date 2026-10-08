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
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

/**
 * API model exposed by the REST layer for a forward node.
 */
@Getter
@Setter
@Validated
@NoArgsConstructor
public class ForwardNodeModel implements Serializable {

	@Serial
	private static final long serialVersionUID = -4939483865789354368L;

	// The AETitle reaches the DICOM gateway (where it becomes a called AE title) and the
	// gateway logs. DICOM PS3.5 already excludes control characters and the backslash
	// from an AE title; enforcing it here also keeps a crafted payload from forging log
	// records with an embedded CR/LF.
	public static final String AE_TITLE = "[^\\p{Cntrl}\\\\]*";

	public static final String NO_CONTROL_CHARACTERS = "[^\\p{Cntrl}]*";

	private UUID uuid;

	@Pattern(regexp = NO_CONTROL_CHARACTERS, message = "Forward description contains control characters")
	private String fwdDescription;

	@NotBlank(message = "Forward AETitle is mandatory")
	@Size(max = 16, message = "Forward AETitle has more than 16 characters")
	@Pattern(regexp = AE_TITLE, message = "Forward AETitle contains characters not allowed in an AETitle")
	private String fwdAeTitle;

	private Set<DicomSourceNodeModel> sourceNodes = new HashSet<>();

	private Set<DestinationModel> destinations = new HashSet<>();

}
