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
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.data.entity.ProfileEntity;

public class ProfileElementMainView extends VerticalLayout {

	private Set<ProfileElementEntity> profilesOrder;

	public ProfileElementMainView() {
	}

	private void profilesView() {
		removeAll();
		add(new HorizontalLayout(new H2("Profile element(s)")));
		for (ProfileElementEntity profileElementEntity : profilesOrder) {
			add(setProfileName((profileElementEntity.getPosition() + 1) + ". " + profileElementEntity.getName()));
			add(new ProfileElementView(profileElementEntity));
		}
	}

	private Div setProfileName(String name) {
		Div profileNameDiv = new Div();
		profileNameDiv.add(new Text(name));
		profileNameDiv.getStyle().set("font-weight", "bold").set("padding-left", "5px");
		return profileNameDiv;
	}

	public void setProfile(ProfileEntity profileEntity) {
		if (profileEntity != null) {
			Set<ProfileElementEntity> profileElementEntities = profileEntity.getProfileElementEntities();

			if (profileElementEntities != null) {
				profilesOrder = profileElementEntities.stream()
					.sorted(Comparator.comparing(ProfileElementEntity::getPosition))
					.collect(Collectors.toCollection(LinkedHashSet::new));
				profilesView();
				setEnabled(true);
			}
			else {
				removeAll();
				setEnabled(false);
			}
		}
		else {
			removeAll();
			setEnabled(false);
		}
	}

}
