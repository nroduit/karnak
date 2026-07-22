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

public class TagActionMap {

	private static final Pattern TAG_SEPARATORS = Pattern.compile("[(),\\s]");

	/** A tag pattern resolved to its matching tag value and bit mask. */
	private record PatternAction(int tag, int mask, ActionItem action) {
	}

	private final Map<Integer, ActionItem> tagAction = new HashMap<>();

	private final Map<String, PatternAction> tagPatternAction = new HashMap<>();

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

	public void put(String tag, ActionItem action) {
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

	public @Nullable ActionItem get(Integer tag) {
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

	public int size() {
		return this.tagAction.size() + this.tagPatternAction.size();
	}

	public boolean isEmpty() {
		return this.tagAction.isEmpty() && this.tagPatternAction.isEmpty();
	}

}
