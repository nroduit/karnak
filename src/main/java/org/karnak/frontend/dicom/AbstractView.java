/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.dicom;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.karnak.backend.enums.MessageType;
import org.karnak.backend.model.dicom.Message;
import org.karnak.frontend.component.MessageBox;

public abstract class AbstractView extends Div {

	private static final int DURATION_MSG_INFO_SUCCESS = 5000;

	protected VerticalLayout mainLayout;

	public void displayMessage(Message message) {
		MessageBox messageBox = new MessageBox(message, MessageType.NOTIFICATION_MESSAGE);

		Notification notification = new Notification(messageBox);
		notification.setPosition(Position.TOP_CENTER);
		notification.setDuration(DURATION_MSG_INFO_SUCCESS);
		notification.open();
	}

	protected void setFormResponsive(FormLayout formLayout) {
		formLayout.setResponsiveSteps(new ResponsiveStep("0px", 1), new ResponsiveStep("350px", 2),
				new ResponsiveStep("1050px", 4), new ResponsiveStep("2450px", 8));
	}

}
