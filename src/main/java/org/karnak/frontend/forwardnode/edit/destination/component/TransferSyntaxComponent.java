/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.forwardnode.edit.destination.component;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.binder.Binder;
import java.util.Arrays;
import lombok.Getter;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.enums.UIDType;

/**
 * Create a transfer syntax component
 */
@Getter
public class TransferSyntaxComponent extends VerticalLayout {

	private Select<String> transferSyntaxSelect;

	public TransferSyntaxComponent() {

		// In order to not have a padding around the component
		setPadding(false);

		// Build transfer syntax components
		buildComponents();

		// Add components
		addComponents();
	}

	/**
	 * Add components in transfer syntax
	 */
	private void addComponents() {
		add(transferSyntaxSelect);
	}

	/**
	 * Build components used in Transfer Syntax component
	 */
	private void buildComponents() {
		transferSyntaxSelect = new Select<>();
		transferSyntaxSelect.setWidth("600px");
		transferSyntaxSelect.getElement().getStyle().set("--vaadin-combo-box-overlay-width", "550px");

		// Values
		transferSyntaxSelect.setEmptySelectionAllowed(true);
		transferSyntaxSelect.setEmptySelectionCaption(UIDType.DEFAULT_DESCRIPTION);
		transferSyntaxSelect.setItems(Arrays.stream(UIDType.values()).map(UIDType::getCode).toList());

		// Labels
		transferSyntaxSelect.setLabel("Transfer Syntax");
		transferSyntaxSelect.setItemLabelGenerator(UIDType::descriptionOf);
	}

	public void init(Binder<DestinationEntity> binder) {
		binder.forField(transferSyntaxSelect)
			.bind(DestinationEntity::getTransferSyntax, DestinationEntity::setTransferSyntax);
	}

}
