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

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class ConfirmDialog extends Composite<Dialog> {

	// UI COMPONENTS
	private Dialog dialog;

	private VerticalLayout mainLayout;

	private HorizontalLayout headerLayout;

	private Span titleText;

	private Div messageLayout;

	private HorizontalLayout buttonsLayout;

	private Button yesBtn;

	private Button noBtn;

	// DATA
	private final String messageText;

	public ConfirmDialog(String messageText) {
		this.messageText = messageText;

		init();

		createMainLayout();

		dialog.add(mainLayout);
	}

	public void open() {
		dialog.open();
	}

	public void addConfirmationListener(ComponentEventListener<ConfirmationEvent> listener) {
		addListener(ConfirmationEvent.class, listener);
	}

	private void init() {
		dialog = getContent();
		dialog.setId("confirm-dialog");
		dialog.setCloseOnEsc(false);
		dialog.setCloseOnOutsideClick(false);
	}

	private void createMainLayout() {
		mainLayout = new VerticalLayout();
		mainLayout.setSizeFull();
		mainLayout.addClassName("dialog-container");

		createHeaderLayout();
		createMessageLayout();
		createButtonsLayout();

		mainLayout.add(headerLayout, messageLayout, buttonsLayout);
	}

	private void createHeaderLayout() {
		headerLayout = new HorizontalLayout();
		headerLayout.setWidthFull();
		headerLayout.addClassName("dialog-title");
		headerLayout.setAlignItems(Alignment.BASELINE);

		createTitleText();

		headerLayout.add(titleText);
	}

	private void createTitleText() {
		titleText = new Span("Confirmation");
		titleText.getStyle().set("font-size", "24px");
		titleText.getStyle().set("font-weight", "400");
	}

	private void createMessageLayout() {
		messageLayout = new Div();
		messageLayout.addClassName("dialog-content");

		messageLayout.getElement().setProperty("innerHTML", messageText);
	}

	private void createButtonsLayout() {
		buttonsLayout = new HorizontalLayout();
		buttonsLayout.setWidthFull();
		buttonsLayout.addClassName("dialog-buttons");

		createNoBtn();
		createYesBtn();

		buttonsLayout.add(noBtn, yesBtn);

		noBtn.getElement().getStyle().set("margin-left", "auto"); // https://vaadin.com/forum/thread/17198105/button-alignment-in-horizontal-layout
	}

	private void createYesBtn() {
		yesBtn = new Button();
		yesBtn.addClassName("stroked-button");
		yesBtn.addClassName("primary");
		yesBtn.setText("Confirm");
		yesBtn.setWidth("90px");

		yesBtn.focus();

		yesBtn.addClickListener(e -> {
			fireConfirmationEvent();
			dialog.close();
		});

		yesBtn.addClickShortcut(Key.ENTER);
	}

	private void createNoBtn() {
		noBtn = new Button();
		noBtn.addClassName("primary");
		noBtn.setText("Cancel");
		noBtn.setWidth("90px");

		noBtn.addClickListener(e -> dialog.close());
	}

	private void fireConfirmationEvent() {
		fireEvent(new ConfirmationEvent(this, false));
	}

	public static class ConfirmationEvent extends ComponentEvent<ConfirmDialog> {

		public ConfirmationEvent(ConfirmDialog source, boolean fromClient) {
			super(source, fromClient);
		}

	}

}
