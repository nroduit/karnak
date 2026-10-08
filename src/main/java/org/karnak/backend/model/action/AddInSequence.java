/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.action;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.VR;
import org.dcm4che3.util.TagUtils;
import org.karnak.backend.model.profilepipe.HMAC;

/**
 * Adds an attribute inside a sequence rather than at the top level of the dataset.
 *
 * <p>
 * The destination is given as the chain of sequences enclosing the attribute, outermost
 * first, and is resolved from the dataset the action is executed on. A sequence that is
 * missing is created with one item, and an existing but empty one is given an item, so
 * that the attribute can always be written; when a sequence holds several items, every
 * one of them receives the attribute. An item already holding the attribute is left
 * untouched, as {@link Add} does at the top level.
 *
 * <p>
 * Nothing is created when a tag of the chain already exists with a value that is not a
 * sequence: overwriting it would destroy data the profile was not asked to touch.
 */
@Slf4j
public class AddInSequence extends Add {

	private final int[] sequenceTags;

	/**
	 * @param symbol the action symbol, {@code A}
	 * @param sequenceTags the sequences enclosing the attribute, outermost first
	 * @param newTag the tag of the attribute to add
	 * @param vr its value representation
	 * @param dummyValue the value to set, {@code null} to add it empty
	 */
	public AddInSequence(String symbol, int[] sequenceTags, int newTag, VR vr, String dummyValue) {
		super(symbol, newTag, vr, dummyValue);
		this.sequenceTags = sequenceTags.clone();
	}

	@Override
	public void execute(Attributes dcm, int tag, HMAC hmac) {
		if (dcm == null) {
			return;
		}
		if (sequenceTags.length == 0) {
			super.execute(dcm, tag, hmac);
			return;
		}
		addInto(dcm, 0, hmac);
	}

	/**
	 * Descends one level of the chain, creating what is missing, then writes the leaves.
	 */
	private void addInto(Attributes dataset, int level, HMAC hmac) {
		int sequenceTag = sequenceTags[level];
		Sequence sequence = openSequence(dataset, sequenceTag);
		if (sequence == null) {
			return;
		}
		for (Attributes item : sequence) {
			if (level + 1 < sequenceTags.length) {
				addInto(item, level + 1, hmac);
			}
			else {
				super.execute(item, newTag, hmac);
			}
		}
	}

	/**
	 * The sequence of {@code sequenceTag}, created with one item when absent and given an
	 * item when empty. {@code null} when the tag is held by something that is not a
	 * sequence.
	 */
	private Sequence openSequence(Attributes dataset, int sequenceTag) {
		Sequence sequence = dataset.getSequence(sequenceTag);
		if (sequence == null) {
			if (dataset.contains(sequenceTag)) {
				log.warn("Cannot add {} in {}: {} is not a sequence", TagUtils.toString(newTag),
						Arrays.toString(sequenceTags), TagUtils.toString(sequenceTag));
				return null;
			}
			sequence = dataset.newSequence(sequenceTag, 1);
		}
		if (sequence.isEmpty()) {
			sequence.add(new Attributes());
		}
		return sequence;
	}

}