/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.util;

import java.util.List;

/**
 * Outcome of parsing a CSV document: the detached entities that were read (no id, no
 * de-duplication) and one message per row that was skipped or adjusted. Persistence and
 * de-duplication are the caller's responsibility.
 *
 * @param entities the entities parsed from valid rows
 * @param errors per-row messages for rows skipped or adjusted by conformity validation
 */
public record CsvParseResult<T>(List<T> entities, List<String> errors) {
}
