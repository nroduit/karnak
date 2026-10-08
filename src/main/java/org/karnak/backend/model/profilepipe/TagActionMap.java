/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profilepipe;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.dcm4che3.util.TagUtils;
import org.jspecify.annotations.Nullable;
import org.karnak.backend.model.action.ActionItem;
import org.weasis.core.util.StringUtil;

/**
 * Resolves the action a profile item configured for a set of tags must apply to the tag
 * being visited.
 *
 * <p>
 * Three kinds of entry can be registered, and {@link #get(Integer, TagPath)} tries them
 * in that order:
 * <ol>
 * <li>a <b>path</b> ({@code (0040,0275).(0040,0007)}), which also constrains the
 * sequences the tag must be nested in — see {@link TagPathPattern}. The most specific
 * matching path wins;</li>
 * <li>an <b>exact tag</b> ({@code (0010,0010)}), matched at any depth;</li>
 * <li>a <b>wildcard tag</b> ({@code 0010XXXX}), matched at any depth.</li>
 * </ol>
 * A path therefore takes precedence over the flat entries, which keep matching wherever
 * the tag appears.
 */
public class TagActionMap {

	private static final Pattern TAG_SEPARATORS = Pattern.compile("[(),\\s]");

	/** A tag pattern resolved to its matching tag value and bit mask. */
	private record PatternAction(int tag, int mask, ActionItem action) {
	}

	/** A path pattern and the action to apply where it matches. */
	private record PathAction(TagPathPattern pattern, ActionItem action) {
	}

	private final Map<Integer, ActionItem> tagAction = new HashMap<>();

	private final Map<String, PatternAction> tagPatternAction = new HashMap<>();

	private final Map<String, PathAction> tagPathAction = new HashMap<>();

	public static boolean isValidPattern(String tagPattern) {
		if (!StringUtil.hasText(tagPattern) || tagPattern.length() != 8) {
			return false;
		}
		String p = tagPattern.toUpperCase();
		return p.matches("[0-9A-FX]+") && p.contains("X");
	}

	public static String getMask(String tagPattern) {
		char[] chars = tagPattern.toUpperCase().toCharArray();
		for (int i = 0; i < chars.length; i++) {
			if (chars[i] == 'X') {
				chars[i] = '0';
			}
			else {
				chars[i] = 'F';
			}
		}
		return new String(chars);
	}

	/**
	 * Registers the action for a tag, a wildcard tag pattern or a path.
	 * @param tag the configured tag value
	 * @param action the action to apply where it matches
	 * @throws IllegalArgumentException when {@code tag} is a malformed path
	 */
	public void put(String tag, ActionItem action) {
		if (TagPathPattern.isPath(tag)) {
			this.tagPathAction.put(tag.trim().toUpperCase(), new PathAction(TagPathPattern.parse(tag), action));
			return;
		}
		String cleanTag = TAG_SEPARATORS.matcher(tag).replaceAll("").toUpperCase();
		if (isValidPattern(cleanTag)) {
			int patternTag = TagUtils.intFromHexString(cleanTag.replace("X", "0"));
			int patternMask = TagUtils.intFromHexString(getMask(cleanTag));
			this.tagPatternAction.put(cleanTag, new PatternAction(patternTag, patternMask, action));
		}
		else {
			this.tagAction.put(TagUtils.intFromHexString(cleanTag), action);
		}
	}

	/**
	 * Resolves the action without knowing where the tag sits. Path entries never match:
	 * callers that walk sequences must use {@link #get(Integer, TagPath)} instead.
	 * @param tag tag being visited
	 * @return the action to apply, or {@code null}
	 */
	public @Nullable ActionItem get(Integer tag) {
		return get(tag, null);
	}

	/**
	 * Resolves the action for the tag being visited at {@code path}.
	 * @param tag tag being visited
	 * @param path sequences enclosing {@code tag}, {@code null} when the location is
	 * unknown, in which case path entries are skipped
	 * @return the action to apply, or {@code null}
	 */
	public @Nullable ActionItem get(Integer tag, @Nullable TagPath path) {
		ActionItem pathMatch = getByPath(tag, path);
		if (pathMatch != null) {
			return pathMatch;
		}
		ActionItem action = this.tagAction.get(tag);
		if (action == null) {
			for (PatternAction pattern : this.tagPatternAction.values()) {
				if ((tag & pattern.mask()) == pattern.tag()) {
					return pattern.action();
				}
			}
		}
		return action;
	}

	private @Nullable ActionItem getByPath(Integer tag, @Nullable TagPath path) {
		if (path == null || this.tagPathAction.isEmpty()) {
			return null;
		}
		int[] location = path.locationOf(tag);
		ActionItem best = null;
		int bestSpecificity = -1;
		for (PathAction candidate : this.tagPathAction.values()) {
			int specificity = candidate.pattern().specificity();
			if (specificity > bestSpecificity && candidate.pattern().matches(location)) {
				best = candidate.action();
				bestSpecificity = specificity;
			}
		}
		return best;
	}

	public int size() {
		return this.tagAction.size() + this.tagPatternAction.size() + this.tagPathAction.size();
	}

	public boolean isEmpty() {
		return this.tagAction.isEmpty() && this.tagPatternAction.isEmpty() && this.tagPathAction.isEmpty();
	}

}
