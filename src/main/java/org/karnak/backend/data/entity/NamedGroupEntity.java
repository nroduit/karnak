/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.data.entity;

/**
 * Common contract for the optional one-level grouping applied to Profiles, Projects and
 * Forward Nodes. A group is purely an organizational label: it has an id and a name and
 * carries no behavior. Implementing this interface lets the shared {@code GroupTreeGrid}
 * render and rename any group regardless of the feature it belongs to.
 */
public interface NamedGroupEntity {

	Long getId();

	String getName();

	void setName(String name);

}
