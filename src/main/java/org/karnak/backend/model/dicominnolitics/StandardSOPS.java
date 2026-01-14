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

public class StandardSOPS {

	private static final String sopsFileName = "sops.json";

	private static jsonSOP[] sops;

	public StandardSOPS() {
		URL url = this.getClass().getResource(sopsFileName);
		sops = read(url);
	}

	public static jsonSOP[] readJsonSOPS() {
		URL url = StandardSOPS.class.getResource(sopsFileName);
		return read(url);
	}

	private static jsonSOP[] read(URL url) {
		Gson gson = new Gson();
		try {
			JsonReader reader = new JsonReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
			return gson.fromJson(reader, jsonSOP[].class);
		}
		catch (Exception e) {
			throw new JsonParseException(String.format("Cannot parse json %s correctly", sopsFileName), e);
		}
	}

	public jsonSOP[] getSOPS() {
		return sops;
	}

}
