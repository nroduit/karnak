/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.dicom;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.DicomNodeConfigEntity;
import org.weasis.core.util.annotations.Generated;

/**
 * Grid of the configured DICOM nodes. Rows whose {@link #editablePredicate} returns false
 * (e.g. the dynamic Gateway destinations) carry no edit/delete actions.
 */
@Generated()
@NullUnmarked
public class DicomNodeManagementGrid extends Grid<DicomNodeConfigEntity> {

	private transient Consumer<DicomNodeConfigEntity> editHandler = node -> {
	};

	private transient Consumer<DicomNodeConfigEntity> deleteHandler = node -> {
	};

	private transient Predicate<DicomNodeConfigEntity> editablePredicate = node -> node.getId() != null;

	public DicomNodeManagementGrid() {
		super(DicomNodeConfigEntity.class, false);
		init();
	}

	public void setEditHandler(Consumer<DicomNodeConfigEntity> editHandler) {
		this.editHandler = editHandler;
	}

	public void setDeleteHandler(Consumer<DicomNodeConfigEntity> deleteHandler) {
		this.deleteHandler = deleteHandler;
	}

	public void setEditablePredicate(Predicate<DicomNodeConfigEntity> editablePredicate) {
		this.editablePredicate = editablePredicate;
	}

	private void init() {
		setEmptyStateText("No DICOM nodes configured");
		setSelectionMode(SelectionMode.NONE);
		addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.WRAP_CELL_CONTENT);

		addColumn(DicomNodeConfigEntity::getDescription).setHeader("Description").setAutoWidth(true).setSortable(true);
		addColumn(DicomNodeConfigEntity::getAeTitle).setHeader("AE Title").setAutoWidth(true).setSortable(true);
		addColumn(DicomNodeConfigEntity::getHostname).setHeader("Hostname").setAutoWidth(true).setSortable(true);
		addColumn(DicomNodeConfigEntity::getPort).setHeader("Port").setAutoWidth(true);
		addColumn(DicomNodeConfigEntity::getNodeType).setHeader("Node Type").setAutoWidth(true).setSortable(true);
		addColumn(DicomNodeConfigEntity::getNodeGroup).setHeader("Group").setAutoWidth(true).setSortable(true);
		addColumn(createActionsRenderer()).setHeader("Actions").setAutoWidth(true).setFlexGrow(0);
	}

	private ComponentRenderer<HorizontalLayout, DicomNodeConfigEntity> createActionsRenderer() {
		return new ComponentRenderer<>(node -> {
			HorizontalLayout actions = new HorizontalLayout();
			actions.setPadding(false);
			actions.setSpacing(true);
			if (!editablePredicate.test(node)) {
				return actions;
			}

			Button editBtn = new Button(VaadinIcon.EDIT.create(), event -> editHandler.accept(node));
			editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
			editBtn.setAriaLabel("Edit");

			Button deleteBtn = new Button(VaadinIcon.TRASH.create(), event -> deleteHandler.accept(node));
			deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL,
					ButtonVariant.LUMO_ERROR);
			deleteBtn.setAriaLabel("Delete");

			actions.add(editBtn, deleteBtn);
			return actions;
		});
	}

}
