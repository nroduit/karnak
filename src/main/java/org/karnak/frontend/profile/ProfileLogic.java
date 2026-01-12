/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.profile;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.provider.ListDataProvider;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.backend.model.profilebody.ProfilePipeBody;
import org.karnak.backend.service.profilepipe.ProfilePipeService;
import org.karnak.frontend.profile.component.errorprofile.ProfileError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

@Service
@Slf4j
public class ProfileLogic extends ListDataProvider<ProfileEntity> {

	@Getter
	@Setter
	private ProfileView profileView;

	// services
	private final transient ProfilePipeService profilePipeService;

	/**
	 * Autowired constructor
	 * @param profilePipeService Profile Pipe Service
	 */
	@Autowired
	public ProfileLogic(final ProfilePipeService profilePipeService) {
		super(new ArrayList<>());
		this.profilePipeService = profilePipeService;
		this.profileView = null;
		initDataProvider();
	}

	@Override
	public void refreshAll() {
		getItems().clear();
		getItems().addAll(profilePipeService.getAllProfiles());
		super.refreshAll();
	}

	/**
	 * Initialize the data provider
	 */
	private void initDataProvider() {
		getItems().addAll(profilePipeService.getAllProfiles());
	}

	public Long enter(String dataIdStr) {
		try {
			return Long.valueOf(dataIdStr);
		}
		catch (NumberFormatException e) {
			log.error("Cannot get valueOf {}", dataIdStr, e);
		}
		return null;
	}

	/**
	 * Retrieve a profile depending of its id
	 * @param profileID Id of the profile to retrieve
	 * @return Project found
	 */
	public ProfileEntity retrieveProfile(Long profileID) {
		refreshAll();
		return getItems().stream().filter(project -> project.getId().equals(profileID)).findAny().orElse(null);
	}

	public void deleteProfile(ProfileEntity profileEntity) {
		profilePipeService.deleteProfile(profileEntity);
		profileView.remove(profileView.getProfileHorizontalLayout());
		refreshAll();
	}

	public ProfileEntity updateProfile(ProfileEntity profileEntity) {
		final ProfileEntity profileUpdate = profilePipeService.updateProfile(profileEntity);
		refreshAll();
		profileView.getProfileGrid().selectRow(profileUpdate);
		return profileEntity;
	}

	private ProfilePipeBody readProfileYaml(InputStream stream) {
		final Yaml yaml = new Yaml(new Constructor(ProfilePipeBody.class, new LoaderOptions()));
		return yaml.load(stream);
	}

	public void setProfileComponent(InputStream stream) {
		try {
			ProfilePipeBody profilePipe = readProfileYaml(stream);
			List<ProfileError> profileErrors = profilePipeService.validateProfile(profilePipe);
			Predicate<ProfileError> errorPredicate = profileError -> profileError.getError() != null;
			if (profileErrors.stream().noneMatch(errorPredicate)) {
				final ProfileEntity newProfileEntity = profilePipeService.saveProfilePipe(profilePipe, false);
				profileView.getProfileErrorView().removeAll();
				profileView.getProfileGrid().selectRow(newProfileEntity);
				profileView.getProfileComponent().setProfile(newProfileEntity);
				profileView.getProfileElementMainView().setProfile(newProfileEntity);
			}
			else {
				profileView.getProfileGrid().deselectAll();
				profileView.getProfileErrorView().setView(profileErrors);
				profileView.remove(profileView.getProfileHorizontalLayout());
				profileView.add(profileView.getProfileErrorView());
			}
			if (profilePipe.getDefaultIssuerOfPatientID() != null) {
				openWarningIssuerDialog();
			}
		}
		catch (YAMLException e) {
			log.error("Unable to read uploaded YAML", e);
			profileView.getProfileErrorView()
				.setView("Unable to read uploaded YAML file.\n"
						+ "Please make sure it is a YAML file and respects the YAML structure.");
		}
	}

	public void openWarningIssuerDialog() {
		var warningIssuer = new Dialog();
		var content = new Div();
		var divTitle = new Div();
		var btn = new Div();
		divTitle.setText("Warning");
		divTitle.getStyle()
			.set("font-size", "large")
			.set("font-weight", "bolder")
			.set("padding-bottom", "10px")
			.set("color", "red");

		var okBtn = new Button("Ok", e -> warningIssuer.close());
		okBtn.getStyle().set("margin-top", "10px");

		var txt = new Text(
				"The Issuer of Patient ID is no longer linked to a profile. Please fill in this field in the destination in the de-identification menu.");

		btn.getStyle().set("text-align", "right");
		btn.add(okBtn);
		content.add(divTitle, txt);
		warningIssuer.add(content, btn);
		warningIssuer.setMaxWidth("30%");
		warningIssuer.open();
	}

}
