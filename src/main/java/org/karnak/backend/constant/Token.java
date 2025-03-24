/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.constant;

/**
 * Constants dealing used for token management
 */
public class Token {

	// Name of the resource in the access token for retrieving roles
	public static final String RESOURCE_NAME = "karnak";

	// Access token resource access
	public static final String RESOURCE_ACCESS = "resource_access";

	// Roles
	public static final String ROLES = "roles";

	// Prefix of built roles
	public static final String PREFIX_ROLE = "ROLE_";

	private Token() {
	}

}
