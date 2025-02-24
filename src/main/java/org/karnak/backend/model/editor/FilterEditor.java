/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.editor;

import java.util.Set;
import java.util.function.Predicate;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.karnak.backend.data.entity.SOPClassUIDEntity;
import org.weasis.dicom.param.AttributeEditor;
import org.weasis.dicom.param.AttributeEditorContext;
import org.weasis.dicom.param.AttributeEditorContext.Abort;

public class FilterEditor implements AttributeEditor {

	private final Set<SOPClassUIDEntity> sopClassUIDEntitySet;

	public FilterEditor(Set<SOPClassUIDEntity> sopClassUIDEntitySet) {
		this.sopClassUIDEntitySet = sopClassUIDEntitySet;
	}

	@Override
	public void apply(Attributes dcm, AttributeEditorContext context) {
		String classUID = dcm.getString(Tag.SOPClassUID);
		Predicate<SOPClassUIDEntity> sopClassUIDPredicate = sopClassUID -> sopClassUID.getUid().equals(classUID);
		if (sopClassUIDEntitySet.stream().noneMatch(sopClassUIDPredicate)) {
			context.setAbort(Abort.FILE_EXCEPTION);
			context.setAbortMessage(dcm.getString(Tag.SOPInstanceUID) + " is blocked because " + classUID
					+ " is not in the SOPClassUID filter");
		}
	}

}
