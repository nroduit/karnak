/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.patient;

import java.io.Serial;
import java.io.Serializable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row of a {@link PseudonymImportResult} that did not result in a created pseudonym:
 * either it conflicted with an existing one, or it failed validation / lookup.
 */
@Getter
@Setter
@NoArgsConstructor
public class PseudonymImportIssue implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	// Index of the row in the request's JSON array (0-based).
	private int index;

	// Human-readable explanation, e.g. "Project not found" or a validation message.
	private String reason;

	// For a conflict, the pseudonym already stored for the same
	// (patientId + issuerOfPatientId + project); null for an error row.
	private PatientModel existing;

	public PseudonymImportIssue(int index, String reason) {
		this.index = index;
		this.reason = reason;
	}

	public PseudonymImportIssue(int index, String reason, PatientModel existing) {
		this.index = index;
		this.reason = reason;
		this.existing = existing;
	}

}
