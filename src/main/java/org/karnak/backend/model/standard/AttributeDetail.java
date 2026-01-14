/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.standard;

public class AttributeDetail {

	private final String id;

	private final String keyword;

	private final String name;

	private final String retired;

	private final String tag;

	private final String valueMultiplicity;

	private final String valueRepresentation;

	public AttributeDetail(String id, String keyword, String name, String retired, String tag, String valueMultiplicity,
			String valueRepresentation) {
		this.id = id;
		this.keyword = keyword;
		this.name = name;
		this.retired = retired;
		this.tag = tag;
		this.valueMultiplicity = valueMultiplicity;
		this.valueRepresentation = valueRepresentation;
	}

	public String getId() {
		return id;
	}

	public String getKeyword() {
		return keyword;
	}

	public String getName() {
		return name;
	}

	public String getRetired() {
		return retired;
	}

	public String getTag() {
		return tag;
	}

	public String getValueMultiplicity() {
		return valueMultiplicity;
	}

	public String getValueRepresentation() {
		return valueRepresentation;
	}

}
