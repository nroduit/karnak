/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.validation;

import java.util.HashSet;
import java.util.Set;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.BulkData;
import org.dcm4che3.data.Fragments;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.util.TagUtils;

/**
 * A metadata-only copy of a DICOM dataset, safe to keep in memory while a study
 * accumulates: bulk values (pixel/overlay/curve/waveform data, large binaries, bulk-data
 * URI references whose backing temp files are short-lived) are replaced by empty values
 * and their tags recorded so presence checks (Type 1) do not raise false positives.
 *
 * <p>
 * Private elements are kept (only their bulk values are stripped like any other) so the
 * conformance validator can check private-block structure (private creator presence).
 *
 * @param metadata the pruned dataset copy
 * @param bulkPresentTags top-level tags whose value was present but stripped
 */
public record MetadataSnapshot(Attributes metadata, Set<Integer> bulkPresentTags) {

	/**
	 * Binary values larger than this are stripped even when not a known bulk tag (e.g.
	 * large LUTs, vendor blobs).
	 */
	private static final int MAX_BINARY_BYTES = 4096;

	/** Default nesting depth sequences are copied down to; deeper items are dropped. */
	public static final int DEFAULT_MAX_SEQUENCE_DEPTH = 3;

	/**
	 * Builds a snapshot copying sequences down to {@link #DEFAULT_MAX_SEQUENCE_DEPTH}.
	 */
	public static MetadataSnapshot of(Attributes source) {
		return of(source, DEFAULT_MAX_SEQUENCE_DEPTH);
	}

	/**
	 * Builds a snapshot copying sequences down to {@code maxSequenceDepth} nesting
	 * levels. The depth must match the recursion depth of
	 * {@code DicomConformanceValidator}, so the deep-sequence-validation option can
	 * validate as deep as the snapshot retained.
	 */
	public static MetadataSnapshot of(Attributes source, int maxSequenceDepth) {
		Set<Integer> bulkTags = new HashSet<>();
		Attributes copy = copyWithoutBulkData(source, bulkTags, 0, maxSequenceDepth);
		return new MetadataSnapshot(copy, Set.copyOf(bulkTags));
	}

	private static Attributes copyWithoutBulkData(Attributes source, Set<Integer> bulkTags, int depth,
	                                              int maxSequenceDepth) {
		Attributes copy = new Attributes(source.bigEndian(), source.size());
		copyInto(source, copy, bulkTags, depth, maxSequenceDepth);
		return copy;
	}

	private static void copyInto(Attributes source, Attributes copy, Set<Integer> bulkTags, int depth,
	                             int maxSequenceDepth) {
		for (int tag : source.tags()) {
			if (TagUtils.isGroupLength(tag)) {
				continue;
			}
			VR vr = source.getVR(tag);
			Object value = source.getValue(tag);
			if (value instanceof Sequence sequence) {
				Sequence copySequence = copy.newSequence(tag, sequence.size());
				if (depth < maxSequenceDepth) {
					for (Attributes item : sequence) {
						Attributes itemCopy = new Attributes(item.bigEndian(), item.size());
						copySequence.add(itemCopy);
						copyInto(item, itemCopy, bulkTags, depth + 1, maxSequenceDepth);
					}
				}
			}
			else if (isBulkValue(tag, value)) {
				if (depth == 0) {
					bulkTags.add(tag);
				}
				copy.setNull(tag, vr);
			}
			else {
				copy.addSelected(source, tag);
			}
		}
	}

	private static boolean isBulkValue(int tag, Object value) {
		return isBulkDataTag(tag) || value instanceof BulkData || value instanceof Fragments
				|| (value instanceof byte[] bytes && bytes.length > MAX_BINARY_BYTES);
	}

	private static boolean isBulkDataTag(int tag) {
		int repeatingGroupMasked = tag & 0xFF00FFFF;
		return tag == Tag.PixelData || tag == Tag.FloatPixelData || tag == Tag.DoubleFloatPixelData
				|| tag == Tag.WaveformData || tag == Tag.EncapsulatedDocument || tag == Tag.SpectroscopyData
				|| repeatingGroupMasked == Tag.OverlayData // (60xx,3000)
				|| repeatingGroupMasked == Tag.CurveData; // (50xx,3000), retired
	}

}
