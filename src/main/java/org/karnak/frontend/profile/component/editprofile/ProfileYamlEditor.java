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

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import java.util.List;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.frontend.profile.ProfileLogic;

/**
 * Raw-YAML editing mode for a profile: shows the profile serialized as YAML (the same
 * representation as the download / import file) in an editable text area. On save the
 * content is parsed, validated and applied in place, keeping the profile id so
 * referencing projects are preserved. The built-in default profile is shown read-only.
 */
@NullUnmarked
public class ProfileYamlEditor extends VerticalLayout {

	private final transient ProfileLogic profileLogic;

	private ProfileEntity profileEntity;

	private final TextArea yamlArea = new TextArea();

	private final Div errorBox = new Div();

	private final Button saveButton;

	private final Button resetButton;

	public ProfileYamlEditor(ProfileLogic profileLogic) {
		this.profileLogic = profileLogic;
		setSizeFull();
		setPadding(false);
		setSpacing(true);
		// min-height:0 lets the text area flex-shrink and scroll internally instead
		// of adding a second, page-level scrollbar.
		getStyle().set("min-height", "0");

		yamlArea.setSizeFull();
		yamlArea.setWidthFull();
		// Floor the editor height: it flex-shrinks down to this height before the
		// panel scrolls.
		yamlArea.setMinHeight("200px");
		yamlArea.getStyle().set("font-family", "monospace").set("--vaadin-input-field-value-font-size", "13px");

		errorBox.setVisible(false);
		errorBox.getStyle()
			.set("color", "var(--aura-red-text)")
			.set("background-color", "color-mix(in srgb, var(--aura-red) 10%, transparent)")
			.set("padding", "var(--vaadin-gap-s)")
			.set("border-radius", "var(--vaadin-radius-m)")
			.set("margin", "5px 0")
			.set("white-space", "pre-wrap");

		saveButton = new Button("Save", VaadinIcon.CHECK.create(), event -> save());
		saveButton.addThemeVariants(ButtonVariant.PRIMARY);
		resetButton = new Button("Reset", VaadinIcon.REFRESH.create(), event -> reload());

		HorizontalLayout actions = new HorizontalLayout(saveButton, resetButton);
		actions.setWidthFull();
		// Breathing room above and below the action buttons.
		actions.getStyle().set("margin", "var(--vaadin-gap-m) 0");

		add(infoMessage(), errorBox, yamlArea, actions);
		setFlexGrow(1, yamlArea);
	}

	private Div infoMessage() {
		Div message = new Div(new Span(
				"Edit the whole profile as YAML (same format as the import/export file), then Save. Saving replaces the profile content in place."));
		message.getStyle()
			.set("color", "var(--vaadin-text-color-secondary)")
			.set("font-size", "var(--aura-font-size-s)")
			.set("margin", "2px 0");
		return message;
	}

	public void setProfile(ProfileEntity profileEntity) {
		this.profileEntity = profileEntity;
		errorBox.setVisible(false);
		if (profileEntity == null) {
			yamlArea.clear();
			setEnabled(false);
			return;
		}
		setEnabled(true);
		reload();
		boolean editable = !Boolean.TRUE.equals(profileEntity.getByDefault());
		yamlArea.setReadOnly(!editable);
		saveButton.setVisible(editable);
		resetButton.setVisible(editable);
	}

	/** Reload the text area from the persisted profile, discarding unsaved edits. */
	private void reload() {
		errorBox.setVisible(false);
		if (profileEntity != null) {
			yamlArea.setValue(ProfileYamlSerializer.toYaml(profileEntity));
		}
	}

	private void save() {
		if (profileEntity == null) {
			return;
		}
		List<String> errors = profileLogic.saveProfileYaml(profileEntity.getId(), yamlArea.getValue());
		if (errors.isEmpty()) {
			errorBox.setVisible(false);
			Notification notification = Notification.show("Profile saved");
			notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
		}
		else {
			errorBox.removeAll();
			errorBox.add(new Span("The profile could not be saved:"));
			errors.forEach(message -> errorBox.add(new Div(new Span("• " + message))));
			errorBox.setVisible(true);
		}
	}

}