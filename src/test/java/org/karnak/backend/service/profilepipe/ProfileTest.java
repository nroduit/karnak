/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.profilepipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.img.op.MaskArea;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.IncludedTagEntity;
import org.karnak.backend.data.entity.MaskEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.data.entity.SecretEntity;
import org.karnak.backend.enums.DestinationType;
import org.karnak.backend.enums.PseudonymType;
import org.springframework.boot.test.context.SpringBootTest;
import org.weasis.dicom.param.AttributeEditorContext;

@SpringBootTest
class ProfileTest {

	@Test
	void should_apply() {

		// Init data
		Attributes attributes = new Attributes();
		DestinationEntity destinationEntity = new DestinationEntity();
		destinationEntity.setDestinationType(DestinationType.dicom);
		ProjectEntity projectEntity = new ProjectEntity();
		ProfileEntity profileEntityProject = new ProfileEntity();
		projectEntity.setProfileEntity(profileEntityProject);
		byte[] tabByte = new byte[16];
		tabByte[0] = 1;
		projectEntity.addActiveSecretEntity(new SecretEntity(tabByte));
		destinationEntity.setDeIdentificationProjectEntity(projectEntity);
		destinationEntity.setPseudonymType(PseudonymType.EXTID_IN_TAG);
		destinationEntity.setTag("0008,0080");
		destinationEntity.setSavePseudonym(false);
		ProfileEntity profileEntity = new ProfileEntity();
		Set<ProfileElementEntity> profileElementEntities = new HashSet<>();
		ProfileElementEntity profileElementEntityBasic = new ProfileElementEntity();
		profileElementEntityBasic.setCodename("basic.dicom.profile");
		profileElementEntityBasic.setName("nameBasic");
		ProfileElementEntity profileElementEntityCleanPixelData = new ProfileElementEntity();
		profileElementEntityCleanPixelData.setCodename("clean.pixel.data");
		profileElementEntityCleanPixelData.setName("nameCleanPixel");
		profileElementEntityBasic.setPosition(1);
		profileElementEntityCleanPixelData.setPosition(2);
		profileElementEntityBasic.setAction("ReplaceNull");
		profileElementEntityCleanPixelData.setAction("ReplaceNull");

		profileElementEntityCleanPixelData.setCondition("!tagValueContains(#Tag.StationName,'ICT256')");

		profileElementEntities.add(profileElementEntityBasic);
		profileElementEntities.add(profileElementEntityCleanPixelData);
		profileEntity.setProfileElementEntities(profileElementEntities);
		profileEntity.setDefaultIssuerOfPatientId("defaultIssuerOfPatientId");
		Set<MaskEntity> maskEntities = new HashSet<>();
		MaskEntity maskEntity = new MaskEntity();
		maskEntities.add(maskEntity);
		maskEntity.setColor("1234567897");
		maskEntity.setStationName("stationName");
		maskEntity.setRectangles(Arrays.asList(new Rectangle()));
		profileEntity.setMaskEntities(maskEntities);
		AttributeEditorContext context = new AttributeEditorContext("tsuid", null, null);
		attributes.setString(Tag.PatientID, VR.SH, "patientID");
		attributes.setString(Tag.SeriesInstanceUID, VR.SH, "seriesInstanceUID");
		attributes.setString(Tag.SOPInstanceUID, VR.SH, "sopInstanceUID");
		attributes.setString(Tag.IssuerOfPatientID, VR.SH, "issuerOfPatientID");
		attributes.setString(Tag.PixelData, VR.SH, "pixelData");
		attributes.setString(Tag.SOPClassUID, VR.SH, "1.2.840.10008.5.1.4.1.1.88.74");
		attributes.setString(Tag.BurnedInAnnotation, VR.SH, "YES");
		attributes.setString(Tag.StationName, VR.SH, "stationName");
		attributes.setString(524416, VR.SH, "pseudonym");

		// Call method
		Profile profile = new Profile(profileEntity);
		// profile.init(profileEntity);
		profile.applyDeIdentification(attributes, destinationEntity, profileEntity, context, projectEntity);

		// Test results
		assertEquals("NONE", context.getAbort().name());
		assertNull(context.getMaskArea());
	}

	@Test
	void should_evaluate_condition_clean_pixel_case_no_condition() {

		// Init data
		ProfileEntity profileEntity = new ProfileEntity();
		Attributes attributes = new Attributes();
		Set<ProfileElementEntity> profileElementEntities = new HashSet<>();
		ProfileElementEntity profileElementEntityCleanPixelData = new ProfileElementEntity();
		profileElementEntityCleanPixelData.setCodename("clean.pixel.data");
		profileElementEntityCleanPixelData.setName("nameCleanPixel");
		profileElementEntityCleanPixelData.setAction("ReplaceNull");
		profileElementEntityCleanPixelData.setCondition(null);

		profileElementEntities.add(profileElementEntityCleanPixelData);
		profileEntity.setProfileElementEntities(profileElementEntities);
		Profile profile = new Profile(profileEntity);

		// Evaluate condition
		boolean evaluation = profile.evaluateConditionCleanPixelData(attributes);

		// Test results
		assertTrue(evaluation);
	}

	@Test
	void should_evaluate_condition_clean_pixel_case_exclude_station_name() {

		// Init data
		ProfileEntity profileEntity = new ProfileEntity();
		Attributes attributes = new Attributes();
		attributes.setString(Tag.StationName, VR.SH, "ICT256");
		Set<ProfileElementEntity> profileElementEntities = new HashSet<>();
		ProfileElementEntity profileElementEntityCleanPixelData = new ProfileElementEntity();
		profileElementEntityCleanPixelData.setCodename("clean.pixel.data");
		profileElementEntityCleanPixelData.setName("nameCleanPixel");
		profileElementEntityCleanPixelData.setAction("ReplaceNull");

		profileElementEntityCleanPixelData.setCondition("!tagValueContains(#Tag.StationName,'ICT256')");

		profileElementEntities.add(profileElementEntityCleanPixelData);
		profileEntity.setProfileElementEntities(profileElementEntities);
		Profile profile = new Profile(profileEntity);

		// Evaluate condition
		boolean evaluation = profile.evaluateConditionCleanPixelData(attributes);

		// Test results
		assertFalse(evaluation);
	}

	@Test
	void should_evaluate_condition_clean_pixel_case_include_station_name() {

		// Init data
		ProfileEntity profileEntity = new ProfileEntity();
		Attributes attributes = new Attributes();
		attributes.setString(Tag.StationName, VR.SH, "ICT256");
		Set<ProfileElementEntity> profileElementEntities = new HashSet<>();
		ProfileElementEntity profileElementEntityCleanPixelData = new ProfileElementEntity();
		profileElementEntityCleanPixelData.setCodename("clean.pixel.data");
		profileElementEntityCleanPixelData.setName("nameCleanPixel");
		profileElementEntityCleanPixelData.setAction("ReplaceNull");

		profileElementEntityCleanPixelData.setCondition("tagValueContains(#Tag.StationName,'ICT256')");

		profileElementEntities.add(profileElementEntityCleanPixelData);
		profileEntity.setProfileElementEntities(profileElementEntities);
		Profile profile = new Profile(profileEntity);

		// Evaluate condition
		boolean evaluation = profile.evaluateConditionCleanPixelData(attributes);

		// Test results
		assertTrue(evaluation);
	}

	@Test
	void should_select_correct_mask_station_size() {

		// Init data
		ProfileEntity profileEntity = new ProfileEntity();
		Attributes attributes = new Attributes();
		attributes.setString(Tag.StationName, VR.SH, "ICT256");
		Attributes attributes2 = new Attributes();
		attributes2.setString(Tag.StationName, VR.SH, "ICT256");
		attributes2.setString(Tag.Rows, VR.US, "1024");
		attributes2.setString(Tag.Columns, VR.US, "1024");
		Attributes attributes3 = new Attributes();
		attributes3.setString(Tag.StationName, VR.SH, "ICT258");
		attributes3.setString(Tag.Rows, VR.US, "1024");
		attributes3.setString(Tag.Columns, VR.US, "1024");
		Set<ProfileElementEntity> profileElementEntities = new HashSet<>();
		ProfileElementEntity profileElementEntityCleanPixelData = new ProfileElementEntity();
		profileElementEntityCleanPixelData.setCodename("clean.pixel.data");
		profileElementEntityCleanPixelData.setName("nameCleanPixel");
		profileElementEntityCleanPixelData.setAction("ReplaceNull");

		MaskEntity maskEntity = new MaskEntity("*", "ffff00", profileEntity);
		maskEntity.addRectangle("25 75 150 50");
		MaskEntity maskEntityStation = new MaskEntity("ICT256", "00ff00", profileEntity);
		maskEntityStation.addRectangle("350 15 150 50");
		MaskEntity maskEntityStationSize = new MaskEntity("ICT256", 1024L, 1024L, "00ffff", profileEntity);
		maskEntityStationSize.addRectangle("250 10 150 50");

		profileElementEntities.add(profileElementEntityCleanPixelData);
		profileEntity.setProfileElementEntities(profileElementEntities);
		profileEntity.setMaskEntities(Set.of(maskEntity, maskEntityStation, maskEntityStationSize));
		Profile profile = new Profile(profileEntity);

		// Matches the maskEntityStation, stationName = ICT256
		MaskArea ma = profile.getMask(new MaskStationCondition(attributes.getString(Tag.StationName),
				attributes.getString(Tag.Columns), attributes.getString(Tag.Rows)));
		assertEquals(new Rectangle(350, 15, 150, 50), ma.getShapeList().getFirst());

		// Matches the maskEntityStationSize, stationName = ICT256 && width = 1024 &&
		// height = 1024
		MaskArea ma2 = profile.getMask(new MaskStationCondition(attributes2.getString(Tag.StationName),
				attributes2.getString(Tag.Columns), attributes2.getString(Tag.Rows)));
		assertEquals(new Rectangle(250, 10, 150, 50), ma2.getShapeList().getFirst());

		// Matches the maskEntity, stationName = *
		MaskArea ma3 = profile.getMask(new MaskStationCondition(attributes3.getString(Tag.StationName),
				attributes3.getString(Tag.Columns), attributes3.getString(Tag.Rows)));
		assertEquals(new Rectangle(25, 75, 150, 50), ma3.getShapeList().getFirst());
	}

	@Test
	void cleanPixelData_imageTypeIsXA_forceCleanPixelByStationNumber() {
		// Use case : X Ray Angiography that contains identifying information
		// but the BurnedInAnnotation attribute is not set
		ProfileEntity profileEntity = new ProfileEntity();
		Attributes attributes = new Attributes();
		attributes.setString(Tag.Modality, VR.CS, "XA");
		attributes.setString(Tag.SOPClassUID, VR.UI, "1.2.840.10008.5.1.4.1.1.12.1");
		attributes.setString(Tag.StationName, VR.SH, "ICT256");
		// Similar object but not from a station that put information in the image
		// The BurnedInAnnotation attribute should not be added and the mask not applied
		Attributes attributes2 = new Attributes();
		attributes2.setString(Tag.Modality, VR.CS, "XA");
		attributes2.setString(Tag.SOPClassUID, VR.UI, "1.2.840.10008.5.1.4.1.1.12.1");
		attributes2.setString(Tag.StationName, VR.SH, "ICT258");

		ProfileElementEntity profileElementEntityAddBurnedAttr = new ProfileElementEntity();
		profileElementEntityAddBurnedAttr.setCodename("action.add.tag");
		profileElementEntityAddBurnedAttr.setName("Add tag BurnedInAnnotation if does not exist");
		profileElementEntityAddBurnedAttr
			.setCondition("tagValueContains(#Tag.StationName,'ICT256') && !tagIsPresent(#Tag.BurnedInAnnotation)");
		profileElementEntityAddBurnedAttr
			.addArgument(new ArgumentEntity("value", "YES", profileElementEntityAddBurnedAttr));
		profileElementEntityAddBurnedAttr
			.addArgument(new ArgumentEntity("vr", "CS", profileElementEntityAddBurnedAttr));
		profileElementEntityAddBurnedAttr
			.addIncludedTag(new IncludedTagEntity("(0028,0301)", profileElementEntityAddBurnedAttr));
		profileElementEntityAddBurnedAttr.setPosition(1);

		ProfileElementEntity profileElementEntityCleanPixelPerMachine = new ProfileElementEntity();
		profileElementEntityCleanPixelPerMachine.setCodename("expression.on.tags");
		profileElementEntityCleanPixelPerMachine.setName("Set BurnedInAnnotation to YES");
		profileElementEntityCleanPixelPerMachine.setCondition("tagValueContains(#Tag.StationName,'ICT256')");
		profileElementEntityCleanPixelPerMachine
			.addArgument(new ArgumentEntity("expr", "Replace('YES')", profileElementEntityCleanPixelPerMachine));
		profileElementEntityCleanPixelPerMachine
			.addIncludedTag(new IncludedTagEntity("(0028,0301)", profileElementEntityCleanPixelPerMachine));
		profileElementEntityCleanPixelPerMachine.setPosition(2);

		Set<ProfileElementEntity> profileElementEntities = new HashSet<>();
		ProfileElementEntity profileElementEntityCleanPixelData = new ProfileElementEntity();
		profileElementEntityCleanPixelData.setCodename("clean.pixel.data");
		profileElementEntityCleanPixelData.setName("nameCleanPixel");
		profileElementEntityCleanPixelData.setAction("ReplaceNull");
		profileElementEntityCleanPixelData.setPosition(3);

		profileElementEntities.add(profileElementEntityCleanPixelData);
		profileElementEntities.add(profileElementEntityAddBurnedAttr);
		profileElementEntities.add(profileElementEntityCleanPixelPerMachine);
		profileEntity.setProfileElementEntities(profileElementEntities);
		Profile profile = new Profile(profileEntity);

		// First object : should not match the mask application conditions
		boolean evaluation = profile.evaluateConditionCleanPixelData(attributes);
		String sopClassUID = attributes.getString(Tag.SOPClassUID);
		boolean cleanPixelAllowed = profile.isCleanPixelAllowedDependingImageType(attributes, sopClassUID);
		// As such the image doesn't comply with the requirements to apply a mask
		assertFalse(cleanPixelAllowed && evaluation);

		// Second object : should not match the mask application conditions
		evaluation = profile.evaluateConditionCleanPixelData(attributes2);
		sopClassUID = attributes2.getString(Tag.SOPClassUID);
		cleanPixelAllowed = profile.isCleanPixelAllowedDependingImageType(attributes2, sopClassUID);
		// As such the image doesn't comply with the requirements to apply a mask
		assertFalse(cleanPixelAllowed && evaluation);

		// Apply the profile that adds the BurnedInAnnotation attribute to both objects
		profile.applyAction(attributes, attributes, null, null, null, null);
		profile.applyAction(attributes2, attributes2, null, null, null, null);

		// The BurnedInAnnotation attribute is set to YES in the first object
		assertEquals("YES", attributes.getString(Tag.BurnedInAnnotation));
		// The BurnedInAnnotation attribute does not exist in the second object
		assertNull(attributes2.getString(Tag.BurnedInAnnotation));

		// First object : should match the mask application conditions
		evaluation = profile.evaluateConditionCleanPixelData(attributes);
		sopClassUID = attributes.getString(Tag.SOPClassUID);
		cleanPixelAllowed = profile.isCleanPixelAllowedDependingImageType(attributes, sopClassUID);
		// Since the attribute BurnedInAnnotation is set to YES,
		// the application conditions for the mask are fulfilled
		assertTrue(cleanPixelAllowed && evaluation);

	}

}
