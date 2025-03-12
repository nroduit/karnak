/*
 * Copyright (c) 2022 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.util;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;

public class NotificationUtil {

	private NotificationUtil() {
	}

	/**
	 * Display error message if issue when exporting
	 * @param message Message to display
	 */
	public static void displayErrorMessage(String message, Notification.Position position) {
		Span content = new Span(message);
		content.getStyle().set("color", "var(--lumo-error-text-color)");
		Notification notification = new Notification(content);
		notification.setDuration(3000);
		notification.setPosition(position);
		notification.open();
	}

}
