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

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * Encoded pixel data and original identifying values captured on the forwarding thread so
 * the conformance report pipeline can later ask the de-identification image API which
 * identifying tag values are burned into the image.
 *
 * @param imageBytes the encoded pixel data of the received image
 * @param sensitiveData the original identifying tag values to look for (name to value)
 * @param transferSyntaxUid the transfer syntax the {@code imageBytes} are encoded with
 */
public record ImageIdentityCheckInput(byte[] imageBytes, Map<String, String> sensitiveData, String transferSyntaxUid) {

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ImageIdentityCheckInput(byte[] bytes, Map<String, String> data, String syntaxUid))) {
			return false;
		}
		return Arrays.equals(imageBytes, bytes)
			&& Objects.equals(sensitiveData, data)
			&& Objects.equals(transferSyntaxUid, syntaxUid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			Arrays.hashCode(imageBytes),
			sensitiveData,
			transferSyntaxUid
		);
	}

	@Override
	public String toString() {
		return "ImageIdentityCheckInput{" +
				"imageBytes=" + (imageBytes == null ? null : imageBytes.length + " bytes") +
				", sensitiveData=" + (sensitiveData == null ? null : sensitiveData.keySet()) +
				", transferSyntaxUid='" + transferSyntaxUid + '\'' +
				'}';
	}
}
