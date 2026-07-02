/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
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
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.List;
import java.util.Optional;
import org.karnak.backend.util.SecurityUtil;
import org.karnak.frontend.dicom.DicomMainView;
import org.karnak.frontend.extid.ExternalIDView;
import org.karnak.frontend.forwardnode.ForwardNodeView;
import org.karnak.frontend.help.HelpView;
import org.karnak.frontend.image.LogoKarnak;
import org.karnak.frontend.profile.ProfileView;
import org.karnak.frontend.project.ProjectView;
import org.karnak.frontend.pseudonym.mapping.PseudonymMappingView;
import org.weasis.core.util.annotations.Generated;

/**
 * UI content when the user is not authorized to see the view. Also serves as the error
 * view of the navigation access control when the access to a view is denied.
 */
@Route(NotAuthorizedScreen.ROUTE)
@PageTitle("Karnak - Not authorized")
@AnonymousAllowed
@Generated()
public class NotAuthorizedScreen extends FlexLayout implements HasErrorParameter<AccessDeniedException> {

	public static final String ROUTE = "not-authorized";

	private static final String NOT_AUTHORIZED_TITLE = "Not Authorized";

	private static final String LOGOUT_LABEL = "Logout";

	// Views to evaluate when looking for a fallback view the user may access
	private static final List<Class<? extends Component>> FALLBACK_VIEWS = List.of(ForwardNodeView.class,
			ProfileView.class, ProjectView.class, ExternalIDView.class, PseudonymMappingView.class, DicomMainView.class,
			HelpView.class);

	// Checks view security annotations (@RolesAllowed / @PermitAll / @AnonymousAllowed)
	// against the current user.
	private final transient AccessAnnotationChecker accessAnnotationChecker;

	public NotAuthorizedScreen(AccessAnnotationChecker accessAnnotationChecker) {
		this.accessAnnotationChecker = accessAnnotationChecker;
		buildUI();
	}

	/**
	 * Called by the navigation access control when the access to a view is denied: if the
	 * root view was requested, forward the user to the first view it may access,
	 * otherwise display this screen
	 * @param event BeforeEnterEvent
	 * @param parameter Access denied error
	 * @return the HTTP status code to set in the response
	 */
	@Override
	public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<AccessDeniedException> parameter) {
		if (event.getLocation().getPath().isEmpty()) {
			// Try to find the first authorized view
			Optional<Class<? extends Component>> firstAuthorizedViewFoundOpt = FALLBACK_VIEWS.stream()
				.filter(accessAnnotationChecker::hasAccess)
				.findFirst();

			// If an authorized view has been found, forward to it
			if (firstAuthorizedViewFoundOpt.isPresent()) {
				event.forwardTo(firstAuthorizedViewFoundOpt.get());
				return HttpStatusCode.OK.getCode();
			}
		}
		return HttpStatusCode.FORBIDDEN.getCode();
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
		logoutButton.addThemeVariants(ButtonVariant.ERROR, ButtonVariant.LARGE);
		return logoutButton;
	}

}
