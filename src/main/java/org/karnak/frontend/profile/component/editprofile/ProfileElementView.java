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

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.List;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.data.entity.TagEntity;

public class ProfileElementView extends Div {

	ProfileElementEntity profileElementEntity;

	public ProfileElementView(ProfileElementEntity profileElementEntity) {
		this.profileElementEntity = profileElementEntity;
		getStyle().set("margin-top", "0px");
		setView();
	}

	public void setView() {
		removeAll();
		if (profileElementEntity.getCodename() != null) {
			add(setProfileValue("Codename : " + profileElementEntity.getCodename()));
		}
		if (profileElementEntity.getAction() != null) {
			add(setProfileValue("Action : " + profileElementEntity.getAction()));
		}
		if (profileElementEntity.getOption() != null) {
			add(setProfileValue("Option : " + profileElementEntity.getOption()));
		}
		if (profileElementEntity.getArgumentEntities() != null
				&& !profileElementEntity.getArgumentEntities().isEmpty()) {
			add(setProfileValue("Arguments"));
			add(setProfileArguments(profileElementEntity.getArgumentEntities()));
		}
		if (profileElementEntity.getCondition() != null) {
			add(setProfileValue("Condition : " + profileElementEntity.getCondition()));
		}
		if (!profileElementEntity.getIncludedTagEntities().isEmpty()) {
			add(setProfileValue("Tags"));
			add(setProfileTags(profileElementEntity.getIncludedTagEntities()));
		}
		if (!profileElementEntity.getExcludedTagEntities().isEmpty()) {
			add(setProfileValue("Excluded tags"));
			add(setProfileTags(profileElementEntity.getExcludedTagEntities()));
		}
	}

	private Div setProfileValue(String value) {
		Div profileNameDiv = new Div();
		profileNameDiv.add(new Text(value));
		profileNameDiv.getStyle().set("color", "grey").set("padding-left", "10px").set("margin-top", "5px");
		return profileNameDiv;
	}

	private VerticalLayout setProfileTags(List<? extends TagEntity> tagEntities) {
		VerticalLayout verticalLayout = new VerticalLayout();
		verticalLayout.getStyle().set("margin-top", "0px");
		for (TagEntity tagEntity : tagEntities) {
			Div tagDiv = new Div();
			tagDiv.add(new Text(tagEntity.getTagValue()));
			tagDiv.getStyle().set("color", "grey").set("padding-left", "15px").set("margin-top", "2px");
			verticalLayout.add(tagDiv);
		}
		return verticalLayout;
	}

	private VerticalLayout setProfileArguments(List<ArgumentEntity> argumentEntities) {
		VerticalLayout verticalLayout = new VerticalLayout();
		verticalLayout.getStyle().set("margin-top", "0px");
		for (ArgumentEntity argumentEntity : argumentEntities) {
			Div tagDiv = new Div();
			tagDiv.add(new Text(argumentEntity.getArgumentKey() + " : " + argumentEntity.getArgumentValue()));
			tagDiv.getStyle().set("color", "grey").set("padding-left", "15px").set("margin-top", "2px");
			verticalLayout.add(tagDiv);
		}
		return verticalLayout;
	}

}
