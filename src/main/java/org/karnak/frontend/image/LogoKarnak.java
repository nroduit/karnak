/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.image;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.StreamResource;

public class LogoKarnak extends Image {

	private static final String LOGO_PATH = "karnak.png";

	public LogoKarnak(String alt, String maxSize) {
		super(new StreamResource(LOGO_PATH,() -> LogoKarnak.class.getResourceAsStream("/META-INF/resources/img/" + LOGO_PATH)), alt);
		setMaxHeight(maxSize);
		setMaxWidth(maxSize);
	}
}
