/*
 * Copyright (c) 2025-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.h2;

import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Profile;
import org.weasis.core.util.annotations.Generated;

/**
 * H2 stand-ins for the PostgreSQL {@code pgcrypto} functions, used only by the
 * {@code portable} profile (embedded H2 instead of Postgres).
 *
 * <p>
 * These methods have <b>no direct Java callers</b>: they are registered as SQL functions
 * via {@code CREATE ALIAS} in {@code db/changelog/changes/db.changelog-1.4.xml} and
 * invoked by H2 when it evaluates the {@code @ColumnTransformer} expressions on encrypted
 * columns (see {@link org.karnak.backend.data.entity.AuthConfigEntity}). On Postgres the
 * real {@code pgcrypto} extension is used instead.
 *
 * <p>
 * The portable build is a single-user embedded deployment, so these perform no real
 * encryption — the value is passed through unchanged and the key argument is ignored.
 */
@Profile("portable")
@Generated
public final class PgCryptoNoop {

	private PgCryptoNoop() {
	}

	public static @Nullable String pgpSymDecrypt(byte[] data, String key) {
		return data == null ? null : new String(data, StandardCharsets.UTF_8);
	}

	public static byte @Nullable [] pgpSymEncrypt(String data, String key) {
		return data == null ? null : data.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Aliased to SQL {@code CURRENT_SETTING}; the key is irrelevant for the no-op cipher.
	 */
	public static String currentSetting(String name) {
		return "";
	}

}
