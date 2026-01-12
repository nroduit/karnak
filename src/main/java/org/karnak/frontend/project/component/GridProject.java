/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.project.component;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import java.util.List;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.frontend.util.CollatorUtils;

public class GridProject extends Grid<ProjectEntity> {

	public GridProject() {
		setWidthFull();

		Column<ProjectEntity> projectNameColumn = addColumn(ProjectEntity::getName).setHeader("Project Name")
			.setFlexGrow(15)
			.setSortable(true)
			.setComparator(CollatorUtils.comparing(ProjectEntity::getName));

		addColumn(project -> String.format("%s [version %s]", project.getProfileEntity().getName(),
				project.getProfileEntity().getVersion()))
			.setHeader("De-identification profile")
			.setFlexGrow(15)
			.setSortable(true)
			.setComparator(CollatorUtils.comparingThen(p -> CollatorUtils.nullSafe(p.getProfileEntity().getName()),
					p -> CollatorUtils.nullSafe(p.getProfileEntity().getVersion())));

		// Set by default the order on the name of the column
		GridSortOrder<ProjectEntity> order = new GridSortOrder<>(projectNameColumn, SortDirection.ASCENDING);
		sort(List.of(order));
	}

	public void selectRow(ProjectEntity row) {
		if (row != null) {
			getSelectionModel().select(row);
		}
		else {
			getSelectionModel().deselectAll();
		}
	}

}
