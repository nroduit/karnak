/*
 * Copyright (c) 2024-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.project;

import jakarta.validation.constraints.NotBlank;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

/**
 * API model exposed by the REST layer for a project. Decouples the wire format from
 * {@link org.karnak.backend.data.entity.ProjectEntity}. The technical database id is not
 * exposed: the public {@code uuid}, conveyed by the URL path, identifies the resource.
 * Secrets (HMAC keys) are never exposed here for security reasons, and destinations are
 * managed through their own endpoint; only their public uuids are listed for reference.
 * The organizational group is managed through its own endpoint.
 */
@Getter
@Setter
@Validated
@NoArgsConstructor
public class ProjectModel implements Serializable {

	@Serial
	private static final long serialVersionUID = -5924005525998806991L;

	private UUID uuid;

	@NotBlank(message = "Name is mandatory")
	private String name;

	// The de-identification profile applied by this project, referenced by its public
	// uuid.
	@NotNull(message = "Profile uuid is mandatory")
	private UUID profileUuid;

	// Read-only: public uuids of the destinations using this project for
	// de-identification. Destinations themselves are managed via ForwardNodeController.
	private Set<UUID> destinationUuids = new HashSet<>();

}

