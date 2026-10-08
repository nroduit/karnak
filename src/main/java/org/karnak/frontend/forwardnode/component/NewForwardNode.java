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

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import lombok.Getter;
import org.karnak.frontend.component.ButtonFactory;
import org.karnak.frontend.component.NewItemDialog;
import org.weasis.core.util.annotations.Generated;

/**
 * Toolbar entry that creates a new forward node: a "New forward node" button which opens
 * a popup ({@link NewItemDialog}) where the forward AETitle is entered.
 */
@Getter
@Generated()
public class NewForwardNode extends HorizontalLayout {

	private final Button newForwardNodeBtn;

	private final TextField newAETitleForwardNode;

	private final NewItemDialog dialog;

	public NewForwardNode() {
		newAETitleForwardNode = new TextField("Forward AETitle");
		newAETitleForwardNode.setPlaceholder("Forward AETitle");
		newAETitleForwardNode.setWidthFull();

		dialog = new NewItemDialog("New forward node", "Add", newAETitleForwardNode);

		newForwardNodeBtn = ButtonFactory.createAddButton("New forward node");
		newForwardNodeBtn.addClickListener(click -> openDialog());
		// CTRL+N will create a new window which is unavoidable
		newForwardNodeBtn.addClickShortcut(Key.KEY_N, KeyModifier.ALT);

		setPadding(false);
		add(newForwardNodeBtn);
	}

	private void openDialog() {
		newAETitleForwardNode.clear();
		dialog.open();
		newAETitleForwardNode.focus();
	}

}
