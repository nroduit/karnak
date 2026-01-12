/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.forwardnode.edit.destination.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.karnak.backend.data.entity.DestinationEntity;

@Setter
@Getter
public class GridDestination extends Grid<DestinationEntity> {

	private static final Map<Long, Boolean> loadingState = new HashMap<>();

	private UI attachedUi;

	public GridDestination() {
		setSizeFull();

		getListDataView().setIdentifierProvider(DestinationEntity::getId);
		addColumn(DestinationEntity::getDescription).setHeader("Description").setFlexGrow(20).setSortable(true);
		addColumn(DestinationEntity::getDestinationType).setHeader("Type").setFlexGrow(20).setSortable(true);

		addComponentColumn(destination -> {
			Span spanDot = new Span();
			spanDot.getStyle().set("height", "30px");
			spanDot.getStyle().set("width", "30px");
			spanDot.getStyle().set("border-radius", "50%");
			spanDot.getStyle().set("display", "inline-block");
			spanDot.getStyle().set("background-color", destination.isActivate() ? "#5FC04C" : "#FC4848");
			return spanDot;
		}).setHeader("Enabled").setFlexGrow(20).setSortable(true);

		// Loading image - always create fresh based on entity state
		addComponentColumn(destination -> {
			LoadingImage loading = new LoadingImage("In progress", "30px");
			loading.getStyle().set("display", "block");
			loading.getStyle().set("margin", "0 auto");
			loading.setVisible(destination.isTransferInProgress());
			return loading;
		}).setHeader("Activity");
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		attachedUi = attachEvent.getUI();
	}

	public DestinationEntity getSelectedRow() {
		return asSingleSelect().getValue();
	}

	public void refreshLoading(DestinationEntity entity) {
		if (entity == null || attachedUi == null) {
			return;
		}

		boolean loadingUI = isLoading(entity.getId());
		boolean loading = entity.isTransferInProgress();
		if (loadingUI == loading) {
			return;
		}
		setLoading(entity.getId(), loading);

		attachedUi.access(() -> getDataProvider().refreshItem(entity));
	}

	private static boolean isLoading(long id) {
		return loadingState.getOrDefault(id, false);
	}

	private static void setLoading(long id, boolean isLoading) {
		loadingState.put(id, isLoading);
	}

}
