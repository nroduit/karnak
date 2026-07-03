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

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import java.util.List;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.model.dicom.result.SopClassCapability;

/**
 * Grid presenting the SOP Classes a peer accepted during a non-invasive capability probe,
 * one row per {@link SopClassCapability}: its functional category, the SOP Class name and
 * the transfer syntaxes the peer accepted for it.
 */
@NullUnmarked
public class DicomCapabilitiesGrid extends Grid<SopClassCapability> {

	public DicomCapabilitiesGrid() {
		super(SopClassCapability.class, false);

		setSelectionMode(SelectionMode.NONE);
		setEmptyStateText("The peer accepted none of the proposed SOP Classes");
		addThemeVariants(GridVariant.WRAP_CELL_CONTENT);

		addColumn(SopClassCapability::category).setHeader("Category").setAutoWidth(true).setFlexGrow(0);
		addColumn(SopClassCapability::sopClassName).setHeader("SOP Class").setAutoWidth(true).setFlexGrow(0);
		addColumn((capability) -> String.join(", ", capability.transferSyntaxes()))
			.setHeader("Accepted Transfer Syntaxes");
	}

	public void setCapabilities(List<SopClassCapability> capabilities) {
		setItems(capabilities);
	}

}