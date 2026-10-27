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
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Summary of a bulk pseudonym import ({@code POST /api/pseudonyms/import}). A partial
 * failure never fails the whole request: every row is attempted, and conflicting or
 * errored rows are reported here instead of aborting the batch.
 */
@Getter
@NoArgsConstructor
public class PseudonymImportResult implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private int created;

	private int conflict;

	private int error;

	private final List<PseudonymImportIssue> issues = new ArrayList<>();

	public void addCreated() {
		this.created++;
	}

	public void addConflict(int index, String reason, PatientModel existing) {
		this.conflict++;
		this.issues.add(new PseudonymImportIssue(index, reason, existing));
	}

	public void addError(int index, String reason) {
		this.error++;
		this.issues.add(new PseudonymImportIssue(index, reason));
	}

}
