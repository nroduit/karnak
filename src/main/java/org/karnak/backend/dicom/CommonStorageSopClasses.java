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
import org.dcm4che3.data.UID;

/**
 * A curated set of the most common DICOM Storage SOP Classes.
 *
 * <p>
 * It is used to <em>pre-negotiate</em> presentation contexts when an outgoing DICOM
 * association is first opened, so that the usual mix of objects in a study does not force
 * the association to be closed and re-opened the first time each new SOP Class appears
 * (DICOM forbids adding a presentation context to an established association, so a new
 * SOP Class otherwise triggers a renegotiation that blocks every other transfer to that
 * destination).
 *
 * <p>
 * The list is intentionally kept under the DICOM limit of 128 presentation contexts once
 * paired with the standard uncompressed transfer syntaxes (Explicit/Implicit VR Little
 * Endian). Objects carrying a SOP Class not listed here, or a compressed transfer syntax
 * that was not pre-negotiated, still negotiate on first occurrence — this only removes
 * the common cold-start churn.
 */
public final class CommonStorageSopClasses {

	/**
	 * Common Storage SOP Class UIDs, ordered roughly by how frequently they are
	 * forwarded.
	 */
	public static final List<String> UIDS = List.of(UID.CTImageStorage, UID.EnhancedCTImageStorage, UID.MRImageStorage,
			UID.EnhancedMRImageStorage, UID.ComputedRadiographyImageStorage, UID.DigitalXRayImageStorageForPresentation,
			UID.DigitalXRayImageStorageForProcessing, UID.DigitalMammographyXRayImageStorageForPresentation,
			UID.DigitalMammographyXRayImageStorageForProcessing, UID.XRayAngiographicImageStorage,
			UID.EnhancedXAImageStorage, UID.XRayRadiofluoroscopicImageStorage, UID.UltrasoundImageStorage,
			UID.UltrasoundMultiFrameImageStorage, UID.SecondaryCaptureImageStorage,
			UID.MultiFrameGrayscaleByteSecondaryCaptureImageStorage,
			UID.MultiFrameGrayscaleWordSecondaryCaptureImageStorage,
			UID.MultiFrameTrueColorSecondaryCaptureImageStorage, UID.NuclearMedicineImageStorage,
			UID.PositronEmissionTomographyImageStorage, UID.EnhancedPETImageStorage,
			UID.GrayscaleSoftcopyPresentationStateStorage, UID.BasicTextSRStorage, UID.EnhancedSRStorage,
			UID.ComprehensiveSRStorage, UID.KeyObjectSelectionDocumentStorage, UID.EncapsulatedPDFStorage,
			UID.SegmentationStorage, UID.RTImageStorage, UID.RTDoseStorage, UID.RTStructureSetStorage,
			UID.RTPlanStorage, UID.VLPhotographicImageStorage, UID.OphthalmicPhotography8BitImageStorage);

	private CommonStorageSopClasses() {
	}

}