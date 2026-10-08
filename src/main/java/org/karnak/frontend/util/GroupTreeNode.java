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

import java.util.Objects;
import org.karnak.backend.data.entity.NamedGroupEntity;

/**
 * Row model for a {@link GroupTreeGrid}. A row is either a {@link GroupNode} (an
 * expandable group header) or an {@link ItemNode} (a feature item, either nested under a
 * group or at the root). Identity is based on a stable {@link #key()} so the grid can
 * track rows across rebuilds and refreshes regardless of the wrapped bean's own
 * {@code equals}.
 *
 * @param <T> the feature item type (e.g. {@code ProfileEntity})
 */
public sealed interface GroupTreeNode<T> {

	String key();

	default boolean isGroup() {
		return this instanceof GroupNode<T>;
	}

	/** A group header row. */
	final class GroupNode<T> implements GroupTreeNode<T> {

		private final NamedGroupEntity group;

		public GroupNode(NamedGroupEntity group) {
			this.group = group;
		}

		public NamedGroupEntity group() {
			return group;
		}

		@Override
		public String key() {
			return "g:" + group.getId();
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof GroupNode<?> other && Objects.equals(key(), other.key());
		}

		@Override
		public int hashCode() {
			return key().hashCode();
		}

	}

	/** An item row (a profile, project or forward node). */
	final class ItemNode<T> implements GroupTreeNode<T> {

		private final T item;

		private final Long id;

		public ItemNode(T item, Long id) {
			this.item = item;
			this.id = id;
		}

		public T item() {
			return item;
		}

		@Override
		public String key() {
			return "i:" + id;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof ItemNode<?> other && Objects.equals(key(), other.key());
		}

		@Override
		public int hashCode() {
			return key().hashCode();
		}

	}

}
