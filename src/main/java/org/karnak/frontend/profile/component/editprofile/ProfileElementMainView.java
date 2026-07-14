/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.profile.component.editprofile;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.backend.enums.ProfileItemType;
import org.karnak.frontend.component.ButtonFactory;
import org.karnak.frontend.component.ConfirmDialog;
import org.karnak.frontend.component.NewItemDialog;
import org.karnak.frontend.profile.ProfileLogic;

@NullUnmarked
public class ProfileElementMainView extends VerticalLayout {

	private final transient ProfileLogic profileLogic;

	private ProfileEntity profileEntity;

	private final WarningDeleteProfileUsed dialogWarning = new WarningDeleteProfileUsed();

	private final Grid<ProfileElementEntity> grid = new Grid<>(ProfileElementEntity.class, false);

	public ProfileElementMainView(ProfileLogic profileLogic) {
		this.profileLogic = profileLogic;
		setSizeFull();
		getStyle().set("min-height", "0");
		configureGrid();
	}

	private void configureGrid() {
		grid.addColumn(e -> e.getPosition() + 1).setHeader("#").setWidth("60px").setFlexGrow(0);
		grid.addColumn(ProfileElementEntity::getName).setHeader("Name").setFlexGrow(2).setWidth("120px");
		grid.addColumn(ProfileElementEntity::getCodename).setHeader("Type").setFlexGrow(2).setWidth("120px");
		grid.addColumn(this::summary).setHeader("Summary").setFlexGrow(3).setWidth("150px");
		grid.addComponentColumn(this::rowActions).setHeader("").setAutoWidth(true).setFlexGrow(0);
		grid.setSizeFull();
		// Floor the grid height: it flex-shrinks down to this height before scrolling.
		grid.setMinHeight("200px");
	}

	public void setProfile(ProfileEntity profileEntity) {
		this.profileEntity = profileEntity;
		removeAll();
		if (profileEntity == null) {
			setEnabled(false);
			return;
		}
		setEnabled(true);
		if (Boolean.TRUE.equals(profileEntity.getByDefault())) {
			renderReadOnly();
		}
		else {
			renderEditable();
		}
	}

	/** Read-only rendering kept for the built-in (default) profile. */
	private void renderReadOnly() {
		add(buildHeader(false));
		add(metadataInfo());
		for (ProfileElementEntity profileElementEntity : orderedElements()) {
			add(setProfileName((profileElementEntity.getPosition() + 1) + ". " + profileElementEntity.getName()));
			add(new ProfileElementView(profileElementEntity));
		}
	}

	private void renderEditable() {
		add(buildHeader(true));
		add(metadataInfo());
		add(orderInfoMessage());
		if (isBasicProfileMissing()) {
			add(basicProfileMissingWarning());
		}
		Button addButton = ButtonFactory.createAddButton("Add element");
		addButton.addClickListener(event -> openEditor(null));
		add(addButton);
		grid.setItems(orderedElements());
		add(grid);
		setFlexGrow(1, grid);
	}

	/**
	 * Header with the profile name and its actions. The metadata (name / version / min
	 * Karnak version) is edited through the same popup as the "New profile" button; the
	 * built-in default profile is read-only so it only offers the download.
	 */
	private HorizontalLayout buildHeader(boolean editable) {
		HorizontalLayout header = new HorizontalLayout();
		header.setAlignItems(FlexComponent.Alignment.CENTER);
		header.setWidthFull();

		String name = profileEntity.getName() != null ? profileEntity.getName() : "Profile";
		header.add(new H2(name));

		if (editable) {
			Button editButton = new Button("Edit metadata", new Icon(VaadinIcon.EDIT));
			editButton.addThemeVariants(ButtonVariant.TERTIARY);
			editButton.addClickListener(event -> openMetadataDialog());

			header.add(editButton, downloadButton(), deleteButton());
		}
		else {
			header.add(downloadButton());
		}
		return header;
	}

	/** Secondary line recalling the profile version and the minimum Karnak version. */
	private Div metadataInfo() {
		String version = profileEntity.getVersion() != null ? profileEntity.getVersion() : "Not defined";
		String minVersion = profileEntity.getMinimumKarnakVersion() != null ? profileEntity.getMinimumKarnakVersion()
				: "Not defined";
		Div message = new Div(new Span("Version: " + version + " · Min Karnak version: " + minVersion));
		message.getStyle()
			.set("color", "var(--vaadin-text-color-secondary)")
			.set("font-size", "var(--aura-font-size-s)")
			.set("margin", "2px 0");
		return message;
	}

	/** Popup, shared with the "New profile" button, to edit the three metadata values. */
	private void openMetadataDialog() {
		TextField name = new TextField("Name");
		TextField version = new TextField("Version");
		TextField minVersion = new TextField("Min Karnak version (optional)");
		name.setValue(profileEntity.getName() != null ? profileEntity.getName() : "");
		version.setValue(profileEntity.getVersion() != null ? profileEntity.getVersion() : "");
		minVersion
			.setValue(profileEntity.getMinimumKarnakVersion() != null ? profileEntity.getMinimumKarnakVersion() : "");
		name.setWidthFull();
		version.setWidthFull();
		minVersion.setWidthFull();
		NewItemDialog dialog = new NewItemDialog("Edit profile", "Save", name, version, minVersion);
		dialog.setOnConfirm(() -> {
			if (name.getValue() == null || name.getValue().isBlank()) {
				name.setInvalid(true);
				name.setErrorMessage("A name is required");
				return false;
			}
			profileLogic.updateProfileMetadata(profileEntity.getId(), name.getValue().trim(), version.getValue(),
					minVersion.getValue());
			return true;
		});
		dialog.open();
	}

	private Anchor downloadButton() {
		String profile = ProfileYamlSerializer.toYaml(profileEntity);
		Anchor download = new Anchor();
		download.setHref(DownloadHandler
			.fromInputStream(event -> new DownloadResponse(new ByteArrayInputStream(profile.getBytes()),
					String.format("%s.yml", profileEntity.getName()).replace(" ", "-"), "application/x-yaml", -1)));
		download.getElement().setAttribute("download", true);
		Button button = new Button(new Icon(VaadinIcon.DOWNLOAD_ALT));
		button.addThemeVariants(ButtonVariant.TERTIARY);
		download.add(button);
		return download;
	}

	private Button deleteButton() {
		Button delete = new Button(new Icon(VaadinIcon.TRASH));
		delete.addThemeVariants(ButtonVariant.ERROR, ButtonVariant.PRIMARY);
		delete.addClickListener(event -> {
			if (profileEntity.getProjectEntities() != null && !profileEntity.getProjectEntities().isEmpty()) {
				dialogWarning.setText(profileEntity);
				dialogWarning.open();
			}
			else {
				profileLogic.deleteProfile(profileEntity);
			}
		});
		return delete;
	}

	private Div orderInfoMessage() {
		Anchor documentation = new Anchor("https://weasis.org/karnak-documentation/en/profiles/",
				"See the documentation");
		documentation.setTarget("_blank");
		Div message = new Div(
				new Span("The order of the profile elements matters: they are applied from top to bottom. "),
				documentation);
		message.getStyle()
			.set("color", "var(--vaadin-text-color-secondary)")
			.set("font-size", "var(--aura-font-size-s)")
			.set("margin", "2px 0");
		return message;
	}

	private boolean isBasicProfileMissing() {
		return orderedElements().stream().noneMatch(e -> ProfileItemType.BASIC_DICOM_ALIAS.equals(e.getCodename()));
	}

	private Div basicProfileMissingWarning() {
		Div warning = new Div(
				new Text("No \"Basic DICOM confidentiality profile\" (" + ProfileItemType.BASIC_DICOM_ALIAS
						+ ") is present. A de-identification profile should include it; it is always applied last."));
		warning.getStyle()
			.set("color", "var(--aura-red-text)")
			.set("background-color", "color-mix(in srgb, var(--aura-red) 10%, transparent)")
			.set("padding", "var(--vaadin-gap-s)")
			.set("border-radius", "var(--vaadin-radius-m)")
			.set("margin", "5px 0");
		return warning;
	}

	private HorizontalLayout rowActions(ProfileElementEntity element) {
		List<ProfileElementEntity> ordered = orderedElements();
		int index = indexOf(ordered, element);
		// The Basic DICOM profile is pinned to the end and cannot be moved or moved past.
		boolean isBasic = ProfileItemType.BASIC_DICOM_ALIAS.equals(element.getCodename());
		boolean nextIsBasic = index >= 0 && index < ordered.size() - 1
				&& ProfileItemType.BASIC_DICOM_ALIAS.equals(ordered.get(index + 1).getCodename());

		Button up = iconButton(VaadinIcon.ARROW_UP, () -> move(element, -1));
		up.setEnabled(index > 0 && !isBasic);
		Button down = iconButton(VaadinIcon.ARROW_DOWN, () -> move(element, 1));
		down.setEnabled(index >= 0 && index < ordered.size() - 1 && !isBasic && !nextIsBasic);

		Button edit = iconButton(VaadinIcon.EDIT, () -> openEditor(element));
		edit.setEnabled(ProfileElementEditor.supports(element.getCodename()));

		Button delete = iconButton(VaadinIcon.TRASH, () -> confirmDelete(element));
		delete.addThemeVariants(ButtonVariant.ERROR);

		return new HorizontalLayout(up, down, edit, delete);
	}

	private Button iconButton(VaadinIcon icon, Runnable action) {
		Button button = new Button(icon.create(), event -> action.run());
		button.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
		return button;
	}

	private void openEditor(ProfileElementEntity element) {
		new ProfileElementEditor(profileLogic.getProfilePipeService(), profileLogic.getDicomStandardService(),
				profileEntity.getId(), element, orderedElements(),
				() -> profileLogic.refreshProfile(profileEntity.getId()))
			.open();
	}

	private void confirmDelete(ProfileElementEntity element) {
		ConfirmDialog dialog = new ConfirmDialog(
				"Delete the element \"" + element.getName() + "\"? This cannot be undone.");
		dialog.addConfirmationListener(event -> profileLogic.deleteElement(profileEntity.getId(), element.getId()));
		dialog.open();
	}

	private void move(ProfileElementEntity element, int delta) {
		List<ProfileElementEntity> ordered = orderedElements();
		int index = indexOf(ordered, element);
		int target = index + delta;
		if (index < 0 || target < 0 || target >= ordered.size()) {
			return;
		}
		Collections.swap(ordered, index, target);
		List<Long> orderedIds = ordered.stream().map(ProfileElementEntity::getId).toList();
		profileLogic.reorderElements(profileEntity.getId(), orderedIds);
	}

	private String summary(ProfileElementEntity element) {
		List<String> parts = new ArrayList<>();
		if (element.getAction() != null) {
			parts.add("action=" + element.getAction());
		}
		if (element.getOption() != null) {
			parts.add("option=" + element.getOption());
		}
		if (!element.getIncludedTagEntities().isEmpty()) {
			parts.add(element.getIncludedTagEntities().size() + " tag(s)");
		}
		if (!element.getExcludedTagEntities().isEmpty()) {
			parts.add(element.getExcludedTagEntities().size() + " excluded");
		}
		if (element.getCondition() != null) {
			parts.add("condition");
		}
		return String.join(", ", parts);
	}

	private List<ProfileElementEntity> orderedElements() {
		if (profileEntity == null || profileEntity.getProfileElementEntities() == null) {
			return new ArrayList<>();
		}
		return profileEntity.getProfileElementEntities()
			.stream()
			.sorted(Comparator.comparing(ProfileElementEntity::getPosition,
					Comparator.nullsLast(Comparator.naturalOrder())))
			.collect(java.util.stream.Collectors.toCollection(ArrayList::new));
	}

	private static int indexOf(List<ProfileElementEntity> elements, ProfileElementEntity element) {
		for (int i = 0; i < elements.size(); i++) {
			if (elements.get(i).getId() != null && elements.get(i).getId().equals(element.getId())) {
				return i;
			}
		}
		return -1;
	}

	private Div setProfileName(String name) {
		Div profileNameDiv = new Div();
		profileNameDiv.add(new Text(name));
		profileNameDiv.getStyle().set("font-weight", "bold").set("padding-left", "5px");
		return profileNameDiv;
	}

}
