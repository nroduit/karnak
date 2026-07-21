/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.forwardnode.edit.destination.component;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import java.util.Objects;
import lombok.Getter;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.DestinationEntity;
import org.weasis.core.util.annotations.Generated;

@Getter
@Generated()
@NullUnmarked
public class PseudonymFromApi extends Div {

	private final Binder<DestinationEntity> destinationBinder;

	private TextField url;

	private TextField responsePath;

	private TextArea body;

	private Select<String> method;

	private ComboBox<String> authConfig;

	public PseudonymFromApi(Binder<DestinationEntity> destinationBinder) {
		this.destinationBinder = destinationBinder;
		setWidthFull();
		setElements();

		// A responsive grid on three columns when the panel is wide enough, collapsing to
		// a single column on a narrow destination panel. Each field fills its column(s),
		// so labels never overflow or wrap awkwardly the way they did with
		// fixed-percentage HorizontalLayout rows under the Aura theme. Colspans give the
		// URL and JSON Response Path the extra width they need, while the Method selector
		// and the authentication-config combo stay compact in a single column.
		FormLayout formLayout = new FormLayout();
		formLayout.setWidthFull();
		formLayout.setResponsiveSteps(new ResponsiveStep("0", 1), new ResponsiveStep("25em", 3));
		formLayout.add(url, method, body, responsePath, authConfig);
		formLayout.setColspan(url, 2);
		formLayout.setColspan(body, 3);
		formLayout.setColspan(responsePath, 2);
		add(formLayout);
	}

	public void setElements() {
		body = new TextArea("Body (JSON)");
		body.setVisible(false);
		url = new TextField("Url");
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
		method.setRequiredIndicatorVisible(true);
		responsePath = new TextField("JSON Response Path");
		responsePath.setRequired(true);
		// Pick the authentication config from the existing identifiers rather than typing
		// a free-text code; items are populated by the parent view.
		authConfig = new ComboBox<>("Authentication Config Code");
	}

	public void clear() {
		url.clear();
		method.clear();
		body.clear();
		responsePath.clear();
		authConfig.clear();
	}

}
