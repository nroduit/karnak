/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.util;

import java.util.ArrayList;
import java.util.List;
import org.karnak.backend.model.dicom.DicomNodeList;
import org.karnak.frontend.dicom.Util;

public class DicomNodeUtil {

	public static List<DicomNodeList> getAllDicomNodeTypesDefinedLocally() {
		List<DicomNodeList> dicomNodeTypes = new ArrayList<>();

		dicomNodeTypes
			.add(Util.readnodes(DicomNodeUtil.class.getResource("/config/workstations-nodes.csv"), "Workstations"));
		dicomNodeTypes
			.add(Util.readnodes(DicomNodeUtil.class.getResource("/config/pacs-nodes-web.csv"), "PACS Public WEB"));

		return dicomNodeTypes;
	}

	public static DicomNodeList getAllWorkListNodesDefinedLocally() {
		return Util.readnodes(DicomNodeUtil.class.getResource("/config/worklist-nodes.csv"), "Worklists");
	}

}
