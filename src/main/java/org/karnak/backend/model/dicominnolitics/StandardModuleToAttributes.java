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

public class StandardModuleToAttributes {

	private static final String MODULE_TO_ATTRIBUTES_FILENAME = "module_to_attributes.json";

	private final JsonModuleToAttribute[] moduleToAttributes;

	public StandardModuleToAttributes() {
		this.moduleToAttributes = readJsonModuleToAttributes();
	}

	public static JsonModuleToAttribute[] readJsonModuleToAttributes() {
		return JsonStandardReader.read(MODULE_TO_ATTRIBUTES_FILENAME, JsonModuleToAttribute[].class);
	}

	public JsonModuleToAttribute[] getModuleToAttributes() {
		return moduleToAttributes;
	}

}