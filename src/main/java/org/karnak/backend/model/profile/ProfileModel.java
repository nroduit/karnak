/*
 * Copyright (c) 2024-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profile;

import jakarta.validation.constraints.NotBlank;
import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

/**
 * API model exposed by the REST layer for a profile. Decouples the wire format from
 * {@link org.karnak.backend.data.entity.ProfileEntity}. The technical database id is not
 * exposed: the public {@code uuid}, conveyed by the URL path, identifies the resource.
 * The organizational group is managed through its own endpoint.
 */
@Getter
@Setter
@Validated
@NoArgsConstructor
public class ProfileModel implements Serializable {

	@Serial
	private static final long serialVersionUID = -2112595068130150799L;

	private UUID uuid;

	@NotBlank(message = "Name is mandatory")
	private String name;

	@NotBlank(message = "Version is mandatory")
	private String version;

	private String minimumKarnakVersion;

	// Read-only: true for the built-in profiles shipped with Karnak, which cannot be
	// updated or have their elements changed.
	private Boolean byDefault;

	private Set<ProfileElementModel> profileElements = new LinkedHashSet<>();

}

