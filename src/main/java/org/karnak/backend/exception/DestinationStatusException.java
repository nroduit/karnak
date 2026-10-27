/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.exception;

import java.io.IOException;
import lombok.Getter;

/**
 * A destination did not store the forwarded object. Carries the DIMSE status to return to
 * the source, so the sender learns about the refusal instead of receiving Success and can
 * retry when the status is transient (A700, out of resources).
 */
@Getter
public final class DestinationStatusException extends IOException {

	private final int status;

	public DestinationStatusException(int status, String message) {
		super(message);
		this.status = status;
	}

	public DestinationStatusException(int status, String message, Throwable cause) {
		super(message, cause);
		this.status = status;
	}

}
