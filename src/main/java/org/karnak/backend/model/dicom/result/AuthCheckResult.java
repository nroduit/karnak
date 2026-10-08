/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.dicom.result;

import org.jspecify.annotations.Nullable;

/**
 * Outcome of the OAuth 2.0 token check for a DICOMweb destination that references an auth
 * configuration: whether a client-credentials access token could actually be obtained
 * from the identity provider, and the provider error otherwise.
 *
 * @param authConfig the referenced auth-configuration code
 * @param acquired whether an access token was obtained
 * @param error the identity-provider / configuration error when not acquired
 */
public record AuthCheckResult(String authConfig, boolean acquired, @Nullable String error) {

	public String getSummary() {
		if (this.acquired) {
			return "Auth '" + this.authConfig + "': access token obtained";
		}
		return "Auth '" + this.authConfig + "': token request failed" + (this.error != null ? " — " + this.error : "");
	}

}
