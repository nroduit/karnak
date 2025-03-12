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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.Lumo;
import org.karnak.backend.util.SecurityUtil;
import org.karnak.frontend.image.LogoKarnak;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * UI content when the user is not authorized to see the view.
 */
@Route(NotAuthorizedScreen.ROUTE)
@PageTitle("KARNAK - Not authorized")
@CssImport(value = "./styles/shared-styles.css")
@SuppressWarnings("serial")
public class NotAuthorizedScreen extends FlexLayout {

	// View route
	public static final String ROUTE = "not-authorized";

	// Theme
	private static final String THEME_COLOR_KEY = "theme-variant";

	@Autowired
	public NotAuthorizedScreen() {
		buildUI();
	}

	/**
	 * Build User Interface
	 */
	private void buildUI() {
		setSizeFull();
		setClassName("not-authorized-screen");

		// read local storage theme
		UI.getCurrent()
			.getPage()
			.executeJs("return localStorage.getItem($0)", THEME_COLOR_KEY)
			.then(String.class, string -> {
				final String themeColor = string;
				if ((string != null) && (string.equals(Lumo.DARK) || string.equals(Lumo.LIGHT))) {
					UI.getCurrent().getElement().setAttribute("theme", themeColor);
					UI.getCurrent().getPage().executeJs("localStorage.setItem($0, $1)", THEME_COLOR_KEY, themeColor);
				}
			});

		// Build component
		add(buildNotAuthorizedComponent());
	}

	/**
	 * Build not authorized component
	 * @return the built component
	 */
	private Component buildNotAuthorizedComponent() {

		// layout to center login form when there is sufficient screen space
		VerticalLayout notAuthorizedLayout = new VerticalLayout();
		notAuthorizedLayout.setJustifyContentMode(JustifyContentMode.CENTER);
		notAuthorizedLayout.setAlignItems(Alignment.CENTER);
		LogoKarnak logoKarnak = new LogoKarnak("KARNAK", "225px");
		notAuthorizedLayout.add(logoKarnak);
		notAuthorizedLayout.add(new H1("KARNAK"));
		notAuthorizedLayout.add(new H1("Not Authorized"));

		// logout button
		Button logoutButton = new Button("Logout", VaadinIcon.SIGN_OUT.create());
		logoutButton.addClickListener(event -> SecurityUtil.signOut());
		logoutButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
		logoutButton.addThemeVariants(ButtonVariant.LUMO_LARGE);
		notAuthorizedLayout.add(logoutButton);

		return notAuthorizedLayout;
	}

}
