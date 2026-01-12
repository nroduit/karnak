/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.forwardnode.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.server.VaadinRequest;
import java.util.List;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.util.SystemPropertyUtil;
import org.karnak.frontend.util.CollatorUtils;

public class GridForwardNode extends Grid<ForwardNodeEntity> {

	public GridForwardNode() {
		setSizeFull();

		Column<ForwardNodeEntity> forwardAeTitleColumn = addComponentColumn(forwardNode -> {
			Span span = new Span(forwardNode.getFwdAeTitle());
			Button copyButton = createCopyButton(forwardNode);

			HorizontalLayout layout = new HorizontalLayout(span, copyButton);
			layout.setAlignItems(Alignment.CENTER);
			layout.setSpacing(true);
			return layout;
		}).setHeader("Forward AETitle")
			.setFlexGrow(20)
			.setSortable(true)
			.setComparator(CollatorUtils.comparing(ForwardNodeEntity::getFwdAeTitle));

		addColumn(ForwardNodeEntity::getFwdDescription).setHeader("Description")
			.setFlexGrow(20)
			.setSortable(true)
			.setComparator(CollatorUtils.comparing(ForwardNodeEntity::getFwdDescription));

		GridSortOrder<ForwardNodeEntity> order = new GridSortOrder<>(forwardAeTitleColumn, SortDirection.ASCENDING);
		sort(List.of(order));
	}

	private Button createCopyButton(ForwardNodeEntity forwardNode) {
		Button copyButton = new Button(VaadinIcon.COPY.create());
		copyButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
		copyButton.getElement().setAttribute("aria-label", "Copy DICOM configuration");

		copyButton.addClickListener(event -> {
			String config = buildDicomConfigTooltip(forwardNode);
			copyButton.getElement().executeJs("navigator.clipboard.writeText($0).then(() => {}, () => {})", config);

			Notification notification = Notification.show("DICOM configuration copied to clipboard");
			notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
			notification.setDuration(2000);
			notification.setPosition(Position.MIDDLE);
		});

		return copyButton;
	}

	private String buildDicomConfigTooltip(ForwardNodeEntity forwardNode) {
		StringBuilder tooltip = new StringBuilder();
		tooltip.append("DICOM Configuration\n");
		tooltip.append("===================\n");
		tooltip.append("Description: ").append(forwardNode.getFwdDescription()).append("\n");
		tooltip.append("AE Title: ").append(forwardNode.getFwdAeTitle()).append("\n");

		String host = VaadinRequest.getCurrent().getRemoteHost();
		tooltip.append("Host: ").append(host).append("\n");

		Integer listenerPort = SystemPropertyUtil.retrieveIntegerSystemProperty("DICOM_LISTENER_PORT", 11119);
		tooltip.append("Port: ").append(listenerPort).append("\n\n");
		tooltip.append("Note: if necessary adapt the host to the visible address from the sending node.\n");
		return tooltip.toString();
	}

	public ForwardNodeEntity getSelectedRow() {
		return asSingleSelect().getValue();
	}

	public void refresh(ForwardNodeEntity data) {
		getDataCommunicator().refresh(data);
	}

	public void selectRow(ForwardNodeEntity row) {
		getSelectionModel().select(row);
	}

}
