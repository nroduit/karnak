/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.dicom;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WadoNode {

	private final String name;

	private final URL url;

	private final List<String> tagEntities = new ArrayList<>(2);

	public WadoNode(String name, URL url) {
		this.name = Objects.requireNonNull(name);
		this.url = Objects.requireNonNull(url);
	}

	@Override
	public String toString() {
		return name;
	}

	public String getName() {
		return name;
	}

	public URL getUrl() {
		return url;
	}

	public List<String> getTags() {
		return tagEntities;
	}

}
