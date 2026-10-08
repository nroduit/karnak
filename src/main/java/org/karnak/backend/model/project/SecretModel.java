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
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * API model exposed by the REST layer for a project secret (HMAC key). Decouples the wire
 * format from {@link org.karnak.backend.data.entity.SecretEntity}. The raw key bytes are
 * never exposed: the key is conveyed as its hexadecimal representation (dashed groups),
 * matching the format used by the frontend. The public {@code uuid} identifies the
 * resource in the URL path.
 */
@Getter
@Setter
@NoArgsConstructor
public class SecretModel implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private UUID uuid;

	// Hexadecimal representation of the key (dashed groups), e.g.
	// "01234567-89ab-cdef-0123-456789abcdef".
	@NotBlank
	private String key;

	private LocalDateTime creationDate;

	private boolean active;

}
