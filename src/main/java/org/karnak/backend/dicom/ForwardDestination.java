/*
 * Copyright (c) 2009-2019 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.dicom;

import java.util.List;
import org.dcm4che3.data.UID;
import org.dcm4che3.img.DicomImageReader;
import org.dcm4che3.img.DicomOutputData;
import org.dcm4che3.img.util.DicomUtils;
import org.weasis.dicom.param.AttributeEditor;
import org.weasis.dicom.param.DicomState;

public abstract class ForwardDestination {

	protected final List<AttributeEditor> dicomEditors;

	private final Long id;

	private boolean transcodeOnlyUncompressed = true;

	private String outputTransferSyntax = "";

	protected ForwardDestination(Long id, List<AttributeEditor> dicomEditors) {
		this.dicomEditors = dicomEditors;
		this.id = id;
	}

	public List<AttributeEditor> getDicomEditors() {
		return dicomEditors;
	}

	public Long getId() {
		return id;
	}

	public abstract ForwardDicomNode getForwardDicomNode();

	public abstract void stop();

	public abstract DicomState getState();

	public String getOutputTransferSyntax() {
		return outputTransferSyntax;
	}

	public void setOutputTransferSyntax(String outputTransferSyntax) {
		this.outputTransferSyntax = outputTransferSyntax != null ? outputTransferSyntax : "";
	}

	public boolean isTranscodeOnlyUncompressed() {
		return transcodeOnlyUncompressed;
	}

	public void setTranscodeOnlyUncompressed(boolean transcodeOnlyUncompressed) {
		this.transcodeOnlyUncompressed = transcodeOnlyUncompressed;
	}

	public String getOutputTransferSyntax(String originalTsuid) {
		if (transcodeOnlyUncompressed && !DicomUtils.isNative(originalTsuid)
				&& !UID.RLELossless.equals(originalTsuid)) {
			return originalTsuid;
		}
		if (DicomOutputData.isSupportedSyntax(outputTransferSyntax)
				&& DicomImageReader.isSupportedSyntax(originalTsuid)) {
			return outputTransferSyntax;
		}
		if (UID.RLELossless.equals(originalTsuid) || UID.ImplicitVRLittleEndian.equals(originalTsuid)
				|| UID.ExplicitVRBigEndian.equals(originalTsuid)) {
			return UID.ExplicitVRLittleEndian;
		}
		return originalTsuid;
	}

}
