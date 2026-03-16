/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.api.rqbody;

public class SearchIds {

	private String idType;

	private String idString;

	public SearchIds(String idType, String idString) {
		this.idType = idType;
		this.idString = idString;
	}

	public String get_idType() {
		return this.idType;
	}

	public void set_idType(String idType) {
		this.idType = idType;
	}

	public String get_idString() {
		return this.idString;
	}

	public void set_idString(String idString) {
		this.idString = idString;
	}

}
