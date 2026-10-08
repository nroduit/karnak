/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.dicom;

import java.util.List;
import lombok.Getter;
import org.jspecify.annotations.NullUnmarked;
import org.weasis.core.util.annotations.Generated;
import org.weasis.dicom.param.AttributeEditor;
import org.weasis.dicom.param.DicomProgress;
import org.weasis.dicom.param.DicomState;

/**
 * A "devnull" forward destination used by virtual (report-only) destinations. It holds no
 * network resources: the incoming dataset is processed through the destination's editors
 * so the conformance report reflects what would have been sent, then the data is
 * discarded instead of being forwarded to a real node.
 */
@Generated()
@NullUnmarked
public class NullForwardDestination extends ForwardDestination {

	private final ForwardDicomNode callingNode;

	@Getter
	private final DicomState state;

	public NullForwardDestination(Long id, ForwardDicomNode fwdNode, List<AttributeEditor> editors) {
		super(id, editors);
		this.callingNode = fwdNode;
		this.state = new DicomState(new DicomProgress());
		setVirtual(true);
	}

	@Override
	public ForwardDicomNode getForwardDicomNode() {
		return callingNode;
	}

	@Override
	public void stop() {
		// No network resource to release.
	}

	@Override
	public String toString() {
		return "devnull (virtual report-only destination)";
	}

}