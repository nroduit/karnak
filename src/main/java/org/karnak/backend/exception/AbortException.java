/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.exception;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.weasis.dicom.param.AttributeEditorContext.Abort;

@Getter
public final class AbortException extends IllegalStateException {

	private final Abort abort;

	public AbortException(Abort abort, String message) {
		super(message);
		this.abort = abort;
	}

	public AbortException(Abort abort, String message, Exception e) {
		super(message, e);
		this.abort = abort;
	}

	@Override
	public @Nullable String toString() {
		return getMessage();
	}

}
