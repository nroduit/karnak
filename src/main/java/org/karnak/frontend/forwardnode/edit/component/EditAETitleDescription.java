/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.forwardnode.edit.component;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import org.karnak.backend.data.entity.ForwardNodeEntity;

public class EditAETitleDescription extends HorizontalLayout {

	private final TextField textFieldAETitle;

	private final TextField textFieldDescription;

	private final Binder<ForwardNodeEntity> binder;

	public EditAETitleDescription(Binder<ForwardNodeEntity> binder) {
		this.binder = binder;
		textFieldAETitle = new TextField("Forward AETitle");
		textFieldDescription = new TextField("Description");

		textFieldAETitle.setWidth("30%");
		textFieldDescription.setWidth("70%");
		add(textFieldAETitle, textFieldDescription);
		setBinder();
	}

	public void setForwardNode(ForwardNodeEntity forwardNodeEntity) {
		if (forwardNodeEntity != null) {
			binder.readBean(forwardNodeEntity);
			setEnabled(true);
		}
		else {
			binder.readBean(null);
			textFieldDescription.clear();
			textFieldAETitle.clear();
			setEnabled(false);
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		textFieldAETitle.setEnabled(enabled);
		textFieldDescription.setEnabled(enabled);
	}

	private void setBinder() {
		binder.forField(textFieldAETitle)
			.withValidator(value -> !value.equals(""), "Forward AE Title is mandatory")
			.withValidator(value -> value.length() <= 16, "Forward AETitle has more than 16 characters")
			.bind(ForwardNodeEntity::getFwdAeTitle, ForwardNodeEntity::setFwdAeTitle);
		binder.forField(textFieldDescription)
			.bind(ForwardNodeEntity::getFwdDescription, ForwardNodeEntity::setFwdDescription);
	}

}
