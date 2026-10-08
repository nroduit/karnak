/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.function.SerializableSupplier;
import org.weasis.core.util.annotations.Generated;

/**
 * Reusable "create a new item" popup shared by the configuration views (forward node,
 * project, profile…): the feature-specific fields are shown in the body and a uniform
 * "add" / "Cancel" pair sits in the footer.
 *
 * <p>
 * The view supplies the creation logic through
 * {@link #setOnConfirm(SerializableSupplier)}: it returns {@code true} when the item has
 * been created (the popup then closes) or {@code false} to keep the popup open, e.g. when
 * validation failed.
 */
@Generated()
public class NewItemDialog extends Dialog {

	private SerializableSupplier<Boolean> onConfirm = () -> true;

	/**
	 * @param title the popup header (e.g. {@code "New forward node"})
	 * @param confirmText the label of the confirm button (e.g. {@code "Add"})
	 * @param content the feature-specific fields shown in the popup body
	 */
	public NewItemDialog(String title, String confirmText, Component... content) {
		setHeaderTitle(title);

		VerticalLayout body = new VerticalLayout(content);
		body.setPadding(false);
		body.setSpacing(true);
		add(body);

		Button confirm = ButtonFactory.createAddButton(confirmText);
		confirm.addClickListener(event -> {
			if (onConfirm.get()) {
				close();
			}
		});
		// Confirm on Enter, but only while this popup is open.
		confirm.addClickShortcut(Key.ENTER).listenOn(this);

		Button cancel = new Button("Cancel", event -> close());

		getFooter().add(cancel, confirm);
	}

	/**
	 * Set the action run when the confirm button (or Enter) is triggered.
	 * @param onConfirm returns {@code true} to close the popup, {@code false} to keep it
	 * open (e.g. on a validation error)
	 */
	public void setOnConfirm(SerializableSupplier<Boolean> onConfirm) {
		this.onConfirm = onConfirm != null ? onConfirm : () -> true;
	}

}
