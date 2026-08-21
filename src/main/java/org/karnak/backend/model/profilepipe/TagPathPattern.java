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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.dcm4che3.util.TagUtils;
import org.weasis.core.util.StringUtil;

/**
 * A tag selector that also constrains <b>where</b> the tag sits: the sequences it must be
 * nested in.
 *
 * <p>
 * A pattern is a dot-separated list of segments, the last one designating the tag itself
 * and the preceding ones the sequences enclosing it:
 *
 * <table border="1">
 * <caption>Pattern forms</caption>
 * <tr>
 * <th>Written as</th>
 * <th>Matches</th>
 * </tr>
 * <tr>
 * <td>{@code (0040,0007)}</td>
 * <td>the tag at any depth, root included — not a path, handled by {@link TagActionMap}
 * alone</td>
 * </tr>
 * <tr>
 * <td>{@code .(0040,0007)}</td>
 * <td>the tag at the root only</td>
 * </tr>
 * <tr>
 * <td>{@code (0040,0275).(0040,0007)}</td>
 * <td>the tag directly inside a {@code (0040,0275)} item, itself at any depth</td>
 * </tr>
 * <tr>
 * <td>{@code .(0040,0275).(0040,0007)}</td>
 * <td>same, but the sequence must be at the root</td>
 * </tr>
 * <tr>
 * <td>{@code *.(0040,0007)}</td>
 * <td>the tag exactly one sequence below its enclosing dataset</td>
 * </tr>
 * <tr>
 * <td>{@code **.(0040,0007)}</td>
 * <td>the tag at any depth except the root</td>
 * </tr>
 * <tr>
 * <td>{@code (0040,0275).*}</td>
 * <td>any tag directly inside a {@code (0040,0275)} item</td>
 * </tr>
 * </table>
 *
 * <p>
 * A pattern that does not start with a dot floats: it is matched against the end of the
 * location, so the sequences it names may themselves be nested anywhere. A leading dot
 * anchors it to the top-level dataset. Every tag segment accepts the {@code X} wildcards
 * already supported for flat tags (see {@link TagActionMap#isValidPattern(String)}), so
 * {@code (0040,0275).0040XXXX} is valid.
 */
public record TagPathPattern(List<Segment> segments, boolean anchored) {

	/** What one dot-separated segment of a pattern matches. */
	public enum Kind {

		/** A tag value, possibly with {@code X} wildcards. */
		TAG,
		/** {@code *}: exactly one level, whatever its tag. */
		SINGLE,
		/** {@code **}: one or more levels, whatever their tags. */
		MULTI

	}

	/**
	 * One segment of a pattern. {@code tag} and {@code mask} are only meaningful for
	 * {@link Kind#TAG}; a tag matches when {@code (candidate & mask) == tag}.
	 */
	public record Segment(Kind kind, int tag, int mask) {
	}

	private static final Pattern TAG_SEPARATORS = Pattern.compile("[(),\\s]");

	private static final Segment SINGLE = new Segment(Kind.SINGLE, 0, 0);

	private static final Segment MULTI = new Segment(Kind.MULTI, 0, 0);

	/**
	 * Whether {@code value} constrains the location of the tag and must therefore be
	 * parsed as a path. Flat tags and flat {@code X} patterns do not.
	 * @param value the configured tag value
	 * @return {@code true} when {@code value} contains a path separator
	 */
	public static boolean isPath(String value) {
		return value != null && value.indexOf('.') >= 0;
	}

	/**
	 * Parses a path pattern.
	 * @param value the configured tag value, containing at least one dot
	 * @return the parsed pattern
	 * @throws IllegalArgumentException when a segment is neither a wildcard nor a valid
	 * tag
	 */
	public static TagPathPattern parse(String value) {
		String trimmed = value == null ? "" : value.trim();
		boolean anchored = trimmed.startsWith(".");
		String body = anchored ? trimmed.substring(1) : trimmed;
		if (body.isEmpty()) {
			throw new IllegalArgumentException("Invalid tag path '" + value + "': no tag given");
		}

		List<Segment> segments = new ArrayList<>();
		// -1 keeps the trailing empty segment of "(0040,0275)." so that it is rejected
		for (String rawSegment : body.split("\\.", -1)) {
			segments.add(parseSegment(rawSegment, value));
		}
		return new TagPathPattern(List.copyOf(segments), anchored);
	}

	private static Segment parseSegment(String rawSegment, String path) {
		String segment = TAG_SEPARATORS.matcher(rawSegment).replaceAll("").toUpperCase();
		if ("*".equals(segment)) {
			return SINGLE;
		}
		if ("**".equals(segment)) {
			return MULTI;
		}
		if (TagActionMap.isValidPattern(segment)) {
			return new Segment(Kind.TAG, TagUtils.intFromHexString(segment.replace('X', '0')),
					TagUtils.intFromHexString(TagActionMap.getMask(segment)));
		}
		if (StringUtil.hasText(segment) && segment.length() == 8 && segment.matches("[0-9A-F]+")) {
			return new Segment(Kind.TAG, TagUtils.intFromHexString(segment), 0xFFFFFFFF);
		}
		throw new IllegalArgumentException(
				"Invalid tag path '" + path + "': '" + rawSegment + "' is neither a tag nor a wildcard");
	}

	/**
	 * Whether {@code value} is a path this class can parse. Non-paths return
	 * {@code false}: use {@link #isPath(String)} first to tell the two apart.
	 * @param value the configured tag value
	 * @return {@code true} when {@code value} is a path and every segment is valid
	 */
	public static boolean isValid(String value) {
		if (!isPath(value)) {
			return false;
		}
		try {
			parse(value);
			return true;
		}
		catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Whether this pattern selects the given location.
	 * @param location enclosing sequence tags followed by the visited tag, as built by
	 * {@link TagPath#locationOf(int)}
	 * @return {@code true} when the location matches
	 */
	public boolean matches(int[] location) {
		if (anchored) {
			return matchFrom(0, location, 0);
		}
		// Floating: the pattern may start at any level, so long as it consumes the
		// visited tag, which is the last element of the location
		for (int start = 0; start < location.length; start++) {
			if (matchFrom(0, location, start)) {
				return true;
			}
		}
		return false;
	}

	private boolean matchFrom(int segmentIndex, int[] location, int levelIndex) {
		if (segmentIndex == segments.size()) {
			return levelIndex == location.length;
		}
		Segment segment = segments.get(segmentIndex);
		return switch (segment.kind()) {
			case TAG -> levelIndex < location.length && (location[levelIndex] & segment.mask()) == segment.tag()
					&& matchFrom(segmentIndex + 1, location, levelIndex + 1);
			case SINGLE -> levelIndex < location.length && matchFrom(segmentIndex + 1, location, levelIndex + 1);
			case MULTI -> matchAtLeastOne(segmentIndex, location, levelIndex);
		};
	}

	private boolean matchAtLeastOne(int segmentIndex, int[] location, int levelIndex) {
		for (int consumed = levelIndex + 1; consumed <= location.length; consumed++) {
			if (matchFrom(segmentIndex + 1, location, consumed)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The tags of this path when every segment names one exactly, outermost first and the
	 * designated tag last.
	 *
	 * <p>
	 * A pattern is only usable as a destination — creating an attribute rather than
	 * matching one — when it is literal: {@code *}, {@code **} and {@code X} wildcards
	 * describe a set of locations, not the single one a value can be written to.
	 * @return the tags of the path, or an empty list when it holds a wildcard
	 */
	public List<Integer> literalTags() {
		List<Integer> tags = new ArrayList<>(segments.size());
		for (Segment segment : segments) {
			if (segment.kind() != Kind.TAG || segment.mask() != 0xFFFFFFFF) {
				return List.of();
			}
			tags.add(segment.tag());
		}
		return List.copyOf(tags);
	}

	/**
	 * How precisely this pattern designates a location, used to pick a winner when
	 * several patterns match the same tag. Named segments weigh more than wildcards, and
	 * an anchored pattern breaks a tie against a floating one.
	 * @return the specificity score, the highest wins
	 */
	public int specificity() {
		int named = (int) segments.stream().filter(s -> s.kind() == Kind.TAG).count();
		return named * 2 + (anchored ? 1 : 0);
	}

}
