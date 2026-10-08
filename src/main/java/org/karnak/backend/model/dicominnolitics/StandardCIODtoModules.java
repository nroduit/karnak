/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.dicominnolitics;

public class StandardCIODtoModules {

	private static final String CIOD_TO_MODULES_FILENAME = "ciod_to_modules.json";

	private final JsonCIODtoModule[] ciodToModules;

	public StandardCIODtoModules() {
		this.ciodToModules = readJsonCIODToModules();
	}

	public static JsonCIODtoModule[] readJsonCIODToModules() {
		return JsonStandardReader.read(CIOD_TO_MODULES_FILENAME, JsonCIODtoModule[].class);
	}

	public JsonCIODtoModule[] getCIODToModules() {
		return ciodToModules;
	}

}