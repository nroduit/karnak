/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.profile.component;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import java.util.Arrays;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.frontend.util.CollatorUtils;

public class ProfileGrid extends Grid<ProfileEntity> {

	public ProfileGrid() {
		setSelectionMode(SelectionMode.SINGLE);

		Column<ProfileEntity> nameColumn = addColumn(ProfileEntity::getName).setHeader("Name")
			.setSortable(true)
			.setKey("name")
			.setComparator(CollatorUtils.comparing(ProfileEntity::getName));

		Column<ProfileEntity> versionColumn = addColumn(ProfileEntity::getVersion).setHeader("Version")
			.setSortable(true)
			.setKey("version")
			.setComparator(CollatorUtils.comparing(ProfileEntity::getVersion));

		setMultiSort(true);
		sort(Arrays.asList(new GridSortOrder<>(nameColumn, SortDirection.ASCENDING),
				new GridSortOrder<>(versionColumn, SortDirection.ASCENDING)));
	}

	public void selectRow(ProfileEntity row) {
		if (row != null) {
			getSelectionModel().select(row);
		}
		else {
			getSelectionModel().deselectAll();
		}
	}

}
