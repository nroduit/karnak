/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.standard;

import java.util.List;
import lombok.Getter;
import org.jspecify.annotations.NullUnmarked;

@Getter
@NullUnmarked
public class SOP {

	private final String UID;

	private final String name;

	private final String ciod;

	private final String ciod_id;

	private final List<Module> modules;

	SOP(String UID, String name, String ciod, String ciod_id, List<Module> modules) {
		this.UID = UID;
		this.name = name;
		this.ciod = ciod;
		this.ciod_id = ciod_id;
		this.modules = modules;
	}

}
