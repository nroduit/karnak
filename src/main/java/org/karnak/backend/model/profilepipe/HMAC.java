/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profilepipe;

import lombok.Getter;
import org.jspecify.annotations.NullUnmarked;
import org.weasis.dicom.util.Hmac;

/**
 * De-identification HMAC adding a {@link HashContext} on top of the weasis {@link Hmac}.
 */
@NullUnmarked
public class HMAC extends Hmac {

	@Getter
	private final HashContext hashContext;

	public HMAC(byte[] hmacKey) {
		super(hmacKey);
		this.hashContext = null;
	}

	public HMAC(HashContext hashContext) {
		super(hashContext.secret());
		this.hashContext = hashContext;
	}

}
