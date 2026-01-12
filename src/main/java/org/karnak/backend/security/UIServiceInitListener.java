/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.security;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.karnak.backend.util.SecurityUtil;
import org.karnak.frontend.authentication.NotAuthorizedScreen;
import org.karnak.frontend.dicom.DicomMainView;
import org.karnak.frontend.extid.ExternalIDView;
import org.karnak.frontend.forwardnode.ForwardNodeView;
import org.karnak.frontend.help.HelpView;
import org.karnak.frontend.profile.ProfileView;
import org.karnak.frontend.project.ProjectView;
import org.karnak.frontend.pseudonym.mapping.PseudonymMappingView;
import org.springframework.stereotype.Component;

@Component
public class UIServiceInitListener implements VaadinServiceInitListener {

	// All view classes
	private final List<? extends Class<? extends com.vaadin.flow.component.Component>> viewClasses = Arrays.asList(
			ForwardNodeView.class, ProfileView.class, ProjectView.class, ExternalIDView.class,
			PseudonymMappingView.class, DicomMainView.class, HelpView.class);

	/**
	 * Listen for the initialization of the UI (the internal root component in Vaadin) and
	 * then add a listener before every view transition
	 * @param event ServiceInitEvent
	 */
	@Override
	public void serviceInit(ServiceInitEvent event) {
		event.getSource().addUIInitListener(uiEvent -> {
			final UI ui = uiEvent.getUI();
			ui.addBeforeEnterListener(this::beforeEnter);
		});
	}

	/**
	 * Check authorized view to display to the user
	 *
	 * <p>
	 * If none redirect to the unauthorized view
	 * @param event BeforeEnterEvent
	 */
	private void beforeEnter(BeforeEnterEvent event) {
		// Lofin screen in memory
		boolean isLoginScreen = Objects.equals(event.getNavigationTarget().getName(),
				"org.karnak.frontend.authentication.LoginScreen");

		// Root view
		boolean isForwardNode = Objects.equals(event.getNavigationTarget().getName(),
				"org.karnak.frontend.forwardnode.ForwardNodeView");

		if (SecurityUtil.isUserLoggedIn() && !SecurityUtil.isAccessGranted(event.getNavigationTarget())
				&& !isLoginScreen) {
			// If root requested
			if (isForwardNode) {
				// List all authorized views and take first one if user request root of
				// the application
				// Try to find first authorized view
				Optional<? extends Class<? extends com.vaadin.flow.component.Component>> firstAuthorizedViewFoundOpt = viewClasses
					.stream()
					.filter(SecurityUtil::isAccessGranted)
					.findFirst();

				// If an authorized view have been found
				if (firstAuthorizedViewFoundOpt.isPresent()) {
					event.rerouteTo(firstAuthorizedViewFoundOpt.get());
				}
				else {
					// No authorized view has been found
					event.rerouteTo(NotAuthorizedScreen.class);
				}
			}
			else {
				// Case direct access not authorized
				event.rerouteTo(NotAuthorizedScreen.class);
			}
		}
	}

}
