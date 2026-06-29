/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.dicom;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.List;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.model.dicom.result.DicomCapabilitiesResult;

/**
 * Reusable presentation of a {@link DicomCapabilitiesResult}: a one-line summary (or the
 * failure reason) above the {@link DicomCapabilitiesGrid}. Used both inline in the DICOM
 * Echo view and inside the Monitor's on-demand capability dialog.
 */
@NullUnmarked
public class DicomCapabilitiesPanel extends VerticalLayout {

	private final Div summary;

	private final DicomCapabilitiesGrid grid;

	public DicomCapabilitiesPanel() {
		setWidthFull();
		setPadding(false);
		setSpacing(false);

		summary = new Div();
		summary.getStyle().set("font-size", "var(--lumo-font-size-s)");
		summary.getStyle().set("padding-bottom", "0.5em");

		grid = new DicomCapabilitiesGrid();
		grid.setWidthFull();
		grid.setAllRowsVisible(true);

		add(summary, grid);
	}

	public void display(DicomCapabilitiesResult capabilities) {
		if (capabilities.isRejected()) {
			summary.setText("Association rejected: " + capabilities.getRejectionReason());
			grid.setCapabilities(List.of());
		}
		else if (capabilities.isUnexpectedError()) {
			summary.setText("Capability probe failed: " + capabilities.getUnexpectedErrorMessage());
			grid.setCapabilities(List.of());
		}
		else {
			StringBuilder text = new StringBuilder();
			text.append(capabilities.getCapabilities().size())
				.append(" SOP class(es) accepted — negotiated max PDU ")
				.append(capabilities.getMaxPduLength())
				.append(" bytes");
			if (capabilities.getRemoteImplementationVersionName() != null) {
				text.append("; peer ").append(capabilities.getRemoteImplementationVersionName());
			}
			summary.setText(text.toString());
			grid.setCapabilities(capabilities.getCapabilities());
		}
	}

}