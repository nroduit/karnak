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

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * Factory for the buttons that are shared, look-alike across the configuration views, so
 * that the same action always renders the same way.
 */
public final class ButtonFactory {

	private ButtonFactory() {
	}

	/**
	 * Build the primary "add a new item" button used to create a new node, project, etc.:
	 * a {@link ButtonVariant#PRIMARY} button prefixed with a plus icon.
	 * @param text the button label (e.g. {@code "New forward node"})
	 * @return the configured button
	 */
	public static Button createAddButton(String text) {
		Button button = new Button(text, VaadinIcon.PLUS_CIRCLE.create());
		button.addThemeVariants(ButtonVariant.PRIMARY);
		return button;
	}

}
