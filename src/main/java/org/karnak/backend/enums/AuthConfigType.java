/*
<<<<<<<< HEAD:src/main/java/org/karnak/backend/enums/AuthConfigType.java
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
========
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
>>>>>>>> master:src/main/java/org/karnak/frontend/forwardnode/edit/destination/component/ProfileLabel.java
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.enums;

import lombok.Getter;

<<<<<<<< HEAD:src/main/java/org/karnak/backend/enums/AuthConfigType.java
@Getter
public enum AuthConfigType {

	OAUTH2("OAuth 2.0");

	final String code;

	AuthConfigType(final String code) {
		this.code = code;
========
public class ProfileLabel extends Div {

	public void setShowValue(String text) {
		removeAll();
		add(text);
>>>>>>>> master:src/main/java/org/karnak/frontend/forwardnode/edit/destination/component/ProfileLabel.java
	}

}
