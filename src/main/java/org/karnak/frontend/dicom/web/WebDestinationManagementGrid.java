/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.dicom.web;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.WebDestinationConfigEntity;
import org.karnak.backend.enums.DicomWebServiceType;
import org.karnak.backend.service.WebDestinationConfigService;
import org.weasis.core.util.annotations.Generated;

/**
 * Grid of the configured DICOMweb endpoints. Rows whose {@link #editablePredicate}
 * returns false (e.g. the dynamic gateway STOW-RS destinations) carry no edit/delete
 * actions.
 */
@Generated()
@NullUnmarked
public class WebDestinationManagementGrid extends Grid<WebDestinationConfigEntity> {

	private transient Consumer<WebDestinationConfigEntity> editHandler = endpoint -> {
	};

	private transient Consumer<WebDestinationConfigEntity> deleteHandler = endpoint -> {
	};

	private transient Predicate<WebDestinationConfigEntity> editablePredicate = endpoint -> endpoint.getId() != null;

	public WebDestinationManagementGrid() {
		super(WebDestinationConfigEntity.class, false);
		init();
	}

	public void setEditHandler(Consumer<WebDestinationConfigEntity> editHandler) {
		this.editHandler = editHandler;
	}

	public void setDeleteHandler(Consumer<WebDestinationConfigEntity> deleteHandler) {
		this.deleteHandler = deleteHandler;
	}

	public void setEditablePredicate(Predicate<WebDestinationConfigEntity> editablePredicate) {
		this.editablePredicate = editablePredicate;
	}

	private void init() {
		setEmptyStateText("No DICOMweb endpoints configured");
		setSelectionMode(SelectionMode.NONE);
		addThemeVariants(GridVariant.WRAP_CELL_CONTENT);

		addColumn(WebDestinationConfigEntity::getDescription).setHeader("Description")
			.setAutoWidth(true)
			.setSortable(true);
		addColumn(WebDestinationConfigEntity::getUrl).setHeader("URL").setAutoWidth(true).setSortable(true);
		addColumn(WebDestinationManagementGrid::servicesDisplay).setHeader("Services").setAutoWidth(true);
		addColumn(WebDestinationConfigEntity::getGroupName).setHeader("Group").setAutoWidth(true).setSortable(true);
		addColumn(createActionsRenderer()).setHeader("Actions").setAutoWidth(true).setFlexGrow(0);
	}

	private static String servicesDisplay(WebDestinationConfigEntity endpoint) {
		var services = WebDestinationConfigService.decodeServices(endpoint.getServices());
		return services.isEmpty() ? "All services"
				: services.stream().map(DicomWebServiceType::getDisplayName).collect(Collectors.joining(", "));
	}

	private ComponentRenderer<HorizontalLayout, WebDestinationConfigEntity> createActionsRenderer() {
		return new ComponentRenderer<>(endpoint -> {
			HorizontalLayout actions = new HorizontalLayout();
			actions.setPadding(false);
			actions.setSpacing(true);
			if (!editablePredicate.test(endpoint)) {
				return actions;
			}

			Button editBtn = new Button(VaadinIcon.EDIT.create(), event -> editHandler.accept(endpoint));
			editBtn.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
			editBtn.setAriaLabel("Edit");

			Button deleteBtn = new Button(VaadinIcon.TRASH.create(), event -> deleteHandler.accept(endpoint));
			deleteBtn.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL, ButtonVariant.ERROR);
			deleteBtn.setAriaLabel("Delete");

			actions.add(editBtn, deleteBtn);
			return actions;
		});
	}

}
