/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.standard;

import org.jspecify.annotations.Nullable;

/**
 * A Functional Group Macro used by an enhanced multi-frame IOD (e.g. PS3.3 Table A.55-2
 * for Breast Tomosynthesis), with its usage in that IOD.
 *
 * @param id the innolitics macro identifier (e.g. {@code frame-content})
 * @param usage M, C or U
 * @param conditionalStatement the free-text condition of a C macro, else null
 */
public record FunctionalGroupMacro(String id, String usage, @Nullable String conditionalStatement) {

	public static final String CONDITIONAL = "C";

}
