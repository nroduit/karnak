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

/**
 * Severity of a conformance finding. ERROR is a DICOM standard violation, WARNING a
 * recommended-practice deviation (e.g. missing Type 2), INFO is informational only. The
 * color is used by the HTML report renderer.
 */
public enum Severity {

	ERROR("#c0392b"), WARNING("#e67e22"), INFO("#7f8c8d");

	private final String color;

	Severity(String color) {
		this.color = color;
	}

	public String getColor() {
		return color;
	}

}
