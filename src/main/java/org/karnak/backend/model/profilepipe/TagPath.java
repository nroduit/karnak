/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profilepipe;

import java.util.Arrays;
import org.dcm4che3.util.TagUtils;

/**
 * Where the attribute being visited sits in the object: the sequence tags enclosing it,
 * from the top-level dataset inwards. The visited tag itself is not part of it — it is
 * appended by {@link #locationOf(int)} when a pattern has to be matched.
 *
 * <p>
 * The profile pipeline walks a dataset depth first and descends one level per sequence
 * ({@link org.karnak.backend.service.profilepipe.Profile#applyAction}), so an instance of
 * this class is what tells a profile item whether the tag it is asked about is at the
 * root of the object or nested in a given sequence.
 *
 * @see TagPathPattern
 */
public final class TagPath {

	/** The top-level dataset: no enclosing sequence. */
	public static final TagPath ROOT = new TagPath(new int[0]);

	private final int[] ancestors;

	private TagPath(int[] ancestors) {
		this.ancestors = ancestors;
	}

	/**
	 * Returns the path of the items of {@code sequenceTag}, one level below this one.
	 * @param sequenceTag tag of the sequence being entered
	 * @return a new path, this one is left untouched
	 */
	public TagPath descend(int sequenceTag) {
		int[] descended = Arrays.copyOf(ancestors, ancestors.length + 1);
		descended[ancestors.length] = sequenceTag;
		return new TagPath(descended);
	}

	/** The number of sequences enclosing the visited tag, {@code 0} at the root. */
	public int depth() {
		return ancestors.length;
	}

	public boolean isRoot() {
		return ancestors.length == 0;
	}

	/**
	 * The full location of {@code tag}: the enclosing sequences followed by {@code tag}
	 * itself. This is what {@link TagPathPattern#matches(int[])} is matched against.
	 * @param tag tag being visited
	 * @return the enclosing sequence tags followed by {@code tag}
	 */
	public int[] locationOf(int tag) {
		return descend(tag).ancestors;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof TagPath other && Arrays.equals(ancestors, other.ancestors);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(ancestors);
	}

	@Override
	public String toString() {
		if (ancestors.length == 0) {
			return ".";
		}
		StringBuilder sb = new StringBuilder();
		for (int ancestor : ancestors) {
			sb.append(TagUtils.toString(ancestor)).append('.');
		}
		return sb.toString();
	}

}
