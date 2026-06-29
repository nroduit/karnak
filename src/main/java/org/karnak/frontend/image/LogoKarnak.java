/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.image;

import com.vaadin.flow.component.html.Image;
import org.weasis.core.util.annotations.Generated;

@Generated()
public class LogoKarnak extends Image {

	// Served statically from src/main/resources/META-INF/resources/img/ (relative to the
	// app base href), like LoadingImage. A plain static src avoids the overhead of a
	// per-instance DownloadHandler stream for a fixed asset.
	private static final String LOGO_SRC = "img/karnak.png";

	public LogoKarnak(String alt, String maxSize) {
		super(LOGO_SRC, alt);
		setMaxHeight(maxSize);
		setMaxWidth(maxSize);
	}

}
