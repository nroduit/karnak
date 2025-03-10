/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.util;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.img.util.DicomUtils;
import org.dcm4che3.util.TagUtils;

public class MetadataDICOMObject {

	private MetadataDICOMObject() {
	}

	/*
	 * Search a tagValue in the current DicomObject and his parent Will loop in the parent
	 * of the DicomObject until the last parent or the tagValue
	 */
	public static String getValue(Attributes dcm, int tag) {
		return getValueRec(dcm, tag);
	}

	private static String getValueRec(Attributes dcm, int tag) {
		String tagValue = DicomUtils.getStringFromDicomElement(dcm, tag);
		Attributes dcmParent = dcm.getParent();
		if (dcmParent != null && tagValue == null) {
			return getValueRec(dcmParent, tag);
		}
		return tagValue;
	}

	/*
	 * Generate the tag Path as needed in the class StandardDICOM Will loop in the parent
	 * of the DicomObject until the last parent
	 */
	public static String getTagPath(Attributes dcm, int currentTag) {
		return getTagPathRec(dcm, TagUtils.toString(currentTag));
	}

	private static String getTagPathRec(Attributes dcm, String tagPath) {
		Attributes dcmParent = dcm.getParent();
		if (dcmParent != null) {
			return getTagPathRec(dcmParent,
					String.format("%s:%s", TagUtils.toHexString(dcm.getParentSequenceTag()), tagPath));
		}
		return tagPath;
	}

}
