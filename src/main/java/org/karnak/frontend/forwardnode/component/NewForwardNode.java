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

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import lombok.Getter;

@Getter
public class NewForwardNode extends HorizontalLayout {

	private final Button newForwardNodeBtn;

	private final TextField newAETitleForwardNode;

	private final Button addNewForwardNode;

	private final Button cancelNewForwardNode;

	public NewForwardNode() {
		newAETitleForwardNode = new TextField();
		addNewForwardNode = new Button("Add");
		cancelNewForwardNode = new Button("Cancel");
		newForwardNodeBtn = new Button("New forward node");
		initView();
	}

	private void initView() {
		setNewAETitleForwardNode();
		setAddNewForwardNode();
		setCancelNewForwardNode();
		setNewForwardNode();
		add(newForwardNodeBtn);
	}

	private void setNewAETitleForwardNode() {
		newAETitleForwardNode.setPlaceholder("Forward AETitle");
		newAETitleForwardNode.addKeyDownListener(Key.ENTER, keyDownEvent -> {
			removeAll();
			add(newForwardNodeBtn);
		});
	}

	private void setAddNewForwardNode() {
		addNewForwardNode.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		addNewForwardNode.setIcon(VaadinIcon.PLUS_CIRCLE.create());
		addNewForwardNode.addClickListener(click -> {
			removeAll();
			add(newForwardNodeBtn);
		});
	}

	private void setCancelNewForwardNode() {
		cancelNewForwardNode.addClickListener(click -> {
			removeAll();
			add(newForwardNodeBtn);
		});
	}

	private void setNewForwardNode() {
		newForwardNodeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		newForwardNodeBtn.setIcon(VaadinIcon.PLUS_CIRCLE.create());
		newForwardNodeBtn.addClickListener(click -> {
			removeAll();
			newAETitleForwardNode.setValue("");
			add(newAETitleForwardNode, addNewForwardNode, cancelNewForwardNode);
		});
		// CTRL+N will create a new window which is unavoidable
		newForwardNodeBtn.addClickShortcut(Key.KEY_N, KeyModifier.ALT);
	}

}
