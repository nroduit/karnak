/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
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

	/**
	 * Returns the tag value, walking up the parent objects until it is found or the root
	 * is reached.
	 */
	public static String getValue(Attributes dcm, int tag) {
		String tagValue = DicomUtils.getStringFromDicomElement(dcm, tag);
		Attributes dcmParent = dcm.getParent();
		if (tagValue == null && dcmParent != null) {
			return getValue(dcmParent, tag);
		}
		return tagValue;
	}

	/**
	 * Builds the tag path (as expected by StandardDICOM) by walking up to the root
	 * object.
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
