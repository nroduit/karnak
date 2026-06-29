/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.validation;

import java.util.List;
import org.jspecify.annotations.NullUnmarked;

/**
 * The curated allowed-value set for an attribute. {@code closed} distinguishes a DICOM
 * <em>Enumerated Values</em> attribute (a closed set: any other value is a standard
 * violation → ERROR) from a <em>Defined Terms</em> attribute (an open set: other values
 * are permitted, so an unexpected one is only worth a WARNING). The fields are populated
 * by Gson reflection.
 */
@NullUnmarked
public class EnumeratedRule {

	private List<String> values;

	private Boolean closed;

	public List<String> getValues() {
		return values == null ? List.of() : values;
	}

	/**
	 * True for a closed Enumerated Values set; defaults to false (Defined Terms /
	 * WARNING).
	 */
	public boolean isClosed() {
		return Boolean.TRUE.equals(closed);
	}

}