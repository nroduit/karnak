/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.data.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.io.Serial;
import java.io.Serializable;

@Entity(name = "IncludedTag")
@DiscriminatorValue("IncludedTag")
public class IncludedTagEntity extends TagEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	public IncludedTagEntity() {
	}

	public IncludedTagEntity(String tagValue, ProfileElementEntity profileElementEntity) {
		super(tagValue, profileElementEntity);
	}

}
