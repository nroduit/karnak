/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profilebody;

import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;

@Getter
@Setter
@NullUnmarked
public class ProfileElementBody {

	private String name;

	private String codename;

	private String condition;

	private String action;

	private String option;

	private String args;

	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private List<String> tagEntities;

	private List<String> excludedTags;

	private Map<String, String> arguments;

	public List<String> getTags() {
		return tagEntities;
	}

	public void setTags(List<String> tagEntities) {
		this.tagEntities = tagEntities;
	}

}
