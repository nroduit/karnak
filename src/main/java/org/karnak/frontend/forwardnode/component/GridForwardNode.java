/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.forwardnode.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.util.SystemPropertyUtil;
import org.karnak.frontend.util.CollatorUtils;
import org.karnak.frontend.util.GroupTreeGrid;
import org.karnak.frontend.util.GroupTreeNode;
import org.weasis.core.util.annotations.Generated;

@Generated()
@NullUnmarked
public class GridForwardNode extends GroupTreeGrid<ForwardNodeEntity> {

	public GridForwardNode() {
		setSizeFull();

		var forwardAeTitleColumn = addPrimaryColumn("Forward AETitle", this::buildAeTitleCell,
				CollatorUtils.comparing(ForwardNodeEntity::getFwdAeTitle))
			.setFlexGrow(20);

		addItemTextColumn("Description", ForwardNodeEntity::getFwdDescription,
				CollatorUtils.comparing(ForwardNodeEntity::getFwdDescription))
			.setFlexGrow(20);

		sort(List.of(new GridSortOrder<>(forwardAeTitleColumn, SortDirection.ASCENDING)));
	}

	private Component buildAeTitleCell(ForwardNodeEntity forwardNode) {
		Span span = new Span(forwardNode.getFwdAeTitle());
		Button copyButton = createCopyButton(forwardNode);

		HorizontalLayout layout = new HorizontalLayout(span, copyButton);
		layout.setAlignItems(Alignment.CENTER);
		layout.setSpacing(true);
		return layout;
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
		GroupTreeNode<ForwardNodeEntity> value = asSingleSelect().getValue();
		return value instanceof GroupTreeNode.ItemNode<ForwardNodeEntity> item ? item.item() : null;
	}

	public void refresh(ForwardNodeEntity data) {
		getDataProvider().refreshItem(new GroupTreeNode.ItemNode<>(data, data.getId()));
	}

	public void selectRow(ForwardNodeEntity row) {
		selectItem(row);
	}

}
