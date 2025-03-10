/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.standard;

import java.util.ArrayList;
import java.util.List;

public class SOP {

	private final String UID;

	private final String name;

	private final String ciod;

	private final String ciod_id;

	private final ArrayList<Module> modules;

	SOP(String UID, String name, String ciod, String ciod_id, ArrayList<Module> modules) {
		this.UID = UID;
		this.name = name;
		this.ciod = ciod;
		this.ciod_id = ciod_id;
		this.modules = modules;
	}

	public String getUID() {
		return UID;
	}

	public String getName() {
		return name;
	}

	public String getCiod() {
		return ciod;
	}

	public String getCiod_id() {
		return ciod_id;
	}

	public List<Module> getModules() {
		return modules;
	}

}
