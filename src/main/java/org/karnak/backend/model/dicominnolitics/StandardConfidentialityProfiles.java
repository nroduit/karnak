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

public class StandardConfidentialityProfiles {

	private static final String confidentialityProfilesFileName = "confidentiality_profile_attributes.json";

	private static jsonConfidentialityProfiles[] confidentialityProfiles;

	public StandardConfidentialityProfiles() {
		URL url = this.getClass().getResource(confidentialityProfilesFileName);
		Gson gson = new Gson();
		try {
			JsonReader reader = new JsonReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
			confidentialityProfiles = gson.fromJson(reader, jsonConfidentialityProfiles[].class);
		}
		catch (Exception e) {
			throw new JsonParseException(
					String.format("Cannot parse json %s correctly", confidentialityProfilesFileName), e);
		}
	}

	public static jsonConfidentialityProfiles[] getConfidentialityProfiles() {
		return confidentialityProfiles;
	}

}
