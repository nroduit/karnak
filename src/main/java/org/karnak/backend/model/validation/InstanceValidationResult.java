/*
<<<<<<<< HEAD:src/main/java/org/karnak/backend/model/validation/InstanceValidationResult.java
 * Copyright (c) 2026 Karnak Team and other contributors.
========
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
>>>>>>>> master:src/main/java/org/karnak/backend/enums/AuthConfigType.java
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
<<<<<<<< HEAD:src/main/java/org/karnak/backend/model/validation/InstanceValidationResult.java
package org.karnak.backend.model.validation;

import java.util.List;

/**
 * Result of validating a single DICOM instance against its IOD.
 */
public record InstanceValidationResult(String sopClassUid, String sopInstanceUid, List<ConformanceFinding> findings) {

========
package org.karnak.backend.enums;

public enum AuthConfigType {

	OAUTH2("OAuth 2.0");

	final String code;

	AuthConfigType(final String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

>>>>>>>> master:src/main/java/org/karnak/backend/enums/AuthConfigType.java
}
