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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

/**
 * API model exposed by the REST layer for a profile element. Decouples the wire format
 * from {@link org.karnak.backend.data.entity.ProfileElementEntity}. The technical
 * database id and the position (managed by the reorder logic) are not exposed: the
 * public {@code uuid}, conveyed by the URL path, identifies the resource.
 */
@Getter
@Setter
@Validated
@NoArgsConstructor
public class ProfileElementModel implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private UUID uuid;

	@NotBlank(message = "Name is mandatory")
	private String name;

	@NotBlank(message = "Codename is mandatory")
	private String codename;

	private String condition;

	private String action;

	private String option;

	private List<String> tags;

	private List<String> excludedTags;

	private Map<String, String> arguments = new LinkedHashMap<>();

}

