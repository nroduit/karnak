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

public class StandardSOPS {

	private static final String SOPS_FILENAME = "sops.json";

	private final JsonSOP[] sops;

	public StandardSOPS() {
		this.sops = readJsonSOPS();
	}

	public static JsonSOP[] readJsonSOPS() {
		return JsonStandardReader.read(SOPS_FILENAME, JsonSOP[].class);
	}

	public JsonSOP[] getSOPS() {
		return sops;
	}

}