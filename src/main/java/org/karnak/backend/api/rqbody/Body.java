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

public class Body {

	private String type;

	private Data data;

	public Body(String type, Data data) {
		this.type = type;
		this.data = data;
	}

	public String get_type() {
		return this.type;
	}

	public void set_type(String type) {
		this.type = type;
	}

	public Data get_data() {
		return this.data;
	}

	public void set_data(Data data) {
		this.data = data;
	}

}
