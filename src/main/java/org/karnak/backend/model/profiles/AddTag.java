/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profiles;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.util.TagUtils;
import org.jspecify.annotations.Nullable;
import org.karnak.backend.config.AppConfig;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.exception.ProfileException;
import org.karnak.backend.model.action.ActionItem;
import org.karnak.backend.model.action.Add;
import org.karnak.backend.model.action.AddInSequence;
import org.karnak.backend.model.action.Keep;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.model.profilepipe.TagActionMap;
import org.karnak.backend.model.profilepipe.TagPath;
import org.karnak.backend.model.profilepipe.TagPathPattern;
import org.karnak.backend.model.standard.AttributeDetail;
import org.karnak.backend.model.standard.StandardDICOM;

/**
 * Adds an attribute to the object, once per instance.
 *
 * <p>
 * The configured tag may be a plain tag, added to the top-level dataset, or a literal tag
 * path such as {@code (0040,0275).(0040,0009)}, added inside the named sequences — see
 * {@link AddInSequence} for how a missing sequence is created. A path is always read from
 * the root of the dataset, since the attribute has to be written somewhere definite, and
 * so may not hold the {@code *}, {@code **} or {@code X} wildcards that describe a set of
 * locations. Whichever form is used, the destination must be part of the SOP class of the
 * instance, otherwise nothing is added.
 */
@Slf4j
public class AddTag extends AbstractProfileItem {

	private boolean tagAdded;

	private String currentInstanceUID;

	private final StandardDICOM standardDICOM;

	/** The sequences enclosing the attribute, outermost first, empty at the top level. */
	private final int[] sequenceTags;

	/** The tag of the attribute to add. */
	private final int leafTag;

	/**
	 * The destination as the DICOM standard writes it: the colon-separated hexadecimal
	 * path used to look the attribute up in the modules of a SOP class.
	 */
	private final String standardTagPath;

	private static final String LOG_PATTERN = "SOPInstanceUID={} TAG={} ACTION={} REASON={}";

	public AddTag(ProfileElementEntity profileElementEntity) throws ProfileException {
		super(profileElementEntity);
		standardDICOM = AppConfig.getInstance().getStandardDICOM();

		TagActionMap tagsAction = new TagActionMap();
		ActionItem actionByDefault = new Keep("K");
		profileValidation();
		mapTagsToAction(tagsAction, null, actionByDefault);

		List<Integer> destination = destinationTags(tagEntities.getFirst().getTagValue());
		this.sequenceTags = destination.subList(0, destination.size() - 1)
			.stream()
			.mapToInt(Integer::intValue)
			.toArray();
		this.leafTag = destination.getLast();
		this.standardTagPath = destination.stream().map(TagUtils::toHexString).collect(Collectors.joining(":"));
		this.currentInstanceUID = "";
		this.tagAdded = false;
	}

	/**
	 * The tags of the configured destination, the enclosing sequences followed by the
	 * attribute itself. A plain tag yields a single element.
	 * @throws ProfileException when the value is neither a tag nor a literal path
	 */
	private static List<Integer> destinationTags(String tagValue) throws ProfileException {
		if (!TagPathPattern.isPath(tagValue)) {
			return List.of(TagUtils.intFromHexString(StandardDICOM.cleanTagPath(tagValue)));
		}
		List<Integer> tags = TagPathPattern.parse(tagValue).literalTags();
		if (tags.isEmpty()) {
			throw new ProfileException(
					"Cannot build the profile: the tag path " + tagValue + " must name every sequence exactly, "
							+ "a wildcard does not designate where the tag would be added");
		}
		return tags;
	}

	/**
	 * Whether this item writes inside a sequence, in which case the pipeline applies it
	 * after having walked the object rather than while visiting its tags.
	 */
	public boolean targetsSequence() {
		return sequenceTags.length > 0;
	}

	/** The tag of the attribute this item adds, whatever the sequences enclosing it. */
	public int getTargetTag() {
		return leafTag;
	}

	/**
	 * The action adding the attribute inside its sequences, or {@code null} when there is
	 * nothing to add — the destination is not part of the SOP class of the instance, or
	 * the attribute was already added for it.
	 *
	 * <p>
	 * This is how the pipeline applies an item targeting a sequence: the object has been
	 * walked by then, so the attribute is written where the profile asked and is not
	 * itself de-identified afterwards, as an attribute added at the top level is not.
	 * @param dcm dataset being de-identified, at the top level
	 * @param original untouched copy of {@code dcm}
	 * @param hmac hash context of the current patient
	 * @return the action to execute on {@code dcm}, or {@code null}
	 */
	public @Nullable ActionItem getSequenceAction(Attributes dcm, Attributes original, HMAC hmac) {
		return getAction(dcm, original, leafTag, hmac, TagPath.ROOT);
	}

	@Override
	public @Nullable ActionItem getAction(Attributes dcm, Attributes original, int tag, HMAC hmac) {
		return getAction(dcm, original, tag, hmac, TagPath.ROOT);
	}

	@Override
	public @Nullable ActionItem getAction(Attributes dcm, Attributes original, int tag, HMAC hmac, TagPath path) {
		// The attribute is added once, from the top-level dataset: the returned action
		// resolves the destination itself, and a sequence item carrying its own
		// SOPInstanceUID would otherwise look like a second instance to the latch below
		if (!path.isRoot()) {
			return null;
		}
		// Read from the untouched copy: the pipeline replaces (0008,0018) early in its
		// ascending walk, so the working dataset would answer with the original UID for
		// the
		// first tags and with the pseudonymized one afterwards, which this latch would
		// read
		// as a second instance
		String currentUID = original.getString(Tag.SOPInstanceUID);
		if (!currentInstanceUID.equals(currentUID) && currentUID != null) {
			currentInstanceUID = currentUID;
			tagAdded = false;
		}
		if (!tagAdded) {
			if (!standardDICOM.getAttributesBySOP(original.getString(Tag.SOPClassUID), standardTagPath).isEmpty()) {

				String value = "";
				@Nullable AttributeDetail detail = standardDICOM.getAttributeDetail(TagUtils.toHexString(leafTag));
				if (detail == null) {
					tagAdded = true;
					return null;
				}
				VR vr = VR.valueOf(detail.valueRepresentation());
				for (ArgumentEntity ae : argumentEntities) {
					if ("value".equals(ae.getArgumentKey())) {
						value = ae.getArgumentValue();
					}
				}
				tagAdded = true;
				return targetsSequence() ? new AddInSequence("A", sequenceTags, leafTag, vr, value)
						: new Add("A", leafTag, vr, value);
			}
			else {
				// Tag cannot be added in this instance, flag it as added so that the
				// action is not applied on every attribute in the instance
				tagAdded = true;
				if (log.isWarnEnabled()) {
					log.warn(LOG_PATTERN, original.getString(Tag.SOPInstanceUID), standardTagPath, "A",
							"Tag not added, it is not defined in current SOP " + original.getString(Tag.SOPClassUID));
				}
			}
		}
		return null;
	}

	@Override
	public final void profileValidation() throws ProfileException {
		if (argumentEntities == null || argumentEntities.isEmpty()) {
			throw new ProfileException("Cannot build the profile " + codeName + ": Need to specify value argument");
		}

		validateTagPaths();
		List<Integer> destination = destinationTags(tagEntities.getFirst().getTagValue());
		validateEnclosingSequences(destination);

		AttributeDetail attr = standardDICOM.getAttributeDetail(TagUtils.toHexString(destination.getLast()));

		if (attr == null) {
			throw new ProfileException("Cannot build the profile " + codeName + ": the tag "
					+ tagEntities.getFirst().getTagValue() + " does not exist in the DICOM Standard");
		}
		else {
			try {
				// The VR is currently retrieved from the DICOM Standard, in a very few
				// cases, we cannot infer this value
				// It should only concern fields that would not be included in profiles
				VR.valueOf(attr.valueRepresentation());
			}
			catch (IllegalArgumentException e) {
				throw new ProfileException("Cannot build the profile " + codeName + ": the tag "
						+ tagEntities.getFirst().getTagValue() + " is not supported and cannot be added");
			}
		}

		validateCondition();
	}

	/** Every tag of the path but the last one must be a sequence to be descended into. */
	private void validateEnclosingSequences(List<Integer> destination) throws ProfileException {
		for (int tag : destination.subList(0, destination.size() - 1)) {
			AttributeDetail detail = standardDICOM.getAttributeDetail(TagUtils.toHexString(tag));
			if (detail == null || !"SQ".equals(detail.valueRepresentation())) {
				throw new ProfileException("Cannot build the profile " + codeName + ": " + TagUtils.toString(tag)
						+ " is not a sequence, a tag can only be added inside a sequence");
			}
		}
	}

}