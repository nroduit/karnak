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

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class ButtonSaveDeleteCancel extends HorizontalLayout {

	private final Button save;

	private final Button delete;

	private final Button cancel;

	private final String LABEL_SAVE = "Save";

	private final String LABEL_CANCEL = "Cancel";

	private final String LABEL_DELETE = "Delete";

	public ButtonSaveDeleteCancel() {
		save = new Button(LABEL_SAVE);
		cancel = new Button(LABEL_CANCEL);
		delete = new Button(LABEL_DELETE);

		add(save, delete, cancel);
		setButtonSave();
		setButtonCancel();
		setButtonDelete();
	}

	private void setButtonSave() {
		save.getStyle().set("flex-grow", "1");
		save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
	}

	private void setButtonCancel() {
		cancel.getStyle().set("flex-grow", "1");
	}

	private void setButtonDelete() {
		delete.getStyle().set("flex-grow", "1");
		delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
	}

	public void setEnabled(boolean enabled) {
		save.setEnabled(enabled);
		delete.setEnabled(enabled);
		cancel.setEnabled(enabled);
	}

	public Button getSave() {
		return save;
	}

	public Button getDelete() {
		return delete;
	}

	public Button getCancel() {
		return cancel;
	}

}
