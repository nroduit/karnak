/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

@Slf4j
public class ProfileException extends RuntimeException {

	public ProfileException(@Nullable String message) {
		super(message);
		log.error(message);
	}

}
