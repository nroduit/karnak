/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.editor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.model.Series;
import org.karnak.backend.model.SopInstance;
import org.karnak.backend.model.Study;
import org.weasis.dicom.param.AttributeEditorContext;
import org.weasis.dicom.param.DicomNode;
import org.weasis.dicom.param.DicomProgress;

class StreamRegistryEditorTest {

	// Create streamRegistryEditor
	private StreamRegistryEditor streamRegistryEditor;

	@BeforeEach
	public void setUp() {
		streamRegistryEditor = new StreamRegistryEditor();
	}

	@Test
	void should_add_study_serie_sop() {
		// Init data
		Attributes dcm = new Attributes();
		dcm.setString(Tag.StudyInstanceUID, VR.SH, "studyInstanceUID");
		dcm.setString(Tag.SeriesInstanceUID, VR.SH, "seriesInstanceUID");
		dcm.setString(Tag.SOPInstanceUID, VR.SH, "sopInstanceUID");

		// AttributeEditorContext
		DicomNode source = new DicomNode("source");
		DicomNode destination = new DicomNode("destination");
		AttributeEditorContext attributeEditorContext = new AttributeEditorContext("tsuid", source, destination);

		// Call service
		streamRegistryEditor.setEnable(true);
		streamRegistryEditor.apply(dcm, attributeEditorContext);

		// Test results
		assertNotNull(streamRegistryEditor.getStudy("studyInstanceUID"));
		assertNotNull(streamRegistryEditor.getStudy("studyInstanceUID").getSeries("seriesInstanceUID"));
		assertNotNull(streamRegistryEditor.getStudy("studyInstanceUID")
			.getSeries("seriesInstanceUID")
			.getSopInstance("sopInstanceUID"));
	}

	@Test
	void should_remove_study() {

		// Call service
		streamRegistryEditor.addStudy(new Study("studyInstanceUID", "patientId"));

		// Test study added
		assertNotNull(streamRegistryEditor.getStudy("studyInstanceUID"));

		// Call service
		streamRegistryEditor.removeStudy("studyInstanceUID");

		// Test result
		assertNull(streamRegistryEditor.getStudy("studyInstanceUID"));
	}

	@Test
	void should_update_sopInstance() {
		// Init data
		DicomProgress dicomProgress = new DicomProgress();
		Attributes attributes = new Attributes();
		attributes.setString(Tag.AffectedSOPInstanceUID, VR.SH, "affectedSOPInstanceUID");
		dicomProgress.setAttributes(attributes);
		Study study = new Study("studyInstanceUID", "patientId");
		Series serie = new Series("seriesInstantUID");
		SopInstance sopInstance = new SopInstance("affectedSOPInstanceUID");
		serie.addSopInstance(sopInstance);
		study.addSeries(serie);

		// Call service
		streamRegistryEditor.setEnable(true);
		streamRegistryEditor.addStudy(study);
		streamRegistryEditor.update(dicomProgress);

		// Test result
		assertTrue(sopInstance.isSent());
	}

}
