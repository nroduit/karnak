/*
 * Copyright (c) 2009-2026 Karnak Team and other contributors.
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
import lombok.Setter;
import org.dcm4che3.data.UID;
import org.dcm4che3.img.DicomImageReader;
import org.dcm4che3.img.DicomOutputData;
import org.dcm4che3.img.util.DicomUtils;
import org.jspecify.annotations.Nullable;
import org.weasis.core.util.annotations.Generated;
import org.weasis.dicom.param.AttributeEditor;
import org.weasis.dicom.param.DicomState;

@Getter
@Generated()
public abstract class ForwardDestination {

	protected final @Nullable List<AttributeEditor> dicomEditors;

	private final @Nullable Long id;

	@Setter
	private boolean transcodeOnlyUncompressed = true;

	/** When true, a DICOM conformance report is collected and emailed per study sent. */
	@Setter
	private boolean buildConformanceReport;

	/**
	 * When true, the conformance report also checks value-content conformity (VR rules).
	 */
	@Setter
	private boolean checkValueConformity;

	/**
	 * When true, the conformance checks recurse through every sequence level (SR content
	 * tree, enhanced multiframe functional groups, …) instead of only the first one.
	 */
	@Setter
	private boolean deepSequenceValidation;

	/**
	 * Whether de-identification is enabled; drives PHI redaction in the conformance
	 * report.
	 */
	@Setter
	private boolean deidentified;

	/**
	 * When true, this is a virtual "report-only" destination: the dataset is processed
	 * (editors applied) and a conformance report is produced, but it is never forwarded
	 * to a real node — the DICOM is discarded (devnull).
	 */
	@Setter
	private boolean virtual;

	private String outputTransferSyntax = "";

	protected ForwardDestination(@Nullable Long id, @Nullable List<AttributeEditor> dicomEditors) {
		this.dicomEditors = dicomEditors;
		this.id = id;
	}

	public abstract ForwardDicomNode getForwardDicomNode();

	public abstract void stop();

	public abstract DicomState getState();

	public void setOutputTransferSyntax(@Nullable String outputTransferSyntax) {
		this.outputTransferSyntax = outputTransferSyntax != null ? outputTransferSyntax : "";
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
