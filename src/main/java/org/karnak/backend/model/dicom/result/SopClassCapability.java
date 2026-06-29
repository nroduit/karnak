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

import java.util.List;

/**
 * One accepted abstract syntax (SOP Class) reported by a peer during a non-invasive
 * capability probe, together with the transfer syntaxes the peer accepted for it. Maps to
 * a single row in the capabilities grid.
 *
 * @param category functional grouping (Storage, Query/Retrieve, Worklist, …)
 * @param sopClassName human-readable SOP Class name
 * @param sopClassUid SOP Class UID
 * @param transferSyntaxes human-readable names of the accepted transfer syntaxes
 */
public record SopClassCapability(String category, String sopClassName, String sopClassUid,
		List<String> transferSyntaxes) {
}