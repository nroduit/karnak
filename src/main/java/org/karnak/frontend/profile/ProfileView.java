/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.profile;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import java.io.InputStream;
import lombok.Getter;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.frontend.MainLayout;
import org.karnak.frontend.profile.component.ProfileGrid;
import org.karnak.frontend.profile.component.editprofile.ProfileComponent;
import org.karnak.frontend.profile.component.editprofile.ProfileElementMainView;
import org.karnak.frontend.profile.component.errorprofile.ProfileErrorView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;

@Route(value = ProfileView.ROUTE, layout = MainLayout.class)
@PageTitle("Karnak - Profiles")
@Secured({ "ROLE_admin" })

public class ProfileView extends HorizontalLayout implements HasUrlParameter<String> {

	public static final String VIEW_NAME = "Profiles";

	public static final String ROUTE = "profile";

	private final ProfileLogic profileLogic;

	@Getter
	private final ProfileComponent profileComponent;

	@Getter
	private final ProfileElementMainView profileElementMainView;

	@Getter
	private final ProfileGrid profileGrid;

	@Getter
	private final ProfileErrorView profileErrorView;

	private VerticalLayout barAndGridLayout;

	@Getter
	private final HorizontalLayout profileHorizontalLayout;

	private Upload uploadProfile;

	private UI ui;

	@Autowired
	public ProfileView(final ProfileLogic profileLogic) {
		this.profileLogic = profileLogic;
		this.profileLogic.setProfileView(this);

		profileGrid = new ProfileGrid();
		profileComponent = new ProfileComponent(profileLogic);
		profileElementMainView = new ProfileElementMainView();
		profileErrorView = new ProfileErrorView();
		profileHorizontalLayout = new HorizontalLayout(profileComponent, profileElementMainView);

		initComponents();
		buildLayout();

		add(barAndGridLayout, profileHorizontalLayout);

		addEventGridSelection();
		addAttachListener(event -> this.ui = event.getUI());
	}

	@Override
	public void setParameter(BeforeEvent beforeEvent, @OptionalParameter String parameter) {
		ProfileEntity currentProfileEntity = null;
		profileLogic.refreshAll();
		if (parameter != null) {
			final Long idProfilePipe = profileLogic.enter(parameter);
			if (idProfilePipe != null) {
				currentProfileEntity = profileLogic.retrieveProfile(idProfilePipe);
			}
			remove(profileErrorView);
			add(profileHorizontalLayout);
		}
		profileGrid.selectRow(currentProfileEntity);
		profileComponent.setProfile(currentProfileEntity);
		profileElementMainView.setProfile(currentProfileEntity);
	}

	private void buildLayout() {
		setSizeFull();
		profileComponent.setWidth("45%");
		profileElementMainView.setWidth("55%");
		profileErrorView.setWidth("75%");
		profileHorizontalLayout.setWidth("75%");
		profileHorizontalLayout.getStyle().set("overflow-y", "auto");

		barAndGridLayout = new VerticalLayout();
		barAndGridLayout.add(uploadProfile);
		barAndGridLayout.add(profileGrid);
		barAndGridLayout.setFlexGrow(0, uploadProfile);
		barAndGridLayout.setFlexGrow(1, profileGrid);
		barAndGridLayout.setWidth("25%");
	}

	private void initComponents() {
		initUploadProfile();
		profileGrid.setItems(profileLogic);
	}

	private void initUploadProfile() {
		uploadProfile = new Upload((UploadHandler) upload -> {
			InputStream inputStream = upload.getInputStream();
			if (ui != null) {
				ui.access(() -> profileLogic.setProfileComponent(inputStream));
			}
		});
		uploadProfile.setDropLabel(new Span("Drag and drop your profile here"));
	}

	private void addEventGridSelection() {
		profileGrid.asSingleSelect().addValueChangeListener(event -> navigateProfile(event.getValue()));
	}

	/**
	 * Navigation to the profile in parameter
	 * @param profileEntity Profile to navigate to
	 */
	public void navigateProfile(ProfileEntity profileEntity) {
		if (ui != null) {
			ui.access(() -> {
				if (profileEntity == null) {
					ui.navigate(ProfileView.class, "");
				}
				else {
					String profileID = String.valueOf(profileEntity.getId());
					ui.navigate(ProfileView.class, profileID);
				}
			});
		}
	}

}
