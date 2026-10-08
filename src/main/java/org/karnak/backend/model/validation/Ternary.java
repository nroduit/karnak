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
 * Three-valued result of evaluating a conditional-requirement predicate against a
 * dataset. {@link #UNKNOWN} means the condition references an attribute that is itself
 * absent (or the predicate is malformed), so the requirement cannot be resolved and the
 * validator stays silent — never raising a finding on an unresolved condition.
 */
public enum Ternary {

	TRUE, FALSE, UNKNOWN

}