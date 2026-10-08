/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model;

import org.weasis.dicom.param.DicomNode;

public record SourceNode(String forwardAETitle, DicomNode sourceNode) {

	@Override
	public int hashCode() {
		return 31 + forwardAETitle.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof SourceNode other && forwardAETitle.equals(other.forwardAETitle);
	}

}
