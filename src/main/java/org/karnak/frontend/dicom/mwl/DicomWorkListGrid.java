/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.dicom.mwl;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H6;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import java.util.List;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Keyword;
import org.dcm4che3.data.Tag;
import org.weasis.core.util.StringUtil;
import org.weasis.core.util.annotations.Generated;
import org.weasis.dicom.op.CFind;
import org.weasis.dicom.param.DicomParam;
import org.weasis.dicom.tool.ModalityWorklist;

@Generated()
public class DicomWorkListGrid extends Grid<Attributes> {

	List<DicomParam> params = List.of(CFind.PatientName, CFind.PatientID, CFind.PatientBirthDate, CFind.PatientSex,
			CFind.AccessionNumber, ModalityWorklist.ScheduledProcedureStepDescription, ModalityWorklist.Modality,
			ModalityWorklist.ScheduledStationName);

	private final transient DicomPaneLogic paneLogic = new DicomPaneLogic();

	public DicomWorkListGrid() {
		init();
		buildColumns();
	}

	private void init() {
		// Selecting a row expands an inline details panel rather than opening a modal.
		setSelectionMode(SelectionMode.NONE);
		setDetailsVisibleOnClick(true);
		setItemDetailsRenderer(buildItemDetailsRenderer());
	}

	private ComponentRenderer<Component, Attributes> buildItemDetailsRenderer() {
		return new ComponentRenderer<>(attributes -> {
			VerticalLayout layout = new VerticalLayout();
			layout.setWidthFull();
			layout.setMargin(false);
			layout.setPadding(true);
			layout.setSpacing(false);
			layout.getStyle().set("font-size", "var(--lumo-font-size-s)");

			H6 title = new H6("Details");
			title.getStyle().set("margin-top", "0px");
			layout.add(title);

			UnorderedList details = new UnorderedList();
			addDetail(details, "Accession Number", getText(attributes, Tag.AccessionNumber));
			addDetail(details, "Admission ID", getText(attributes, Tag.AdmissionID));
			addDetail(details, "Requested Procedure", getText(attributes, Tag.RequestedProcedureDescription));
			addDetail(details, "Referring Physician", getText(attributes, Tag.ReferringPhysicianName));
			addDetail(details, "Study Instance UID", getText(attributes, Tag.StudyInstanceUID));
			layout.add(details);

			layout.add(buildActions(attributes));
			return layout;
		});
	}

	private HorizontalLayout buildActions(Attributes attributes) {
		Button viewBtn = new Button("View DICOM Details", event -> new DicomPane(attributes).open());
		viewBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);

		Anchor textDownload = downloadAnchor("Download as Text",
				DownloadHandler
					.fromInputStream(event -> new DownloadResponse(paneLogic.getWorklistItemInputStreamText(attributes),
							"worklistItem.txt", "text/plain", -1)));

		Anchor dicomDownload = downloadAnchor("Download as DICOM",
				DownloadHandler.fromInputStream(
						event -> new DownloadResponse(paneLogic.getWorklistItemInputStreamInDicom(attributes),
								"worklistItem.dcm", "application/dicom", -1)));

		HorizontalLayout actions = new HorizontalLayout(viewBtn, textDownload, dicomDownload);
		actions.setMargin(false);
		actions.setPadding(false);
		actions.setSpacing(true);
		actions.setAlignItems(Alignment.CENTER);
		return actions;
	}

	private static void addDetail(UnorderedList list, String label, String value) {
		list.add(new ListItem(new Div(label + ": " + value)));
	}

	private static Anchor downloadAnchor(String text, DownloadHandler handler) {
		Button button = new Button(text);
		button.addThemeVariants(ButtonVariant.LUMO_SMALL);

		Anchor anchor = new Anchor();
		anchor.setHref(handler);
		anchor.getElement().setAttribute("download", true);
		anchor.add(button);
		return anchor;
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
				for (int i : pSeq) {
					Attributes pn = parent.getNestedDataset(i);
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
