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

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class WarningConfirmDialog extends Composite<Dialog> {

	private static final String DEFAULT_TITLE = "Warning";

	private static final String DEFAULT_VALIDATE = "Validate";

	private static final String DEFAULT_CANCEL = "Cancel";

	private final Div messageLayout;

	// DATA
	private final String valueTitle;

	private final String valueValidate;

	private final String valueCancel;

	// UI COMPONENT
	private Dialog dialog;

	private VerticalLayout mainLayout;

	private HorizontalLayout headerLayout;

	private Span titleText;

	private HorizontalLayout buttonsLayout;

	private Button buttonValidate;

	private Button buttonCancel;

	public WarningConfirmDialog(Div messageLayout) {
		this(DEFAULT_TITLE, messageLayout, DEFAULT_VALIDATE, DEFAULT_CANCEL);
	}

	public WarningConfirmDialog(String valueTitle, Div messageLayout) {
		this(valueTitle, messageLayout, DEFAULT_VALIDATE, DEFAULT_CANCEL);
	}

	public WarningConfirmDialog(String valueTitle, Div messageLayout, String valueValidate, String valueCancel) {
		this.messageLayout = messageLayout;
		this.valueTitle = valueTitle;
		this.valueValidate = valueValidate;
		this.valueCancel = valueCancel;
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
		dialog.setCloseOnEsc(false);
		dialog.setCloseOnOutsideClick(false);
	}

	private void createMainLayout() {
		mainLayout = new VerticalLayout();
		mainLayout.setSizeFull();

		createHeaderLayout();
		createButtonsLayout();

		mainLayout.add(headerLayout, messageLayout, buttonsLayout);
	}

	private void createHeaderLayout() {
		headerLayout = new HorizontalLayout();
		headerLayout.setWidthFull();
		headerLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

		createTitleText();

		headerLayout.add(titleText);
	}

	private void createTitleText() {
		titleText = new Span(valueTitle);
		titleText.getStyle().set("font-size", "24px");
		titleText.getStyle().set("font-weight", "400");
		titleText.getStyle().set("color", "red");
	}

	private void createButtonsLayout() {
		buttonsLayout = new HorizontalLayout();
		buttonsLayout.setWidthFull();
		createButtonCancel();
		createButtonValidate();
		buttonsLayout.add(buttonCancel, buttonValidate);
		buttonCancel.getElement().getStyle().set("margin-left", "auto");
	}

	private void createButtonValidate() {
		buttonValidate = new Button();
		buttonValidate.setText(valueValidate);
		buttonValidate.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
		buttonValidate.setWidth("90px");
		buttonValidate.focus();
		buttonValidate.addClickListener((ComponentEventListener<ClickEvent<Button>>) event -> {
			fireConfirmationEvent();
			dialog.close();
		});
		buttonValidate.addClickShortcut(Key.ENTER);
	}

	private void createButtonCancel() {
		buttonCancel = new Button();
		buttonCancel.setText(valueCancel);
		buttonCancel.setWidth("90px");
		buttonCancel.addClickListener((ComponentEventListener<ClickEvent<Button>>) event -> dialog.close());
	}

	private void fireConfirmationEvent() {
		fireEvent(new ConfirmationEvent(this, false));
	}

	public static class ConfirmationEvent extends ComponentEvent<WarningConfirmDialog> {

		public ConfirmationEvent(WarningConfirmDialog source, boolean fromClient) {
			super(source, fromClient);
		}

	}

}
