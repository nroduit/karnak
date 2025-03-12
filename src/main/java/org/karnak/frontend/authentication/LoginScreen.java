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
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.Lumo;
import java.util.Objects;
import org.karnak.backend.cache.RequestCache;
import org.karnak.backend.enums.SecurityRole;
import org.karnak.frontend.image.LogoKarnak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * UI content when the user is not logged in yet.
 */
@Route(LoginScreen.ROUTE)
@PageTitle("KARNAK - Login")
@CssImport(value = "./styles/shared-styles.css")
@SuppressWarnings("serial")
public class LoginScreen extends FlexLayout implements BeforeEnterObserver {

	// View route
	public static final String ROUTE = "login";

	// Theme
	private final String THEME_COLOR_KEY = "theme-variant";

	// Login form
	private final LoginForm loginForm;

	// Authentication manager
	private final AuthenticationManager authenticationManager;

	// Request cache
	private final RequestCache requestCache;

	@Autowired
	public LoginScreen(AuthenticationManager authenticationManager, RequestCache requestCache) {
		this.loginForm = new LoginForm();
		this.authenticationManager = authenticationManager;
		this.requestCache = requestCache;
		buildUI();
	}

	@Override
	public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
		// inform the user about an authentication error
		if (beforeEnterEvent.getLocation().getQueryParameters().getParameters().containsKey("error")) {
			loginForm.setError(true);
		}
	}

	/**
	 * Build User Interface
	 */
	private void buildUI() {
		setSizeFull();
		setClassName("login-screen");

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
		add(buildLoginMainComponent());

		// It's ugly but it works. @see
		// https://github.com/vaadin/vaadin-login-flow/issues/53
		UI.getCurrent().getPage().executeJs("document.getElementById(\"vaadinLoginUsername\").focus();");
	}

	/**
	 * Build login component
	 * @return the built component
	 */
	private Component buildLoginMainComponent() {

		// sets the LoginForm action to "login" in order to post the login form to Spring
		// Security
		loginForm.setAction(ROUTE);

		// deactivate forgot password button
		loginForm.setForgotPasswordButtonVisible(false);

		// Listener on login form
		loginForm.addLoginListener(this::login);

		// layout to center login form when there is sufficient screen space
		VerticalLayout loginInformation = new VerticalLayout();
		loginInformation.setJustifyContentMode(JustifyContentMode.CENTER);
		loginInformation.setAlignItems(Alignment.CENTER);
		LogoKarnak logoKarnak = new LogoKarnak("KARNAK", "225px");
		loginInformation.add(logoKarnak);
		loginInformation.add(new H1("KARNAK"));
		loginInformation.add(loginForm);

		return loginInformation;
	}

	/**
	 * Manage event on login form submit
	 * @param event Login Event
	 */
	private void login(LoginForm.LoginEvent event) {
		try {
			// try to authenticate with given credentials,
			// should always return not null or throw an {@link AuthenticationException}
			final Authentication authentication = authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(event.getUsername(), event.getPassword()));

			// if the user is admin
			if (authentication.getAuthorities()
				.stream()
				.anyMatch(ga -> Objects.equals(ga.getAuthority(), SecurityRole.ADMIN_ROLE.getRole()))) {

				// if authentication was successful and user is admin,
				// we will update the security context and redirect to the page requested
				// first
				SecurityContextHolder.getContext().setAuthentication(authentication);
				UI.getCurrent().navigate(requestCache.resolveRedirectUrl());
			}
		}
		catch (AuthenticationException ex) { //
			// show default error message
			loginForm.setError(true);
		}
	}

}
