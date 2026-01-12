/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.component;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.karnak.backend.enums.MessageType;
import org.karnak.backend.model.dicom.Message;

public abstract class AbstractDialog extends Composite<Dialog> {

	protected VerticalLayout mainLayout;

	private MessageBox messageBox;

	protected abstract void createMainLayout();

	public void displayMessage(Message message) {
		removeMessage();
		messageBox = new MessageBox(message, MessageType.STATIC_MESSAGE);
		mainLayout.addComponentAtIndex(1, messageBox);
	}

	public void removeMessage() {
		if (messageBox != null) {
			mainLayout.remove(messageBox);
		}
	}

}
