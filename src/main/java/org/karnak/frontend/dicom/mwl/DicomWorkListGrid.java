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

import com.vaadin.flow.component.grid.Grid;
import java.io.Serial;
import java.util.List;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Keyword;
import org.weasis.core.util.StringUtil;
import org.weasis.dicom.op.CFind;
import org.weasis.dicom.param.DicomParam;
import org.weasis.dicom.tool.ModalityWorklist;

public class DicomWorkListGrid extends Grid<Attributes> {

	@Serial
	private static final long serialVersionUID = 1L;

	List<DicomParam> params = List.of(CFind.PatientName, CFind.PatientID, CFind.PatientBirthDate, CFind.PatientSex,
			CFind.AccessionNumber, ModalityWorklist.ScheduledProcedureStepDescription, ModalityWorklist.Modality,
			ModalityWorklist.ScheduledStationName);

	public DicomWorkListGrid() {
		init();
		buildColumns();
	}

	private void init() {
		setSelectionMode(SelectionMode.SINGLE);
	}

	private void buildColumns() {
		for (DicomParam p : params) {
			addColumn(p);
		}
	}

	private void addColumn(DicomParam p) {
		int tag = p.getTag();
		int[] pSeq = p.getParentSeqTags();
		if (pSeq == null || pSeq.length == 0) {
			addColumn(a -> getText(a, tag)).setHeader(Keyword.valueOf(tag))
				.setSortable(true)
				.setKey(String.valueOf(tag));
		}
		else {
			addColumn(a -> {
				Attributes parent = a;
				for (int k = 0; k < pSeq.length; k++) {
					Attributes pn = parent.getNestedDataset(pSeq[k]);
					if (pn == null) {
						break;
					}
					parent = pn;
				}
				return getText(parent, tag);
			}).setHeader(Keyword.valueOf(tag)).setSortable(true).setKey(String.valueOf(tag));
		}
	}

	private String getText(Attributes attributes, int tag) {
		return attributes.getString(tag, StringUtil.EMPTY_STRING);
	}

}
