/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.profile.component.editprofile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import java.io.ByteArrayInputStream;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.frontend.profile.ProfileLogic;

@Slf4j
public class ProfileComponent extends VerticalLayout {

	private final WarningDeleteProfileUsed dialogWarning;

	private ProfileEntity profileEntity;

	private Anchor download;

	private Button deleteButton;

	private final ProfileLogic profileLogic;

	public ProfileComponent(final ProfileLogic profileLogic) {
		setSizeFull();
		this.profileLogic = profileLogic;
		this.dialogWarning = new WarningDeleteProfileUsed();
	}

	public static String createStreamResource(ProfileEntity profileEntity) {
		try {
			Set<ProfileElementEntity> profileElementEntities = profileEntity.getProfileElementEntities()
				.stream()
				.sorted(Comparator.comparing(ProfileElementEntity::getPosition))
				.collect(Collectors.toCollection(LinkedHashSet::new));
			profileEntity.setProfileElementEntities(profileElementEntities);

			// https://stackoverflow.com/questions/61506368/formatting-yaml-with-jackson
			ObjectMapper mapper = new ObjectMapper(
					new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));

			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(profileEntity);
		}
		catch (final Exception e) {
			log.error("Cannot create the StreamResource for downloading the yaml profile", e);
		}
		return "";
	}

	public void setProfile() {
		removeAll();
		H2 title = new H2("Profile");
		ProfileMetadata name = new ProfileMetadata("Name", profileEntity.getName(), profileEntity.getByDefault());
		name.getValidateEditButton().addClickListener(event -> {
			profileEntity.setName(name.getValue());
			updatedProfilePipes();
		});

		ProfileMetadata version = new ProfileMetadata("Profile version", profileEntity.getVersion(),
				profileEntity.getByDefault());
		version.getValidateEditButton().addClickListener(event -> {
			profileEntity.setVersion(version.getValue());
			updatedProfilePipes();
		});

		ProfileMetadata minVersion = new ProfileMetadata("Min Karnak version", profileEntity.getMinimumKarnakVersion(),
				profileEntity.getByDefault());
		minVersion.getValidateEditButton().addClickListener(event -> {
			profileEntity.setMinimumKarnakVersion(minVersion.getValue());
			updatedProfilePipes();
		});

		createDownloadButton(profileEntity);

		ProfileMasksView profileMasksView = new ProfileMasksView(profileEntity.getMaskEntities());

		if (profileEntity.getByDefault()) {
			add(new HorizontalLayout(title, download), name, version, minVersion, profileMasksView);
		}
		else {
			createDeleteButton(profileEntity);
			add(new HorizontalLayout(title, download, deleteButton), name, version, minVersion, profileMasksView);
		}
	}

	private void updatedProfilePipes() {
		profileEntity = profileLogic.updateProfile(profileEntity);
		String profile = createStreamResource(profileEntity);
		updatedDownloadProfile(profile);
		createDeleteButton(profileEntity);
	}

	public void setEventValidate(ProfileMetadata metadata) {
		metadata.getValidateEditButton().addClickListener(event -> profileEntity.setName(metadata.getValue()));
	}

	public ProfileEntity getProfile() {
		return profileEntity;
	}

	public void setProfile(ProfileEntity profileEntity) {
		this.profileEntity = profileEntity;
		if (profileEntity != null) {
			setProfile();
			setEnabled(true);
		}
		else {
			removeAll();
			setEnabled(false);
		}
	}

	public void createDownloadButton(ProfileEntity profileEntity) {
		String profile = createStreamResource(profileEntity);
		download = new Anchor();
		updatedDownloadProfile(profile);
		download.getElement().setAttribute("download", true);
		download.add(new Button(new Icon(VaadinIcon.DOWNLOAD_ALT)));
	}

	private void updatedDownloadProfile(String profile) {
		download.setHref(DownloadHandler
			.fromInputStream(event -> new DownloadResponse(new ByteArrayInputStream(profile.getBytes()),
					String.format("%s.yml", profileEntity.getName()).replace(" ", "-"), "application/x-yaml", -1)));
	}

	private void createDeleteButton(ProfileEntity profileEntity) {
		deleteButton = new Button((new Icon(VaadinIcon.TRASH)));
		deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
		deleteButton.addClickListener(buttonClickEvent -> {
			if (profileEntity.getProjectEntities() != null && !profileEntity.getProjectEntities().isEmpty()) {
				dialogWarning.setText(profileEntity);
				dialogWarning.open();
			}
			else {
				profileLogic.deleteProfile(profileEntity);
				removeAll();
			}
		});
	}

}
