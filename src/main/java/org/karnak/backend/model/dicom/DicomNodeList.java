/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.dicom;

import java.util.ArrayList;
import java.util.Collection;
import lombok.Getter;

@Getter
public class DicomNodeList extends ArrayList<ConfigNode> {

	private final String name;

	public DicomNodeList(String name) {
		this.name = name;
	}

	public DicomNodeList(String name, Collection<? extends ConfigNode> c) {
		super(c);
		this.name = name;
	}

	public DicomNodeList(String name, int initialCapacity) {
		super(initialCapacity);
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

}
