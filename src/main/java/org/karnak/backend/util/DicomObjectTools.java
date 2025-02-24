/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.ElementDictionary;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.VR;
import org.dcm4che3.util.StringUtils;
import org.weasis.core.util.StringUtil;

public class DicomObjectTools {

	public static boolean dicomObjectEquals(Attributes o1, Attributes o2) {
		return Objects.equals(o1, o2);
	}

	public static boolean containsTagFromPath(String path, Attributes dcm) {
		if (!StringUtil.hasText(path)) {
			throw new IllegalArgumentException("path cannot be empty!");
		}

		List<Integer> tags = toTags(StringUtils.split(path, '.'));
		int size = tags.size();
		if (size == 1) {
			return dcm.contains(tags.get(0));
		}
		else if (size > 1) {

		}

		return false;
	}

	public static List<Integer> toTags(String[] tagOrKeywords) {
		List<Integer> tags = new ArrayList<>(tagOrKeywords.length);
		for (int i = 0; i < tagOrKeywords.length; i++) {
			tags.add(toTag(tagOrKeywords[i]));
		}
		return tags;
	}

	public static int toTag(String tagOrKeyword) {
		try {
			return Integer.parseInt(tagOrKeyword, 16);
		}
		catch (IllegalArgumentException e) {
			int tag = ElementDictionary.tagForKeyword(tagOrKeyword, null);
			if (tag == -1) {
				throw new IllegalArgumentException(tagOrKeyword);
			}
			return tag;
		}
	}

	public static boolean containsTagInPath(List<Integer> path, Attributes dcm) {
		int size = path.size();
		if (size == 1) {
			return dcm.contains(path.get(0));
		}

		List<Attributes> list = new ArrayList<>();

		for (int i = 0; i < size; i++) {
			int tag = path.get(i);
			if (i == size - 1) {
				for (Attributes item : list) {
					if (item.contains(tag)) {
						return true;
					}
				}
				return false;
			}

			if (list.isEmpty()) {
				list = dcm.getSequence(tag);
			}
			else {
				list = getChildSequences(list, tag);
			}

			if (list == null || list.isEmpty()) {
				return false;
			}
		}

		return false;
	}

	private static List<Attributes> getChildSequences(List<Attributes> seq, int seqTag) {
		if (seq != null) {
			List<Attributes> items = new ArrayList<>();
			for (Attributes item : seq) {
				Sequence childSeq = item.getSequence(seqTag);
				if (childSeq != null) {
					items.addAll(childSeq);
				}
			}
			return items;
		}
		return null;
	}

	public static boolean containsTagInAllAttributes(int tag, Attributes dcm) {
		if (dcm.contains(tag)) {
			return true;
		}
		for (int t : dcm.tags()) {
			if (dcm.getVR(t) == VR.SQ) {
				Sequence seq = dcm.getSequence(t);
				if (seq != null) {
					for (Attributes item : seq) {
						boolean result = containsTagInAllAttributes(tag, item);
						if (result) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

}
