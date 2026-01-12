/*
 * Copyright (c) 2025 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.h2;

import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Profile;

@Profile("portable")
public final class PgCryptoNoop {

	private PgCryptoNoop() {
	}

	// H2 will call these via CREATE ALIAS
	public static String pgpSymDecrypt(byte[] data, String key) {
		return data == null ? null : new String(data, StandardCharsets.UTF_8);
	}

	public static byte[] pgpSymEncrypt(String data, String key) {
		return data == null ? null : data.getBytes(StandardCharsets.UTF_8);
	}

	public static String currentSetting(String name) {
		// Not used by H2; return any constant
		return "";
	}

}
