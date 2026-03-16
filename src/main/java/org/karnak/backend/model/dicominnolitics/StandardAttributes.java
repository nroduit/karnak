/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
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

public class StandardAttributes {

	private static final String ATTRIBUTES_FILENAME = "attributes.json";

	private StandardAttributes() {
	}

	public static jsonAttributes[] readJsonAttributes() {
		URL url = StandardCIODS.class.getResource(ATTRIBUTES_FILENAME);
		return read(url);
	}

	private static jsonAttributes[] read(URL url) {
		Gson gson = new Gson();
		try {
			JsonReader reader = new JsonReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
			return gson.fromJson(reader, jsonAttributes[].class);
		}
		catch (Exception e) {
			throw new JsonParseException(String.format("Cannot parse json %s correctly", ATTRIBUTES_FILENAME), e);
		}
	}

}
