/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.help;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.karnak.frontend.MainLayout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.annotation.Secured;

@Route(value = HelpView.ROUTE, layout = MainLayout.class)
@PageTitle("Karnak - Help")
@Tag("help-view")
@Secured({ "ROLE_admin", "ROLE_user" })
public class HelpView extends VerticalLayout {

	public static final String VIEW_NAME = "Help";

	public static final String ROUTE = "help";

	private final String appVersion;

	public HelpView(@Value("${spring.application.version:Development}") String appVersion) {
		this.appVersion = appVersion;
		setSizeFull();
		H1 heading = new H1("Help");

		Anchor generalDoc = new Anchor("https://osirix-foundation.github.io/karnak-documentation/",
				"General documentation");
		generalDoc.setTarget("_blank");

		Anchor installation = new Anchor("https://osirix-foundation.github.io/karnak-documentation/en/userguide/",
				"User guide: configuration and administration");
		installation.setTarget("_blank");

		Anchor profile = new Anchor("https://osirix-foundation.github.io/karnak-documentation/en/profiles/",
				"Build your own profile for de-identification or for tag morphing");
		profile.setTarget("_blank");

		VerticalLayout layout = new VerticalLayout();
		layout.add(heading, generalDoc, installation, profile, createAboutSection());
		this.add(layout);
	}

	private Component createAboutSection() {
		VerticalLayout aboutLayout = new VerticalLayout();
		aboutLayout.setSpacing(false);
		aboutLayout.setPadding(false);

		H2 aboutHeading = new H2("About");

		String javaVersion = System.getProperty("java.version");
		String javaVendor = System.getProperty("java.vendor");
		String osName = System.getProperty("os.name");
		String osVersion = System.getProperty("os.version");
		String osArch = System.getProperty("os.arch");
		String vaadinVersion = com.vaadin.flow.component.Component.class.getPackage().getImplementationVersion();
		String springVersion = org.springframework.core.SpringVersion.getVersion();
		String springBootVersion = org.springframework.boot.SpringBootVersion.getVersion();

		Span versionInfo = new Span("Karnak: " + appVersion);
		Span vaadinInfo = new Span("Vaadin: " + (vaadinVersion != null ? vaadinVersion : "Unknown"));
		Span springInfo = new Span("Spring: " + (springVersion != null ? springVersion : "Unknown"));
		Span springBootInfo = new Span("Spring Boot: " + springBootVersion);
		Span javaInfo = new Span("Java: " + javaVersion + " (" + javaVendor + ")");
		Span systemInfo = new Span("System: " + osName + " " + osVersion + " (" + osArch + ")");

		aboutLayout.add(aboutHeading, versionInfo, vaadinInfo, springInfo, springBootInfo, javaInfo, systemInfo);
		return aboutLayout;
	}

}
