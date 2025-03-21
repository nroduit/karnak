/*
 * Copyright (c) 2022 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.enums;

/**
 * Enum for the transfer status
 */
public enum TransferStatusType {

	ALL(null, null, "All"), SENT(true, false, "Sent"), NOT_SENT(false, null, "Not Sent"),
	EXCLUDED(false, false, "Excluded"), ERROR(false, true, "Error");

	/**
	 * Predicate value for the sent attribute
	 */
	private final Boolean sent;

	/**
	 * Predicate value for the error attribute
	 */
	private final Boolean error;

	/**
	 * Label of the filter value
	 */
	private final String label;

	TransferStatusType(Boolean sent, Boolean error, String label) {
		this.label = label;
		this.sent = sent;
		this.error = error;
	}

	public Boolean getSent() {
		return sent;
	}

	public Boolean getError() {
		return error;
	}

	public String getLabel() {
		return label;
	}

}
