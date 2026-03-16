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

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class StandardModuleToAttributes {

	private static final String moduleToAttributesFileName = "module_to_attributes.json";

	private static jsonModuleToAttribute[] moduleToAttributes;

	public StandardModuleToAttributes() {
		URL url = this.getClass().getResource(moduleToAttributesFileName);
		moduleToAttributes = read(url);
	}

	public static jsonModuleToAttribute[] readJsonModuleToAttributes() {
		URL url = StandardModuleToAttributes.class.getResource(moduleToAttributesFileName);
		return read(url);
	}

	private static jsonModuleToAttribute[] read(URL url) {
		Gson gson = new Gson();
		try {
			JsonReader reader = new JsonReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
			return gson.fromJson(reader, jsonModuleToAttribute[].class);
		}
		catch (Exception e) {
			throw new JsonParseException(String.format("Cannot parse json %s correctly", moduleToAttributesFileName),
					e);
		}
	}

	public jsonModuleToAttribute[] getModuleToAttributes() {
		return moduleToAttributes;
	}

}
