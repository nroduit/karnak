/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.util;

import java.util.List;
import org.karnak.backend.data.entity.NamedGroupEntity;

/**
 * Bridge between a {@link GroupTreeGrid} and the backend for one feature (Profiles,
 * Projects or Forward Nodes). Implementations are the existing {@code *Logic} classes,
 * which already hold the relevant services. Method names avoid clashing with
 * {@code ListDataProvider} (e.g. {@code listItems} instead of {@code getItems}).
 *
 * @param <T> the feature item type
 */
public interface GroupTreeController<T> {

	/** All items, regardless of group (already filtered/sorted as desired). */
	List<T> listItems();

	/** All groups for this feature. */
	List<? extends NamedGroupEntity> listGroups();

	/** The group an item belongs to, or {@code null} when it sits at the root. */
	NamedGroupEntity groupOf(T item);

	/** Stable id of an item, used as the tree-node key. */
	Long itemId(T item);

	/** Create and persist a new group. */
	NamedGroupEntity createGroup(String name);

	/** Rename a group. */
	void renameGroup(NamedGroupEntity group, String name);

	/** Delete a group; its members fall back to the root. */
	void deleteGroup(NamedGroupEntity group);

	/** Assign an item to a group, or to the root when {@code group} is {@code null}. */
	void assign(T item, NamedGroupEntity group);

}
