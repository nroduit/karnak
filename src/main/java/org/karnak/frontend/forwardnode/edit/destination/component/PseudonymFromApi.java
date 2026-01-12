/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.forwardnode.edit.destination.component;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import java.util.Objects;
import lombok.Getter;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.frontend.util.UIS;

@Getter
public class PseudonymFromApi extends Div {

	private final Binder<DestinationEntity> destinationBinder;

	private TextField url;

	private TextField responsePath;

	private TextArea body;

	private Select<String> method;

	private TextField authConfig;

	public PseudonymFromApi(Binder<DestinationEntity> destinationBinder) {
		this.destinationBinder = destinationBinder;
		setWidthFull();
		setElements();
		add(UIS.setWidthFull(new HorizontalLayout(url, method)), body,
				UIS.setWidthFull(new HorizontalLayout(responsePath, authConfig)));
	}

	public void setElements() {
		body = new TextArea("Body (JSON)");
		body.setVisible(false);
		body.setWidthFull();
		url = new TextField("Url");
		url.getStyle().setFlexGrow("1");
		url.setRequired(true);
		method = new Select<>(e -> {
			if (e.getValue() != null) {
				body.setVisible(Objects.equals(e.getValue(), "POST"));
			}
		});
		method.setItems("GET", "POST");
		method.setEmptySelectionAllowed(false);
		method.setValue("GET");
		method.setLabel("Method");
		method.getStyle().setFlexGrow("1");
		method.setRequiredIndicatorVisible(true);
		responsePath = new TextField("JSON Response Path");
		responsePath.getStyle().setFlexGrow("1");
		responsePath.setRequired(true);
		authConfig = new TextField("Authentication Config Code");
		authConfig.getStyle().setFlexGrow("1");
	}

	public void clear() {
		url.clear();
		method.clear();
		body.clear();
		responsePath.clear();
		authConfig.clear();
	}

}
