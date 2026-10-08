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

import java.util.List;

/**
 * Result of the burned-in identity OCR check for one forwarded instance.
 *
 * @param failed {@code true} when the de-identification image API could not be queried
 * @param detectedTags names of the identifying DICOM tags whose value was found burned
 * into the image (empty when none or on failure)
 */
public record ImageIdentityCheckOutcome(boolean failed, List<String> detectedTags) {

	public static ImageIdentityCheckOutcome detected(List<String> detectedTags) {
		return new ImageIdentityCheckOutcome(false, detectedTags);
	}

	public static ImageIdentityCheckOutcome failure() {
		return new ImageIdentityCheckOutcome(true, List.of());
	}

}
