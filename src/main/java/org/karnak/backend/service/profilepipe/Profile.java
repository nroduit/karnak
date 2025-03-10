/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.profilepipe;

import java.awt.Color;
import java.awt.Shape;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.BulkData;
import org.dcm4che3.data.Fragments;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.img.op.MaskArea;
import org.dcm4che3.util.TagUtils;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.data.entity.SecretEntity;
import org.karnak.backend.dicom.Defacer;
import static org.karnak.backend.dicom.DefacingUtil.isAxial;
import static org.karnak.backend.dicom.DefacingUtil.isCT;
import org.karnak.backend.enums.ProfileItemType;
import org.karnak.backend.model.action.ActionItem;
import org.karnak.backend.model.action.Add;
import org.karnak.backend.model.action.ExcludeInstance;
import org.karnak.backend.model.action.Remove;
import org.karnak.backend.model.action.ReplaceNull;
import org.karnak.backend.model.expression.ExprCondition;
import org.karnak.backend.model.expression.ExpressionResult;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.model.profilepipe.HashContext;
import org.karnak.backend.model.profiles.ActionTags;
import org.karnak.backend.model.profiles.CleanPixelData;
import org.karnak.backend.model.profiles.Defacing;
import org.karnak.backend.model.profiles.ProfileItem;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.weasis.core.util.StringUtil;
import org.weasis.dicom.param.AttributeEditorContext;

@Slf4j
public class Profile {

	private final List<ProfileItem> profiles;

	private final Pseudonym pseudonym;

	private final Map<MaskStationCondition, MaskArea> maskMap;

	public Profile(ProfileEntity profileEntity) {
		this.maskMap = new HashMap<>();
		this.pseudonym = new Pseudonym();
		this.profiles = createProfilesList(profileEntity);
	}

	public MaskArea getMask(MaskStationCondition key) {
		MaskArea mask = maskMap.get(key);
		if (mask == null) {
			// No exact match, remove image size information to match the station name
			// only
			key.setImageWidth(null);
			key.setImageHeight(null);
			mask = maskMap.get(key);
		}
		if (mask == null) {
			// No match with the station name, get the universal mask
			mask = maskMap.get(new MaskStationCondition("*"));
		}
		return mask;
	}

	public void addMask(String stationName, MaskArea maskArea) {
		this.maskMap.put(new MaskStationCondition(stationName), maskArea);
	}

	public void addMask(MaskStationCondition key, MaskArea maskArea) {
		this.maskMap.put(key, maskArea);
	}

	public static List<ProfileItem> getProfileItems(ProfileEntity profileEntity) {
		final Set<ProfileElementEntity> listProfileElementEntity = profileEntity.getProfileElementEntities();
		List<ProfileItem> profileItems = new ArrayList<>();

		for (ProfileElementEntity profileElementEntity : listProfileElementEntity) {
			ProfileItemType t = ProfileItemType.getType(profileElementEntity.getCodename());
			if (t == null) {
				log.error("Cannot find the profile codename: {}", profileElementEntity.getCodename());
			}
			else {
				Object instanceProfileItem;
				try {
					instanceProfileItem = t.getProfileClass()
						.getConstructor(ProfileElementEntity.class)
						.newInstance(profileElementEntity);
					profileItems.add((ProfileItem) instanceProfileItem);
				}
				catch (Exception e) {
					log.error("Cannot build the profile: {}", t.getProfileClass().getName(), e);
				}
			}
		}
		profileItems.sort(Comparator.comparing(ProfileItem::getPosition));
		return profileItems;
	}

	public List<ProfileItem> createProfilesList(final ProfileEntity profileEntity) {
		if (profileEntity != null) {
			List<ProfileItem> profileItems = getProfileItems(profileEntity);
			profileEntity.getMaskEntities().forEach(m -> {
				Color color = null;
				if (StringUtil.hasText(m.getColor())) {
					color = ActionTags.hexadecimal2Color(m.getColor());
				}
				List<Shape> shapeList = m.getRectangles().stream().map(Shape.class::cast).collect(Collectors.toList());
				addMask(new MaskStationCondition(m.getStationName(), m.getImageWidth(), m.getImageHeight()),
						new MaskArea(shapeList, color));
			});
			return profileItems;
		}
		return Collections.emptyList();
	}

	public void applyAction(Attributes dcm, Attributes dcmCopy, HMAC hmac, ProfileItem profilePassedInSequence,
			ActionItem actionPassedInSequence, AttributeEditorContext context) {
		for (int tag : dcm.tags()) {
			VR vr = dcm.getVR(tag);
			final ExprCondition exprCondition = new ExprCondition(dcmCopy);

			ActionItem currentAction = null;
			ProfileItem currentProfile = null;
			for (ProfileItem profileEntity : profiles.stream().filter(p -> !(p instanceof CleanPixelData)).toList()) {
				currentProfile = profileEntity;

				if (profileEntity.getCondition() == null
						|| profileEntity.getCodeName().equals(ProfileItemType.DEFACING.getClassAlias())
						|| profileEntity.getCodeName().equals(ProfileItemType.CLEAN_PIXEL_DATA.getClassAlias())) {
					currentAction = profileEntity.getAction(dcm, dcmCopy, tag, hmac);
				}
				else {
					boolean conditionIsOk = (Boolean) ExpressionResult.get(profileEntity.getCondition(), exprCondition,
							Boolean.class);
					if (conditionIsOk) {
						currentAction = profileEntity.getAction(dcm, dcmCopy, tag, hmac);
					}
				}

				if (currentAction != null) {
					if (currentAction instanceof Add) {
						// When adding a new tag, the variable tag is irrelevant and
						// should not be flagged as modified
						// Set the current action to null after execution and do not break
						// out of the loop if other
						// profile elements should be applied to the tag
						execute(currentAction, dcm, tag, hmac);
						currentAction = null;

					}
					else if (currentAction instanceof ExcludeInstance) {
						context.setAbort(AttributeEditorContext.Abort.FILE_EXCEPTION);
						context.setAbortMessage(
								String.format("Instance excluded by profile: %s", profileEntity.getName()));
						return;
					}
					else {
						break;
					}
				}

				if (profileEntity.equals(profilePassedInSequence)) {
					currentAction = actionPassedInSequence;
					break;
				}
			}

			if (!(currentAction instanceof Remove) && !(currentAction instanceof ReplaceNull) && vr == VR.SQ) {
				final ProfileItem finalCurrentProfile = currentProfile;
				final ActionItem finalCurrentAction = currentAction;
				Sequence seq = dcm.getSequence(tag);
				if (seq != null) {
					for (Attributes d : seq) {
						applyAction(d, dcmCopy, hmac, finalCurrentProfile, finalCurrentAction, context);
					}
				}
			}
			else {
				if (currentAction != null) {
					execute(currentAction, dcm, tag, hmac);
				}
			}
		}
	}

	private void execute(ActionItem currentAction, Attributes dcm, int tag, HMAC hmac) {
		if (currentAction == null) {
			return;
		}
		try {
			currentAction.execute(dcm, tag, hmac);
		}
		catch (final Exception e) {
			log.error("Cannot execute the currentAction {} for tag: {}", currentAction, TagUtils.toString(tag), e);
		}
	}

	public void applyCleanPixelData(Attributes dcmCopy, AttributeEditorContext context, ProfileEntity profileEntity) {
		Object pix = dcmCopy.getValue(Tag.PixelData);
		if ((pix instanceof BulkData || pix instanceof Fragments) && !profileEntity.getMaskEntities().isEmpty()
				&& profiles.stream().anyMatch(CleanPixelData.class::isInstance)) {
			String sopClassUID = dcmCopy.getString(Tag.SOPClassUID);
			if (!StringUtil.hasText(sopClassUID)) {
				throw new IllegalStateException("DICOM Object does not contain sopClassUID");
			}
			MaskArea mask = getMask(new MaskStationCondition(dcmCopy.getString(Tag.StationName),
					dcmCopy.getString(Tag.Columns), dcmCopy.getString(Tag.Rows)));
			// A mask must be applied with all the US and Secondary Capture sopClassUID,
			// and with
			// BurnedInAnnotation
			if (isCleanPixelAllowedDependingImageType(dcmCopy, sopClassUID)
					&& evaluateConditionCleanPixelData(dcmCopy)) {
				context.setMaskArea(mask);
				if (mask == null) {
					throw new IllegalStateException("Clean pixel is not applied: mask not defined in station name");
				}
			}
			else {
				context.setMaskArea(null);
			}
		}
	}

	/**
	 * Determine if the clean pixel should be applied depending on the image type
	 * @param dcmCopy Attributes
	 * @param sopClassUID SopClassUID
	 * @return true if the clean pixel could be applied
	 */
	boolean isCleanPixelAllowedDependingImageType(Attributes dcmCopy, String sopClassUID) {
		// A mask must be applied with all the US and Secondary Capture sopClassUID, and
		// with BurnedInAnnotation
		String sopPattern = sopClassUID + ".";

		return sopPattern.startsWith("1.2.840.10008.5.1.4.1.1.6.")
				|| sopPattern.startsWith("1.2.840.10008.5.1.4.1.1.7.")
				|| sopPattern.startsWith("1.2.840.10008.5.1.4.1.1.3.")
				|| sopClassUID.equals("1.2.840.10008.5.1.4.1.1.77.1.1")
				|| "YES".equalsIgnoreCase(dcmCopy.getString(Tag.BurnedInAnnotation));
	}

	/**
	 * Evaluate the condition on the profile Clean Pixel Data
	 * @param dcmCopy Context copy
	 * @return true if the condition match, false if there are some exclusions
	 */
	boolean evaluateConditionCleanPixelData(Attributes dcmCopy) {
		boolean conditionCleanPixelData = true;
		// Retrieve the profile item
		ProfileItem profileItemCleanPixelData = profiles.stream()
			.filter(CleanPixelData.class::isInstance)
			.findFirst()
			.orElse(null);
		if (profileItemCleanPixelData != null && profileItemCleanPixelData.getCondition() != null) {
			// Evaluate the condition
			ExprCondition exprCondition = new ExprCondition(dcmCopy);
			conditionCleanPixelData = (Boolean) ExpressionResult.get(profileItemCleanPixelData.getCondition(),
					exprCondition, Boolean.class);
		}
		return conditionCleanPixelData;
	}

	public void applyDefacing(Attributes dcmCopy, AttributeEditorContext context) {
		ProfileItem profileItemDefacing = profiles.stream().filter(Defacing.class::isInstance).findFirst().orElse(null);
		if (profileItemDefacing != null && isCT(dcmCopy) && isAxial(dcmCopy)) {
			if (profileItemDefacing.getCondition() == null) {
				context.getProperties().setProperty(Defacer.APPLY_DEFACING, "true");
			}
			else {
				ExprCondition exprCondition = new ExprCondition(dcmCopy);
				boolean conditionIsOk = (Boolean) ExpressionResult.get(profileItemDefacing.getCondition(),
						exprCondition, Boolean.class);
				if (conditionIsOk) {
					context.getProperties().setProperty(Defacer.APPLY_DEFACING, "true");
				}
			}
		}
	}

	/**
	 * Apply deidentification
	 * @param dcm Attributes
	 * @param destinationEntity Destination
	 * @param profileEntity Profile
	 * @param context Context
	 * @param projectEntity Project
	 */
	public void applyDeIdentification(Attributes dcm, DestinationEntity destinationEntity, ProfileEntity profileEntity,
			AttributeEditorContext context, ProjectEntity projectEntity) {
		final String SOPInstanceUID = dcm.getString(Tag.SOPInstanceUID);
		final String SeriesInstanceUID = dcm.getString(Tag.SeriesInstanceUID);
		final String IssuerOfPatientID = dcm.getString(Tag.IssuerOfPatientID);
		final String PatientID = dcm.getString(Tag.PatientID);
		final HMAC hmac = generateHMAC(PatientID, projectEntity);

		MDC.put("SOPInstanceUID", SOPInstanceUID);
		MDC.put("SeriesInstanceUID", SeriesInstanceUID);
		MDC.put("issuerOfPatientID", IssuerOfPatientID);
		MDC.put("PatientID", PatientID);

		String pseudonymValue = this.pseudonym.generatePseudonym(destinationEntity, dcm);

		String profilesCodeName = profiles.stream().map(ProfileItem::getCodeName).collect(Collectors.joining("-"));
		BigInteger patientValue = generatePatientID(pseudonymValue, hmac);
		String newPatientID = patientValue.toString(16).toUpperCase();

		Attributes dcmCopy = new Attributes(dcm);

		// Apply clean pixel data
		applyCleanPixelData(dcmCopy, context, profileEntity);

		// Apply clean recognizable visual features option
		applyDefacing(dcmCopy, context);

		applyAction(dcm, dcmCopy, hmac, null, null, context);

		// Set tags by default
		AttributesByDefault.setPatientModule(dcm, newPatientID, pseudonymValue,
				destinationEntity.getDeIdentificationProjectEntity());
		AttributesByDefault.setSOPCommonModule(dcm);
		AttributesByDefault.setClinicalTrialAttributes(dcm, destinationEntity.getDeIdentificationProjectEntity(),
				pseudonymValue);

		final Marker clincalMarker = MarkerFactory.getMarker("CLINICAL");
		MDC.put("DeidentifySOPInstanceUID", dcm.getString(Tag.SOPInstanceUID));
		MDC.put("DeidentifySeriesInstanceUID", dcm.getString(Tag.SeriesInstanceUID));
		MDC.put("ProjectName", destinationEntity.getDeIdentificationProjectEntity().getName());
		MDC.put("ProfileName", profileEntity.getName());
		MDC.put("ProfileCodenames", profilesCodeName);
		log.info(clincalMarker, "");
		MDC.clear();
	}

	public void applyTagMorphing(Attributes dcm, DestinationEntity destinationEntity, ProfileEntity profileEntity,
			AttributeEditorContext context, ProjectEntity projectEntity) {
		final String SOPInstanceUID = dcm.getString(Tag.SOPInstanceUID);
		final String SeriesInstanceUID = dcm.getString(Tag.SeriesInstanceUID);
		final String IssuerOfPatientID = dcm.getString(Tag.IssuerOfPatientID);
		final String PatientID = dcm.getString(Tag.PatientID);
		final HMAC hmac = generateHMAC(PatientID, projectEntity);

		MDC.put("SOPInstanceUID", SOPInstanceUID);
		MDC.put("SeriesInstanceUID", SeriesInstanceUID);
		MDC.put("issuerOfPatientID", IssuerOfPatientID);
		MDC.put("PatientID", PatientID);

		String profilesCodeName = profiles.stream().map(ProfileItem::getCodeName).collect(Collectors.joining("-"));

		Attributes dcmCopy = new Attributes(dcm);

		// Apply actions on tags
		applyAction(dcm, dcmCopy, hmac, null, null, context);

		final Marker clincalMarker = MarkerFactory.getMarker("CLINICAL");
		MDC.put("TagMorphingSOPInstanceUID", dcm.getString(Tag.SOPInstanceUID));
		MDC.put("TagMorphingSeriesInstanceUID", dcm.getString(Tag.SeriesInstanceUID));
		MDC.put("ProjectName", destinationEntity.getTagMorphingProjectEntity().getName());
		MDC.put("ProfileName", profileEntity.getName());
		MDC.put("ProfileCodenames", profilesCodeName);
		log.info(clincalMarker, "");
		MDC.clear();
	}

	private HMAC generateHMAC(String patientID, ProjectEntity projectEntity) {
		if (projectEntity == null) {
			throw new IllegalStateException("Cannot build the HMAC a project is not associate at the destination");
		}

		SecretEntity secretEntity = projectEntity.retrieveActiveSecret();
		byte[] secret = secretEntity != null ? secretEntity.getSecretKey() : null;
		if (secret == null || secret.length != HMAC.KEY_BYTE_LENGTH) {
			throw new IllegalStateException(
					"Cannot build the HMAC no secret defined in the project associate at the destination");
		}

		if (patientID == null) {
			throw new IllegalStateException("Cannot build the HMAC no PatientID given");
		}

		HashContext hashContext = new HashContext(secret, patientID);
		return new HMAC(hashContext);
	}

	public BigInteger generatePatientID(String pseudonym, HMAC hmac) {
		byte[] bytes = new byte[16];
		System.arraycopy(hmac.byteHash(pseudonym), 0, bytes, 0, 16);
		return new BigInteger(1, bytes);
	}

}
