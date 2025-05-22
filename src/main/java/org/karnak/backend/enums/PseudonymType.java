/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.enums;

public enum PseudonymType {

	CACHE_EXTID("Pseudonym is already stored in KARNAK"), EXTID_IN_TAG("Pseudonym is in a DICOM tag"),
	EXTID_API("Pseudonym from external API");

	private final String value;

	PseudonymType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}
