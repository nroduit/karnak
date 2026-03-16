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

public class jsonAttributes {

	private String tag;

	private String name;

	private String keyword;

	private String valueRepresentation;

	private String valueMultiplicity;

	private String retired;

	private String id;

	public String getTag() {
		return tag;
	}

	public String getName() {
		return name;
	}

	public String getKeyword() {
		return keyword;
	}

	public String getValueRepresentation() {
		return valueRepresentation;
	}

	public String getValueMultiplicity() {
		return valueMultiplicity;
	}

	public String getRetired() {
		return retired;
	}

	public String getId() {
		return id;
	}

}
