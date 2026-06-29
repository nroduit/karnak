/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.expression;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Setter
@Getter
public class ExpressionError {

	private boolean isValid;

	private @Nullable String msg;

	public ExpressionError(boolean isValid, @Nullable String msg) {
		this.isValid = isValid;
		this.msg = msg;
	}

}
