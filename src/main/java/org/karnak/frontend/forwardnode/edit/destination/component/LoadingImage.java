/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.forwardnode.edit.destination.component;

import com.vaadin.flow.component.html.Image;

public class LoadingImage extends Image {

	private static final String LOADING_IMAGE_PATH = "img/loading.gif";

	public LoadingImage(String alt, String maxSize) {
		setSrc(LOADING_IMAGE_PATH);
		setAlt(alt);
		setMaxHeight(maxSize);
		setMaxWidth(maxSize);
	}

}
