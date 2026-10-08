/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.dicom;

import org.jspecify.annotations.Nullable;

/**
 * A DICOMweb (STOW-RS) destination configured in the gateway, identified by its
 * description, request URL and optional auth-configuration code. The web equivalent of
 * {@link ConfigNode} for the DICOM Tools.
 *
 * @param description human-readable label (falls back to the URL when unset)
 * @param url the STOW-RS request URL
 * @param authConfig the referenced auth-configuration code, or null when the destination
 * needs no authentication
 */
public record WebDestinationNode(String description, String url, @Nullable String authConfig) {

	public WebDestinationNode(String description, String url) {
		this(description, url, null);
	}
}
