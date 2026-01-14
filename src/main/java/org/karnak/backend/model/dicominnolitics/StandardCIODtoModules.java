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

public class StandardCIODtoModules {

	private static final String ciodToModulesFileName = "ciod_to_modules.json";

	private static jsonCIODtoModule[] ciodToModules;

	public StandardCIODtoModules() {
		URL url = this.getClass().getResource(ciodToModulesFileName);
		ciodToModules = read(url);
	}

	public static jsonCIODtoModule[] readJsonCIODToModules() {
		URL url = StandardCIODtoModules.class.getResource(ciodToModulesFileName);
		return read(url);
	}

	private static jsonCIODtoModule[] read(URL url) {
		Gson gson = new Gson();
		try {
			JsonReader reader = new JsonReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
			return gson.fromJson(reader, jsonCIODtoModule[].class);
		}
		catch (Exception e) {
			throw new JsonParseException(String.format("Cannot parse json %s correctly", ciodToModulesFileName), e);
		}
	}

	public jsonCIODtoModule[] getCIODToModules() {
		return ciodToModules;
	}

}
