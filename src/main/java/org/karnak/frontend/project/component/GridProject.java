/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.project.component;

import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.provider.SortDirection;
import java.util.List;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.frontend.util.CollatorUtils;
import org.karnak.frontend.util.GroupTreeGrid;
import org.weasis.core.util.annotations.Generated;

@Generated()
public class GridProject extends GroupTreeGrid<ProjectEntity> {

	public GridProject() {
		setWidthFull();

		var projectNameColumn = addPrimaryColumn("Project Name", project -> new Span(project.getName()),
				CollatorUtils.comparing(ProjectEntity::getName))
			.setFlexGrow(15);

		addItemTextColumn("De-identification profile",
				project -> String.format("%s [version %s]", project.getProfileEntity().getName(),
						project.getProfileEntity().getVersion()),
				CollatorUtils.comparingThen(p -> CollatorUtils.nullSafe(p.getProfileEntity().getName()),
						p -> CollatorUtils.nullSafe(p.getProfileEntity().getVersion())))
			.setFlexGrow(15);

		// Set by default the order on the name of the column
		sort(List.of(new GridSortOrder<>(projectNameColumn, SortDirection.ASCENDING)));
	}

	public void selectRow(ProjectEntity row) {
		selectItem(row);
	}

}
