/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.forwardnode.edit.destination.component;

import com.vaadin.flow.component.html.Image;
import org.weasis.core.util.annotations.Generated;

@Generated()
public class LoadingImage extends Image {

	// Served statically from src/main/resources/META-INF/resources/img/ (relative to the
	// app base href). A plain static src avoids registering a per-row DownloadHandler
	// stream inside the grid component column, which broke client-side row rendering.
	private static final String LOADING_IMAGE_SRC = "img/loading.gif";

	public LoadingImage(String alt, String maxSize) {
		super(LOADING_IMAGE_SRC, alt);
		setMaxHeight(maxSize);
		setMaxWidth(maxSize);
	}

}