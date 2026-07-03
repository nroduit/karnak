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

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import lombok.Getter;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.frontend.profile.ProfileLogic;

/**
 * Right-hand editing area offering two modes for the selected profile: the visual
 * {@link ProfileElementMainView element manager} and a raw {@link ProfileYamlEditor YAML
 * editor}. Both are kept in sync via {@link #setProfile(ProfileEntity)}.
 */
@NullUnmarked
public class ProfileEditorPanel extends VerticalLayout {

	@Getter
	private final ProfileElementMainView elementView;

	@Getter
	private final ProfileYamlEditor yamlEditor;

	private final TabSheet tabSheet = new TabSheet();

	public ProfileEditorPanel(ProfileLogic profileLogic) {
		setSizeFull();
		setPadding(false);
		setSpacing(false);
		// min-height:0 lets this flex child shrink so the tab content scrolls
		// internally instead of forcing a second, page-level scrollbar.
		getStyle().set("min-height", "0");

		elementView = new ProfileElementMainView(profileLogic);
		yamlEditor = new ProfileYamlEditor(profileLogic);

		tabSheet.setSizeFull();
		tabSheet.getStyle().set("min-height", "0");
		tabSheet.add("Profile elements", elementView);
		tabSheet.add("YAML editor", yamlEditor);

		add(tabSheet);
		setFlexGrow(1, tabSheet);
	}

	public void setProfile(ProfileEntity profileEntity) {
		elementView.setProfile(profileEntity);
		yamlEditor.setProfile(profileEntity);
	}

}
