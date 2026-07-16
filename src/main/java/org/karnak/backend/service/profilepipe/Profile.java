/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.profilepipe;

import static org.karnak.backend.dicom.DefacingUtil.isAxial;
import static org.karnak.backend.dicom.DefacingUtil.isCT;

import java.awt.*;
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
import org.jspecify.annotations.Nullable;
import org.karnak.backend.data.converter.RectangleListConverter;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.data.entity.SecretEntity;
import org.karnak.backend.dicom.Defacer;
import org.karnak.backend.enums.ProfileItemType;
import org.karnak.backend.model.action.ActionItem;
import org.karnak.backend.model.action.Add;
import org.karnak.backend.model.action.ExcludeInstance;
import org.karnak.backend.model.action.Remove;
import org.karnak.backend.model.action.ReplaceNull;
import org.karnak.backend.model.expression.ExprCondition;
import org.karnak.backend.model.expression.ExpressionResult;
import org.karnak.backend.model.profilebody.MaskBody;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.model.profilepipe.HashContext;
import org.karnak.backend.model.profilepipe.SensitiveTagDefinition;
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

	private static final Marker CLINICAL_MARKER = MarkerFactory.getMarker("CLINICAL");

	private final List<ProfileItem> profiles;

	/**
	 * {@link #profiles} without the {@link CleanPixelData} items, applied on every tag.
	 */
	private final List<ProfileItem> tagProfiles;

	private final Pseudonym pseudonym;

	private final Map<MaskStationCondition, MaskArea> maskMap;

	private final DeidentifyImageService deidentifyImageService;

	/**
	 * Property key used to store additional {@link MaskArea} objects in the
	 * {@link AttributeEditorContext#getProperties()} map. The main (first) mask is set
	 * via {@link AttributeEditorContext#setMaskArea(MaskArea)}, and any extra masks (with
	 * potentially different colors) are stored here so that {@code ForwardService} can
	 * draw them all sequentially.
	 */
	public static final String ADDITIONAL_MASK_AREAS_KEY = "additionalMaskAreas";

	/**
	 * @param profileEntity the profile configuration
	 * @param deidentifyImageService the service that calls the external de-id image API;
	 * pass {@code null} to disable the API call
	 */
	public Profile(ProfileEntity profileEntity, DeidentifyImageService deidentifyImageService) {
		this.maskMap = new HashMap<>();
		this.pseudonym = new Pseudonym();
		this.deidentifyImageService = deidentifyImageService;
		this.profiles = this.createProfilesList(profileEntity);
		this.tagProfiles = this.profiles.stream().filter(p -> !(p instanceof CleanPixelData)).toList();
	}

	/**
	 * Convenience constructor for tests and legacy callers that do not need the
	 * de-identification image API.
	 * @param profileEntity the profile configuration
	 */
	public Profile(ProfileEntity profileEntity) {
		this(profileEntity, null);
	}

	public static List<ProfileItem> getProfileItems(ProfileEntity profileEntity) {
		final Set<ProfileElementEntity> listProfileElementEntity = profileEntity.getProfileElementEntities();
		List<ProfileItem> profileItems = new ArrayList<>();

		for (ProfileElementEntity profileElementEntity : listProfileElementEntity) {
			ProfileItemType t = ProfileItemType.getType(profileElementEntity.getCodename());
			if (t == null) {
				log.error("Cannot find the profile codename: {}", profileElementEntity.getCodename());
				continue;
			}
			try {
				profileItems.add(t.getProfileClass()
					.getConstructor(ProfileElementEntity.class)
					.newInstance(profileElementEntity));
			}
			catch (Exception e) {
				log.error("Cannot build the profile: {}", t.getProfileClass().getName(), e);
			}
		}
		profileItems.sort(Comparator.comparing(ProfileItem::getPosition));
		return profileItems;
	}

	public @Nullable MaskArea getMask(MaskStationCondition key) {
		MaskArea mask = this.maskMap.get(key);
		if (mask == null) {
			// No exact match, remove image size information to match the station name
			// only
			key.setImageWidth(null);
			key.setImageHeight(null);
			mask = this.maskMap.get(key);
		}
		if (mask == null) {
			// No match with the station name, get the universal mask
			mask = this.maskMap.get(new MaskStationCondition("*"));
		}
		return mask;
	}

	public void addMask(String stationName, MaskArea maskArea) {
		this.maskMap.put(new MaskStationCondition(stationName), maskArea);
	}

	public void addMask(MaskStationCondition key, MaskArea maskArea) {
		this.maskMap.put(key, maskArea);
	}

	public List<ProfileItem> createProfilesList(final ProfileEntity profileEntity) {
		if (profileEntity != null) {
			List<ProfileItem> profileItems = getProfileItems(profileEntity);
			profileEntity.getMaskEntities().forEach(m -> {
				Color color = null;
				if (StringUtil.hasText(m.getColor())) {
					color = ActionTags.hexadecimal2Color(m.getColor());
				}
				List<Shape> shapeList = m.getRectangles().stream().map(Shape.class::cast).toList();
				this.addMask(new MaskStationCondition(m.getStationName(), m.getImageWidth(), m.getImageHeight()),
						new MaskArea(shapeList, color));
			});
			return profileItems;
		}
		return Collections.emptyList();
	}

	public void applyAction(Attributes dcm, Attributes dcmCopy, HMAC hmac,
			@Nullable ProfileItem profilePassedInSequence, @Nullable ActionItem actionPassedInSequence,
			AttributeEditorContext context) {
		for (int tag : dcm.tags()) {
			VR vr = dcm.getVR(tag);
			final ExprCondition exprCondition = new ExprCondition(dcmCopy);

			ActionItem currentAction = null;
			ProfileItem currentProfile = null;
			for (ProfileItem profileEntity : this.tagProfiles) {
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
						this.execute(currentAction, dcm, tag, hmac);
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
				Sequence seq = dcm.getSequence(tag);
				if (seq != null) {
					for (Attributes d : seq) {
						this.applyAction(d, dcmCopy, hmac, currentProfile, currentAction, context);
					}
				}
			}
			else {
				if (currentAction != null) {
					this.execute(currentAction, dcm, tag, hmac);
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
		if ((pix instanceof BulkData || pix instanceof Fragments)
				&& this.profiles.stream().anyMatch(CleanPixelData.class::isInstance)) {
			String sopClassUID = dcmCopy.getString(Tag.SOPClassUID);
			if (!StringUtil.hasText(sopClassUID)) {
				throw new IllegalStateException("DICOM Object does not contain sopClassUID");
			}

			MaskArea mask = null;

			CleanPixelData cleanPixelDataItem = (CleanPixelData) this.profiles.stream()
				.filter(CleanPixelData.class::isInstance)
				.findFirst()
				.orElse(null);

			if (cleanPixelDataItem != null && cleanPixelDataItem.isAutomaticMasksGeneration()) {
				List<MaskArea> apiMasks = this.fetchMasksFromDeidentifyImageApi(dcmCopy, context.getTsuid());
				if (!apiMasks.isEmpty()) {
					// Use the first API mask as the "primary" mask (set on the context),
					// and store the rest as "additional" masks in context.properties.
					mask = apiMasks.getFirst();
					if (apiMasks.size() > 1) {
						context.getProperties().put(ADDITIONAL_MASK_AREAS_KEY, apiMasks.subList(1, apiMasks.size()));
					}
				}
			}
			if (mask == null && (cleanPixelDataItem == null || !cleanPixelDataItem.isAutomaticMasksGeneration())) {
				// Manual mask should be applied only if automatic mask generation is not
				// enabled
				// If the API doesn't return any mask (because of an internal error,
				// unreachable, etc...)
				// then the data should not be sent
				mask = this.getMask(new MaskStationCondition(dcmCopy.getString(Tag.StationName),
						dcmCopy.getString(Tag.Columns), dcmCopy.getString(Tag.Rows)));
			}

			// A mask must be applied with all the US and Secondary Capture sopClassUID,
			// and with BurnedInAnnotation
			if (this.isCleanPixelAllowedDependingImageType(dcmCopy, sopClassUID)
					&& this.evaluateConditionCleanPixelData(dcmCopy)) {
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

	private MaskArea toMaskArea(MaskBody maskBody) {
		Color color = StringUtil.hasText(maskBody.getColor()) ? ActionTags.hexadecimal2Color(maskBody.getColor())
				: null;

		List<Shape> shapeList = new ArrayList<>();
		if (maskBody.getRectangles() != null) {
			for (String rectStr : maskBody.getRectangles()) {
				Rectangle rect = RectangleListConverter.stringToRectangle(rectStr);
				if (rect != null) {
					shapeList.add(rect);
				}
			}
		}

		return shapeList.isEmpty() ? null : new MaskArea(shapeList, color);
	}

	/**
	 * Calls the external de-identification image API and converts the returned masks into
	 * a list of {@link MaskArea}.
	 * @param dcmCopy DICOM attributes (original copy, before de-identification)
	 * @return a list of {@link MaskArea}, possibly empty but never {@code null}
	 */
	private List<MaskArea> fetchMasksFromDeidentifyImageApi(Attributes dcmCopy, String tsuid) {
		if (this.deidentifyImageService == null) {
			return Collections.emptyList();
		}
		Map<String, String> sensitiveData = SensitiveTagDefinition.extractSensitiveData(dcmCopy);

		List<MaskBody> masks = this.deidentifyImageService.callDeidentifyImageApi(dcmCopy, sensitiveData, tsuid);

		if (masks.isEmpty()) {
			return Collections.emptyList();
		}

		// Convert each MaskBody into a MaskArea.
		// Each one becomes a separate MaskArea so different colors are preserved.
		List<MaskArea> maskAreas = new ArrayList<>();
		for (MaskBody maskBody : masks) {
			MaskArea maskArea = this.toMaskArea(maskBody);
			if (maskArea != null) {
				maskAreas.add(maskArea);
			}
		}

		log.debug("De-identification image API returned {} mask area(s)", maskAreas.size());
		return maskAreas;
	}

	/**
	 * A mask applies to US, Secondary Capture and XC SOP classes, or any
	 * BurnedInAnnotation image.
	 */
	boolean isCleanPixelAllowedDependingImageType(Attributes dcmCopy, String sopClassUID) {
		String sopPattern = sopClassUID + ".";

		return sopPattern.startsWith("1.2.840.10008.5.1.4.1.1.6.")
				|| sopPattern.startsWith("1.2.840.10008.5.1.4.1.1.7.")
				|| sopPattern.startsWith("1.2.840.10008.5.1.4.1.1.3.")
				|| sopClassUID.equals("1.2.840.10008.5.1.4.1.1.77.1.1")
				|| "YES".equalsIgnoreCase(dcmCopy.getString(Tag.BurnedInAnnotation));
	}

	/**
	 * Evaluates the optional condition of the Clean Pixel Data profile item (true if
	 * none).
	 */
	boolean evaluateConditionCleanPixelData(Attributes dcmCopy) {
		boolean conditionCleanPixelData = true;
		ProfileItem profileItemCleanPixelData = this.profiles.stream()
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
		ProfileItem profileItemDefacing = this.profiles.stream()
			.filter(Defacing.class::isInstance)
			.findFirst()
			.orElse(null);
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
		HMAC hmac = this.generateHMAC(dcm.getString(Tag.PatientID), projectEntity);
		putSourceMdc(dcm);

		String pseudonymValue = this.pseudonym.generatePseudonym(destinationEntity, dcm);
		String newPatientID = this.generatePatientID(pseudonymValue, hmac).toString(16).toUpperCase();

		Attributes dcmCopy = new Attributes(dcm);
		this.applyCleanPixelData(dcmCopy, context, profileEntity);
		// Clean recognizable visual features option
		this.applyDefacing(dcmCopy, context);
		this.applyAction(dcm, dcmCopy, hmac, null, null, context);

		ProjectEntity deidProject = destinationEntity.getDeIdentificationProjectEntity();
		AttributesByDefault.setPatientModule(dcm, newPatientID, pseudonymValue, deidProject);
		AttributesByDefault.setSOPCommonModule(dcm);
		AttributesByDefault.setClinicalTrialAttributes(dcm, deidProject, pseudonymValue);

		this.logClinicalEvent("Deidentify", dcm, deidProject.getName(), profileEntity.getName());
	}

	public void applyTagMorphing(Attributes dcm, DestinationEntity destinationEntity, ProfileEntity profileEntity,
			AttributeEditorContext context, ProjectEntity projectEntity) {
		HMAC hmac = this.generateHMAC(dcm.getString(Tag.PatientID), projectEntity);
		putSourceMdc(dcm);

		Attributes dcmCopy = new Attributes(dcm);
		this.applyAction(dcm, dcmCopy, hmac, null, null, context);

		this.logClinicalEvent("TagMorphing", dcm, destinationEntity.getTagMorphingProjectEntity().getName(),
				profileEntity.getName());
	}

	private static void putSourceMdc(Attributes dcm) {
		MDC.put("SOPInstanceUID", dcm.getString(Tag.SOPInstanceUID));
		MDC.put("SeriesInstanceUID", dcm.getString(Tag.SeriesInstanceUID));
		MDC.put("issuerOfPatientID", dcm.getString(Tag.IssuerOfPatientID));
		MDC.put("PatientID", dcm.getString(Tag.PatientID));
	}

	private void logClinicalEvent(String prefix, Attributes dcm, String projectName, String profileName) {
		MDC.put(prefix + "SOPInstanceUID", dcm.getString(Tag.SOPInstanceUID));
		MDC.put(prefix + "SeriesInstanceUID", dcm.getString(Tag.SeriesInstanceUID));
		MDC.put("ProjectName", projectName);
		MDC.put("ProfileName", profileName);
		MDC.put("ProfileCodenames",
				this.profiles.stream().map(ProfileItem::getCodeName).collect(Collectors.joining("-")));
		log.debug(CLINICAL_MARKER, "");
		MDC.clear();
	}

	private HMAC generateHMAC(String patientID, ProjectEntity projectEntity) {
		if (projectEntity == null) {
			throw new IllegalStateException("Cannot build the HMAC a project is not associate at the destination");
		}

		SecretEntity secretEntity = projectEntity.retrieveActiveSecret();
		byte[] secret = (secretEntity != null) ? secretEntity.getSecretKey() : null;
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
