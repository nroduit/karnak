/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import jakarta.annotation.security.PermitAll;
import org.karnak.backend.util.SecurityUtil;
import org.karnak.frontend.authconfig.AuthConfigView;
import org.karnak.frontend.dicom.DicomMainView;
import org.karnak.frontend.extid.ExternalIDView;
import org.karnak.frontend.forwardnode.ForwardNodeView;
import org.karnak.frontend.help.HelpView;
import org.karnak.frontend.monitoring.MonitoringView;
import org.karnak.frontend.profile.ProfileView;
import org.karnak.frontend.project.ProjectView;
import org.weasis.core.util.annotations.Generated;

/**
 * The main layout. Contains the navigation menu.
 *
 * <p>
 * The navigation access control also evaluates the parent layout of a view: without an
 * annotation the layout would deny everything, so it permits all authenticated users and
 * each view further restricts the access with its own annotation.
 */
@PermitAll
@Generated()
public class MainLayout extends FlexLayout implements RouterLayout {

	private final Menu menu;

	private final transient AccessAnnotationChecker accessAnnotationChecker;

	public MainLayout(AccessAnnotationChecker accessAnnotationChecker) {
		this.accessAnnotationChecker = accessAnnotationChecker;
		setSizeFull();
		setClassName("main-layout");

		menu = new Menu();

		// Add secured Menu
		addSecuredMenu(ForwardNodeView.class, ForwardNodeView.VIEW_NAME, VaadinIcon.COG_O.create());
		addSecuredMenu(ProfileView.class, ProfileView.VIEW_NAME, VaadinIcon.CLIPBOARD_TEXT.create());
		addSecuredMenu(ProjectView.class, ProjectView.VIEW_NAME, VaadinIcon.FOLDER_OPEN_O.create());
		addSecuredMenu(ExternalIDView.class, ExternalIDView.VIEW_NAME, VaadinIcon.CLIPBOARD_USER.create());
		addSecuredMenu(MonitoringView.class, MonitoringView.VIEW_NAME, VaadinIcon.PIE_BAR_CHART.create());
		addSecuredMenu(DicomMainView.class, DicomMainView.VIEW_NAME, VaadinIcon.TOOLS.create());
		addSecuredMenu(AuthConfigView.class, AuthConfigView.VIEW_NAME, VaadinIcon.LOCK.create());
		addSecuredMenu(HelpView.class, HelpView.VIEW_NAME, VaadinIcon.QUESTION_CIRCLE.create());

		// Add menu to the layout
		add(menu);
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		attachEvent.getUI().addShortcutListener(SecurityUtil::signOut, Key.KEY_L, KeyModifier.CONTROL);
	}

	/**
	 * Build and add secured menus
	 * @param securedClass View to secure
	 * @param viewName Name of the view
	 * @param icon Icon to apply to the menu
	 */
	private void addSecuredMenu(Class<? extends Component> securedClass, String viewName, Icon icon) {
		// Only show the menu entry if the current user may access the target view, based
		// on its security annotations (@RolesAllowed / @PermitAll / @AnonymousAllowed).
		if (accessAnnotationChecker.hasAccess(securedClass)) {
			icon.getStyle().setMargin("2%");
			menu.addView(securedClass, viewName, icon);
		}
	}

}
