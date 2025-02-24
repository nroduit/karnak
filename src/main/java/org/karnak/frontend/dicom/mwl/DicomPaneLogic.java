/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.dicom.mwl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.UID;
import org.dcm4che3.io.DicomOutputStream;

@Slf4j
public class DicomPaneLogic {

	// PANE
	private final DicomPane pane;

	public DicomPaneLogic(DicomPane pane) {
		this.pane = pane;
	}

	public InputStream getWorklistItemInputStreamInDicom(Attributes attributes) {
		InputStream inputStream = null;

		if (attributes != null) {
			try (ByteArrayOutputStream tmp = new ByteArrayOutputStream();
					DicomOutputStream out = new DicomOutputStream(tmp, UID.ImplicitVRLittleEndian)) {
				out.writeDataset(null, attributes);
				inputStream = new ByteArrayInputStream(tmp.toByteArray());
			}
			catch (IOException e) {
				log.error("Cannot write dicom file: {}", e.getMessage()); // $NON-NLS-1$
			}
		}

		return inputStream;
	}

	public InputStream getWorklistItemInputStreamText(Attributes attributes) {
		InputStream inputStream = null;

		if (attributes != null) {
			inputStream = new ByteArrayInputStream(attributes.toString(1500, 300).getBytes());
		}

		return inputStream;
	}

}
