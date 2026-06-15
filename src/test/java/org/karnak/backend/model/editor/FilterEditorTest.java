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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import java.util.Set;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.SOPClassUIDEntity;
import org.weasis.dicom.param.AttributeEditorContext;
import org.weasis.dicom.param.AttributeEditorContext.Abort;
import org.weasis.dicom.param.DicomNode;

class FilterEditorTest {

	@Test
	void when_no_class_uid_found_should_modify_context() {
		// Init data
		// SopClassUIDEntities
		Set<SOPClassUIDEntity> sopClassUIDEntities = new HashSet<>();
		SOPClassUIDEntity sopClassUIDEntityFirst = new SOPClassUIDEntity();
		SOPClassUIDEntity sopClassUIDEntitySecond = new SOPClassUIDEntity();
		sopClassUIDEntityFirst.setUid("TEST FIRST");
		sopClassUIDEntitySecond.setUid("TEST SECOND");
		sopClassUIDEntities.add(sopClassUIDEntityFirst);
		sopClassUIDEntities.add(sopClassUIDEntitySecond);

		// Attributes
		Attributes attributes = new Attributes();
		attributes.setString(Tag.SOPClassUID, VR.SH, "TEST NOT FOUND");

		// AttributeEditorContext
		DicomNode source = new DicomNode("source");
		DicomNode destination = new DicomNode("destination");
		AttributeEditorContext attributeEditorContext = new AttributeEditorContext("tsuid", source, destination);

		// Create filterEditor
		FilterEditor filterEditor = new FilterEditor(sopClassUIDEntities);

		// Call service
		filterEditor.apply(attributes, attributeEditorContext);

		// Test results
		assertEquals(Abort.FILE_EXCEPTION, attributeEditorContext.getAbort());
	}

	@Test
	void when_class_uid_found_should_not_modify_context() {
		// Init data
		// SopClassUIDEntities
		Set<SOPClassUIDEntity> sopClassUIDEntities = new HashSet<>();
		SOPClassUIDEntity sopClassUIDEntityFirst = new SOPClassUIDEntity();
		SOPClassUIDEntity sopClassUIDEntitySecond = new SOPClassUIDEntity();
		sopClassUIDEntityFirst.setUid("TEST FIRST");
		sopClassUIDEntitySecond.setUid("TEST SECOND");
		sopClassUIDEntities.add(sopClassUIDEntityFirst);
		sopClassUIDEntities.add(sopClassUIDEntitySecond);

		// Attributes
		Attributes attributes = new Attributes();
		attributes.setString(Tag.SOPClassUID, VR.SH, "TEST FIRST");

		// AttributeEditorContext
		DicomNode source = new DicomNode("source");
		DicomNode destination = new DicomNode("destination");
		AttributeEditorContext attributeEditorContext = new AttributeEditorContext("tsuid", source, destination);

		// Create filterEditor
		FilterEditor filterEditor = new FilterEditor(sopClassUIDEntities);

		// Call service
		filterEditor.apply(attributes, attributeEditorContext);

		// Test results
		assertNotEquals(Abort.FILE_EXCEPTION, attributeEditorContext.getAbort());
	}

}
