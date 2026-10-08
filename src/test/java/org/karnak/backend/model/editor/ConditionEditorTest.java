/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.weasis.dicom.param.AttributeEditorContext;
import org.weasis.dicom.param.AttributeEditorContext.Abort;
import org.weasis.dicom.param.DicomNode;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ConditionEditorTest {

	private static AttributeEditorContext newContext() {
		return new AttributeEditorContext("tsuid", new DicomNode("source"), new DicomNode("destination"));
	}

	@Test
	void leaves_the_context_untouched_when_the_condition_is_met() {
		Attributes dcm = new Attributes();
		dcm.setString(Tag.PatientName, VR.PN, "Doe^John");
		AttributeEditorContext context = newContext();
		ConditionEditor editor = new ConditionEditor("tagIsPresent('00100010')");

		editor.apply(dcm, context);

		assertEquals(Abort.NONE, context.getAbort());
		assertNull(context.getAbortMessage());
	}

	@Test
	void aborts_the_instance_when_the_condition_is_not_met() {
		Attributes dcm = new Attributes();
		AttributeEditorContext context = newContext();
		ConditionEditor editor = new ConditionEditor("tagIsPresent('00100010')");

		editor.apply(dcm, context);

		assertEquals(Abort.FILE_EXCEPTION, context.getAbort());
		assertNotNull(context.getAbortMessage());
	}

}