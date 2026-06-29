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

import org.jspecify.annotations.Nullable;

/**
 * A curated machine-evaluable form of a DICOM Type 1C/2C "Required if …" condition, keyed
 * in the curated rules file by {@code moduleId/tagPath}. When {@link #getRequiredWhen()}
 * evaluates to {@link Ternary#TRUE} for an instance, the conditional attribute is treated
 * as mandatory (Type 1 for 1C, Type 2 for 2C); otherwise the standard's "checked only
 * when present" behavior is kept. The {@code _condition} field of each entry is the
 * original free-text sentence kept for provenance and ignored at runtime.
 */
public class ConditionalRequirement {

	private @Nullable Condition requiredWhen;

	public @Nullable Condition getRequiredWhen() {
		return requiredWhen;
	}

}