/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.authentication;

import static org.karnak.frontend.authentication.LoginScreen.KARNAK_TITLE;
import static org.karnak.frontend.authentication.LoginScreen.LOGO_SIZE;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.karnak.backend.util.SecurityUtil;
import org.karnak.frontend.image.LogoKarnak;

/**
 * UI content when the user is not authorized to see the view.
 */
@Route(NotAuthorizedScreen.ROUTE)
@PageTitle("Karnak - Not authorized")
@CssImport(value = "./styles/shared-styles.css")
public class NotAuthorizedScreen extends FlexLayout {

	public static final String ROUTE = "not-authorized";

	private static final String NOT_AUTHORIZED_TITLE = "Not Authorized";

	private static final String LOGOUT_LABEL = "Logout";

	public NotAuthorizedScreen() {
		buildUI();
	}

	private void buildUI() {
		setSizeFull();
		setClassName("not-authorized-screen");

		ThemeUtil.initializeTheme();
		add(buildNotAuthorizedComponent());
	}

	private Component buildNotAuthorizedComponent() {

		VerticalLayout notAuthorizedLayout = new VerticalLayout();
		notAuthorizedLayout.setJustifyContentMode(JustifyContentMode.CENTER);
		notAuthorizedLayout.setAlignItems(Alignment.CENTER);
		LogoKarnak logoKarnak = new LogoKarnak(KARNAK_TITLE, LOGO_SIZE);
		Button logoutButton = createLogoutButton();

		notAuthorizedLayout.add(logoKarnak, new H1(KARNAK_TITLE), new H1(NOT_AUTHORIZED_TITLE), logoutButton);

		return notAuthorizedLayout;
	}

	private Button createLogoutButton() {
		Button logoutButton = new Button(LOGOUT_LABEL, VaadinIcon.SIGN_OUT.create());
		logoutButton.addClickListener(event -> SecurityUtil.signOut());
		logoutButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_LARGE);
		return logoutButton;
	}

}
