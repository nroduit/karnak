/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.forwardnode.edit.component;

import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import org.weasis.core.util.annotations.Generated;

@Generated()
public class TabSourcesDestination extends Tabs {

	public String LABEL_SOURCES = "Sources";

	public String LABEL_DESTINATIONS = "Destinations";

	public TabSourcesDestination() {
		Tab sourcesTab = new Tab(LABEL_SOURCES);
		Tab destinationsTab = new Tab(LABEL_DESTINATIONS);
		add(sourcesTab, destinationsTab);
		setSelectedTab(destinationsTab);
	}

}
