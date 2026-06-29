/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.enums;

import lombok.Getter;

/**
 * The DICOMweb (PS3.18) RESTful services a destination can be probed for.
 */
@Getter
public enum DicomWebServiceType {

	STOW_RS("STOW-RS", "Store"),

	QIDO_RS("QIDO-RS", "Query"),

	WADO_RS("WADO-RS", "Retrieve"),

	WADO_URI("WADO-URI", "Retrieve (legacy)"),

	UPS_RS("UPS-RS", "Worklist"),

	CAPABILITIES("Capabilities", "Discovery");

	private final String displayName;

	private final String role;

	DicomWebServiceType(String displayName, String role) {
		this.displayName = displayName;
		this.role = role;
	}

}
