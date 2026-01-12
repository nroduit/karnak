/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.profile.component.editprofile;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;

public class ProfileShowHide extends Div {

	private final Component component;

	private final Button btnShowHide = new Button();

	private Boolean show = true;

	private String textShow = "Show";

	private String textHide = "Hide";

	public ProfileShowHide(Component component, Boolean show) {
		this.component = component;
		this.show = show;
		setStyle();
	}

	public void setTextShow(String textShow) {
		this.textShow = textShow;
		setTextButtonShowHide();
	}

	public void setTextHide(String textHide) {
		this.textHide = textHide;
		setTextButtonShowHide();
	}

	private void setStyle() {
		getStyle().set("margin-top", "0px");
	}

	private void setTextButtonShowHide() {
		btnShowHide.setText(show ? textHide : textShow);
	}

	public void setView() {
		removeAll();
		component.setVisible(show);
		setTextButtonShowHide();
		btnShowHide.addClickListener(buttonClickEvent -> {
			show = !show;
			component.setVisible(show);
			setTextButtonShowHide();
		});
		add(component);
		add(btnShowHide);
	}

}
