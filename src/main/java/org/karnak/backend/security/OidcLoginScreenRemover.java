/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.security;

import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import lombok.extern.slf4j.Slf4j;
import org.karnak.frontend.authentication.LoginScreen;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Removes the form login screen route when the OpenID Connect identity provider is
 * active: authentication is then handled by the IDP and the form login of the
 * {@link LoginScreen} would not work. Without the route, a request to its path requires
 * authentication like any other one and is therefore redirected to the IDP.
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "IDP", havingValue = "oidc")
public class OidcLoginScreenRemover implements VaadinServiceInitListener {

	@Override
	public void serviceInit(ServiceInitEvent event) {
		RouteConfiguration routeConfiguration = RouteConfiguration
			.forRegistry(event.getSource().getRouter().getRegistry());
		if (routeConfiguration.isRouteRegistered(LoginScreen.class)) {
			routeConfiguration.removeRoute(LoginScreen.class);
			log.info("Form login screen removed: authentication is delegated to the OIDC identity provider");
		}
	}

}