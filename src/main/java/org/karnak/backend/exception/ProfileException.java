/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.exception;

import java.io.Serial;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProfileException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = -8468625700663388828L;

	public ProfileException(String message) {
		super(message);
		log.error(message);
	}

}
