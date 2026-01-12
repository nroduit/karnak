/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.kheops;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;
import org.karnak.backend.data.entity.KheopsAlbumsEntity;

public class NewSwitchingAlbum extends Div {

	@Getter
	private final Binder<KheopsAlbumsEntity> binder;

	@Getter
	private final Button buttonAdd;

	private final TextField textAuthorizationDestination;

	private final TextField textAuthorizationSource;

	private final TextField textCondition;

	private final TextField textUrlAPI;

	@Getter
	private final Span textErrorConditionMsg;

	public NewSwitchingAlbum() {
		setWidthFull();

		TextFieldsBindSwitchingAlbum textFieldsBindSwitchingAlbum = new TextFieldsBindSwitchingAlbum();
		binder = textFieldsBindSwitchingAlbum.getBinder();
		buttonAdd = new Button("Add");
		textAuthorizationDestination = textFieldsBindSwitchingAlbum.getTextAuthorizationDestination();
		textAuthorizationSource = textFieldsBindSwitchingAlbum.getTextAuthorizationSource();
		textCondition = textFieldsBindSwitchingAlbum.getTextCondition();
		textErrorConditionMsg = textFieldsBindSwitchingAlbum.getTextErrorConditionMsg();
		textUrlAPI = textFieldsBindSwitchingAlbum.getTextUrlAPI();

		setElements();
		VerticalLayout verticalLayout = new VerticalLayout();
		verticalLayout.add(new HorizontalLayout(textUrlAPI, textAuthorizationDestination, textAuthorizationSource,
				textCondition, buttonAdd));
		verticalLayout.add(new HorizontalLayout(textErrorConditionMsg));
		add(verticalLayout);
		binder.bindInstanceFields(this);
	}

	private void setElements() {
		textErrorConditionMsg.getStyle()
			.set("margin-left", "calc(var(--lumo-border-radius-m) / 4")
			.set("font-size", "var(--lumo-font-size-xs)")
			.set("line-height", "var(--lumo-line-height-xs)")
			.set("color", "var(--lumo-error-text-color)")
			.set("will-change", "max-height")
			.set("transition", "0.4s max-height")
			.set("max-height", "5em");
		textUrlAPI.setWidth("20%");
		textUrlAPI.getStyle().set("padding-right", "10px");
		textUrlAPI.setPlaceholder("Url API");
		textAuthorizationDestination.setWidth("20%");
		textAuthorizationDestination.getStyle().set("padding-right", "10px");
		textAuthorizationDestination.setPlaceholder("Valid token of destination");
		textAuthorizationSource.setWidth("20%");
		textAuthorizationSource.getStyle().set("padding-right", "10px");
		textAuthorizationSource.setPlaceholder("Valid token of source");
		textCondition.setWidth("20%");
		textCondition.getStyle().set("padding-right", "10px");
		textCondition.setPlaceholder("Condition");
	}

	public void clear() {
		binder.readBean(new KheopsAlbumsEntity());
	}

}
