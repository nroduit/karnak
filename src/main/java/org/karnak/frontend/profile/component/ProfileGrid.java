/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.profile.component;

import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.provider.SortDirection;
import java.util.List;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.frontend.util.CollatorUtils;
import org.karnak.frontend.util.GroupTreeGrid;
import org.weasis.core.util.annotations.Generated;

@Generated()
public class ProfileGrid extends GroupTreeGrid<ProfileEntity> {

	public ProfileGrid() {
		var nameColumn = addPrimaryColumn("Name", profile -> new Span(profile.getName()),
				CollatorUtils.comparing(ProfileEntity::getName));
		nameColumn.setFlexGrow(1);
		// Keep the version column narrow so the name column gets most of the space.
		addItemTextColumn("Version", ProfileEntity::getVersion, CollatorUtils.comparing(ProfileEntity::getVersion))
			.setFlexGrow(0)
			.setWidth("5em");

		sort(List.of(new GridSortOrder<>(nameColumn, SortDirection.ASCENDING)));
	}

	public void selectRow(ProfileEntity row) {
		selectItem(row);
	}

}
