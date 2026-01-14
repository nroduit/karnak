/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.forwardnode.edit.source.component;

import com.vaadin.flow.component.grid.Grid;
import org.karnak.backend.data.entity.DicomSourceNodeEntity;

public class GridSourceNode extends Grid<DicomSourceNodeEntity> {

	public GridSourceNode() {
		setSizeFull();

		addColumn(DicomSourceNodeEntity::getAeTitle).setHeader("AET title").setFlexGrow(20).setSortable(true);

		addColumn(DicomSourceNodeEntity::getHostname).setHeader("Hostname").setFlexGrow(20).setSortable(true);

		addColumn(DicomSourceNodeEntity::getDescription).setHeader("Description").setFlexGrow(20).setSortable(true);
	}

}
