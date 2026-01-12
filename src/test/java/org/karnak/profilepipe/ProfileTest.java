/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.profilepipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.data.entity.IncludedTagEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.model.profilepipe.HashContext;
import org.karnak.backend.service.profilepipe.Profile;
import org.karnak.backend.util.DicomObjectTools;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProfileTest {

	private static HMAC defaultHMAC;

	@BeforeAll
	static void beforeAll() {
		final byte[] HMAC_KEY = { 121, -7, 104, 11, 126, -39, -128, -126, 114, -94, 40, -67, 61, -45, 59, -53 };
		defaultHMAC = new HMAC(HMAC_KEY);
	}

	@Test
	void xPatientIDWithSequenceProfile() {
		final Attributes dataset1 = new Attributes();
		final Attributes dataset2 = new Attributes();

		dataset1.setString(Tag.PatientAge, VR.AS, "075Y");
		dataset1.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset1.setString(Tag.PatientID, VR.LO, "10987654321");
		Sequence dicomElemSeq1 = dataset1.newSequence(Tag.GroupOfPatientsIdentificationSequence, 1);
		final Attributes datasetSeq1 = new Attributes();
		datasetSeq1.setString(Tag.PatientID, VR.LO, "12345");
		Sequence dicomElemSeq12 = datasetSeq1.newSequence(Tag.IssuerOfPatientIDQualifiersSequence, 1);
		final Attributes datasetSeq12 = new Attributes();
		datasetSeq12.setString(Tag.UniversalEntityID, VR.UT, "UT");
		dicomElemSeq12.add(datasetSeq12);
		dicomElemSeq1.add(datasetSeq1);

		dataset2.setString(Tag.PatientAge, VR.AS, "075Y");
		dataset2.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset2.setString(Tag.PatientID, VR.LO, "10987654321");
		dataset2.remove(Tag.PatientID);
		Sequence dicomElemSeq2 = dataset2.newSequence(Tag.GroupOfPatientsIdentificationSequence, 1);
		final Attributes datasetSeq2 = new Attributes();
		datasetSeq2.setString(Tag.PatientID, VR.LO, "12345");
		datasetSeq2.remove(Tag.PatientID);
		Sequence dicomElemSeq22 = datasetSeq2.newSequence(Tag.IssuerOfPatientIDQualifiersSequence, 1);
		final Attributes datasetSeq22 = new Attributes();
		datasetSeq22.setString(Tag.UniversalEntityID, VR.UT, "UT");
		dicomElemSeq22.add(datasetSeq22);
		dicomElemSeq2.add(datasetSeq2);

		final ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		final ProfileElementEntity profileElementEntity1 = new ProfileElementEntity("Keep tag Source Group..",
				"action.on.specific.tags", null, "K", null, 0, profileEntity);
		profileElementEntity1.addIncludedTag(new IncludedTagEntity("(0010,0027)", profileElementEntity1));
		final ProfileElementEntity profileElementEntity2 = new ProfileElementEntity("Remove tag PatientID",
				"action.on.specific.tags", null, "X", null, 0, profileEntity);
		profileElementEntity2.addIncludedTag(new IncludedTagEntity("(0010,0020)", profileElementEntity2));

		profileEntity.addProfilePipe(profileElementEntity1);
		profileEntity.addProfilePipe(profileElementEntity2);
		Profile profile = new Profile(profileEntity);
		profile.applyAction(dataset1, dataset1, defaultHMAC, null, null, null);
		assertTrue(DicomObjectTools.dicomObjectEquals(dataset2, dataset1));
	}

	@Test
	void propagationInSequence1() {
		final Attributes dataset1 = new Attributes();
		final Attributes dataset2 = new Attributes();

		dataset1.setString(Tag.PatientAge, VR.AS, "075Y");
		dataset1.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset1.setString(Tag.PatientID, VR.LO, "10987654321");
		dataset1.setString(Tag.PatientName, VR.PN, "toto");
		dataset1.setString(Tag.PatientBirthDate, VR.DA, "20200101");
		dataset1.setString(Tag.PatientSex, VR.CS, "M");
		dataset1.setString(Tag.IssuerOfPatientID, VR.LO, "12345678910");
		Sequence dicomElemSeq1 = dataset1.newSequence(Tag.GroupOfPatientsIdentificationSequence, 1);
		final Attributes datasetSeq1 = new Attributes();
		datasetSeq1.setString(Tag.PatientID, VR.LO, "12345");
		Sequence dicomElemSeq12 = datasetSeq1.newSequence(Tag.IssuerOfPatientIDQualifiersSequence, 1);
		final Attributes datasetSeq12 = new Attributes();
		datasetSeq12.setString(Tag.UniversalEntityID, VR.UT, "UT");
		dicomElemSeq12.add(datasetSeq12);
		dicomElemSeq1.add(datasetSeq1);

		dataset2.setString(Tag.PatientAge, VR.AS, "075Y");
		dataset2.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset2.setString(Tag.PatientID, VR.LO, "10987654321");
		dataset2.setString(Tag.PatientName, VR.PN, "toto");
		dataset2.setString(Tag.PatientBirthDate, VR.DA, "20200101");
		dataset2.setString(Tag.PatientSex, VR.CS, "M");
		dataset2.setString(Tag.IssuerOfPatientID, VR.LO, "12345678910");
		dataset2.remove(Tag.PatientAge);
		dataset2.remove(Tag.StudyInstanceUID);
		dataset2.remove(Tag.PatientID);
		dataset2.remove(Tag.PatientName);
		dataset2.remove(Tag.PatientBirthDate);
		dataset2.remove(Tag.PatientSex);
		dataset2.remove(Tag.IssuerOfPatientID);
		Sequence dicomElemSeq2 = dataset2.newSequence(Tag.GroupOfPatientsIdentificationSequence, 1);
		final Attributes datasetSeq2 = new Attributes();
		datasetSeq2.setString(Tag.PatientID, VR.LO, "12345");
		Sequence dicomElemSeq22 = datasetSeq2.newSequence(Tag.IssuerOfPatientIDQualifiersSequence, 1);
		final Attributes datasetSeq22 = new Attributes();
		datasetSeq22.setString(Tag.UniversalEntityID, VR.UT, "UT");
		dicomElemSeq22.add(datasetSeq22);
		dicomElemSeq2.add(datasetSeq2);

		final ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		final ProfileElementEntity profileElementEntity1 = new ProfileElementEntity("Remove tag",
				"action.on.specific.tags", null, "X", null, 0, profileEntity);
		profileElementEntity1.addIncludedTag(new IncludedTagEntity("(0010,1010)", profileElementEntity1));
		final ProfileElementEntity profileElementEntity2 = new ProfileElementEntity("Keep tag",
				"action.on.specific.tags", null, "K", null, 1, profileEntity);
		profileElementEntity2.addIncludedTag(new IncludedTagEntity("(0010,0027)", profileElementEntity2));
		final ProfileElementEntity profileElementEntity3 = new ProfileElementEntity("Remove tag",
				"action.on.specific.tags", null, "X", null, 2, profileEntity);
		profileElementEntity3.addIncludedTag(new IncludedTagEntity("(xxxx,xxxx)", profileElementEntity3));

		profileEntity.addProfilePipe(profileElementEntity1);
		profileEntity.addProfilePipe(profileElementEntity2);
		profileEntity.addProfilePipe(profileElementEntity3);
		Profile profile = new Profile(profileEntity);
		profile.applyAction(dataset1, dataset1, defaultHMAC, null, null, null);
		assertTrue(DicomObjectTools.dicomObjectEquals(dataset2, dataset1));
	}

	@Test
	void propagationInSequence2() {
		final Attributes dataset1 = new Attributes();
		final Attributes dataset2 = new Attributes();

		dataset1.setString(Tag.PatientAge, VR.AS, "075Y");
		dataset1.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset1.setString(Tag.PatientID, VR.LO, "10987654321");
		dataset1.setString(Tag.PatientName, VR.PN, "toto");
		dataset1.setString(Tag.PatientBirthDate, VR.DA, "20200101");
		dataset1.setString(Tag.PatientSex, VR.CS, "M");
		dataset1.setString(Tag.IssuerOfPatientID, VR.LO, "12345678910");
		Sequence dicomElemSeq1 = dataset1.newSequence(Tag.GroupOfPatientsIdentificationSequence, 1);
		final Attributes datasetSeq1 = new Attributes();
		datasetSeq1.setString(Tag.PatientID, VR.LO, "12345");

		Sequence dicomElemSeq12 = datasetSeq1.newSequence(Tag.IssuerOfPatientIDQualifiersSequence, 1);
		final Attributes datasetSeq12 = new Attributes();
		datasetSeq12.setString(Tag.UniversalEntityID, VR.UT, "UT");
		dicomElemSeq12.add(datasetSeq12);
		dicomElemSeq1.add(datasetSeq1);

		dataset2.setString(Tag.PatientAge, VR.AS, "075Y");
		dataset2.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset2.setString(Tag.PatientID, VR.LO, "10987654321");
		dataset2.setString(Tag.PatientName, VR.PN, "toto");
		dataset2.setString(Tag.PatientBirthDate, VR.DA, "20200101");
		dataset2.setString(Tag.PatientSex, VR.CS, "M");
		dataset2.setString(Tag.IssuerOfPatientID, VR.LO, "12345678910");

		dataset2.remove(Tag.PatientAge);
		dataset2.remove(Tag.StudyInstanceUID);
		dataset2.remove(Tag.PatientID);
		dataset2.remove(Tag.PatientName);
		dataset2.remove(Tag.PatientBirthDate);
		dataset2.remove(Tag.PatientSex);
		dataset2.remove(Tag.IssuerOfPatientID);

		Sequence dicomElemSeq2 = dataset2.newSequence(Tag.GroupOfPatientsIdentificationSequence, 1);
		final Attributes datasetSeq2 = new Attributes();
		datasetSeq2.setString(Tag.PatientID, VR.LO, "12345");
		Sequence dicomElemSeq22 = datasetSeq2.newSequence(Tag.IssuerOfPatientIDQualifiersSequence, 1);
		final Attributes datasetSeq22 = new Attributes();
		datasetSeq22.setString(Tag.UniversalEntityID, VR.UT, "UT");
		datasetSeq22.remove(Tag.UniversalEntityID);
		dicomElemSeq22.add(datasetSeq22);
		dicomElemSeq2.add(datasetSeq2);

		final ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		final ProfileElementEntity profileElementEntity1 = new ProfileElementEntity("Remove tag",
				"action.on.specific.tags", null, "X", null, 0, profileEntity);
		profileElementEntity1.addIncludedTag(new IncludedTagEntity("(0010,1010)", profileElementEntity1));
		profileElementEntity1.addIncludedTag(new IncludedTagEntity("(0040,0032)", profileElementEntity1));
		final ProfileElementEntity profileElementEntity2 = new ProfileElementEntity("Keep tag",
				"action.on.specific.tags", null, "K", null, 1, profileEntity);
		profileElementEntity2.addIncludedTag(new IncludedTagEntity("(0010,0027)", profileElementEntity2));
		final ProfileElementEntity profileElementEntity3 = new ProfileElementEntity("Remove tag",
				"action.on.specific.tags", null, "X", null, 2, profileEntity);
		profileElementEntity3.addIncludedTag(new IncludedTagEntity("(xxxx,xxxx)", profileElementEntity3));

		profileEntity.addProfilePipe(profileElementEntity1);
		profileEntity.addProfilePipe(profileElementEntity2);
		profileEntity.addProfilePipe(profileElementEntity3);
		Profile profile = new Profile(profileEntity);
		profile.applyAction(dataset1, dataset1, defaultHMAC, null, null, null);
		assertTrue(DicomObjectTools.dicomObjectEquals(dataset2, dataset1));
	}

	@Test
	void propagationInSequence3() {
		final Attributes dataset1 = new Attributes();
		final Attributes dataset2 = new Attributes();

		dataset1.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset1.setString(Tag.PatientID, VR.LO, "10987654321");
		dataset1.setString(Tag.PatientName, VR.PN, "toto");
		dataset1.setString(Tag.PatientBirthDate, VR.DA, "20200101");
		dataset1.setString(Tag.PatientSex, VR.CS, "M");
		dataset1.setString(Tag.IssuerOfPatientID, VR.LO, "12345678910");
		dataset1.setString(Tag.PatientAge, VR.AS, "075Y");
		Sequence dicomElemSeq1 = dataset1.newSequence(Tag.CTExposureSequence, 1);
		final Attributes datasetSeq1 = new Attributes();
		datasetSeq1.setDouble(Tag.EstimatedDoseSaving, VR.FD, 0d);
		datasetSeq1.setDouble(Tag.ExposureTimeInms, VR.FD, 2.099d);
		datasetSeq1.setDouble(Tag.XRayTubeCurrentInmA, VR.FD, 381d);
		datasetSeq1.setDouble(Tag.ExposureInmAs, VR.FD, 800d);
		datasetSeq1.setDouble(Tag.CTDIvol, VR.FD, 47d);
		dicomElemSeq1.add(datasetSeq1);

		dataset2.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset2.setString(Tag.PatientID, VR.LO, "10987654321");
		dataset2.setString(Tag.PatientName, VR.PN, "toto");
		dataset2.setString(Tag.PatientBirthDate, VR.DA, "20190101");
		dataset2.setString(Tag.PatientSex, VR.CS, "M");
		dataset2.setString(Tag.IssuerOfPatientID, VR.LO, "12345678910");
		dataset2.setString(Tag.PatientAge, VR.AS, "076Y");
		Sequence dicomElemSeq2 = dataset2.newSequence(Tag.CTExposureSequence, 1);
		final Attributes datasetSeq2 = new Attributes();
		datasetSeq2.setDouble(Tag.EstimatedDoseSaving, VR.FD, 0d);
		datasetSeq2.remove(Tag.EstimatedDoseSaving);
		datasetSeq2.setDouble(Tag.ExposureTimeInms, VR.FD, 2.099d);
		datasetSeq2.setDouble(Tag.XRayTubeCurrentInmA, VR.FD, 381d);
		datasetSeq2.setDouble(Tag.ExposureInmAs, VR.FD, 800d);
		datasetSeq2.setDouble(Tag.CTDIvol, VR.FD, 47d);
		dicomElemSeq2.add(datasetSeq2);

		final ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");

		final ProfileElementEntity profileElementEntity1 = new ProfileElementEntity("Shift Date with argumentEntities",
				"action.on.dates", null, null, "shift", 0, profileEntity);
		profileElementEntity1.addIncludedTag(new IncludedTagEntity("(xxxx,xxxx)", profileElementEntity1));
		profileElementEntity1.addArgument(new ArgumentEntity("seconds", "60", profileElementEntity1));
		profileElementEntity1.addArgument(new ArgumentEntity("days", "365", profileElementEntity1));

		final ProfileElementEntity profileElementEntity2 = new ProfileElementEntity("Remove tag",
				"action.on.specific.tags", null, "X", null, 1, profileEntity);
		profileElementEntity2.addIncludedTag(new IncludedTagEntity("(0018,9324)", profileElementEntity2));

		final ProfileElementEntity profileElementEntity3 = new ProfileElementEntity("Keep tag",
				"action.on.specific.tags", null, "K", null, 2, profileEntity);
		profileElementEntity3.addIncludedTag(new IncludedTagEntity("(0018,9321)", profileElementEntity3));

		final ProfileElementEntity profileElementEntity4 = new ProfileElementEntity("Replace null",
				"action.on.specific.tags", null, "Z", null, 3, profileEntity);
		profileElementEntity4.addIncludedTag(new IncludedTagEntity("(0018,9330)", profileElementEntity2));

		profileEntity.addProfilePipe(profileElementEntity1);
		profileEntity.addProfilePipe(profileElementEntity2);
		profileEntity.addProfilePipe(profileElementEntity3);
		profileEntity.addProfilePipe(profileElementEntity4);
		Profile profile = new Profile(profileEntity);
		profile.applyAction(dataset1, dataset1, defaultHMAC, null, null, null);
		assertTrue(DicomObjectTools.dicomObjectEquals(dataset2, dataset1));
	}

	@Test
	void propagationInSequence4() {
		final Attributes dataset1 = new Attributes();
		final Attributes dataset2 = new Attributes();

		dataset1.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset1.setString(Tag.PatientID, VR.LO, "10987654321");
		dataset1.setString(Tag.PatientName, VR.PN, "toto");
		dataset1.setString(Tag.PatientBirthDate, VR.DA, "20200101");
		dataset1.setString(Tag.PatientSex, VR.CS, "M");
		dataset1.setString(Tag.IssuerOfPatientID, VR.LO, "12345678910");
		dataset1.setString(Tag.PatientAge, VR.AS, "075Y");
		Sequence dicomElemSeq1 = dataset1.newSequence(Tag.ReferencedImageSequence, 1);
		final Attributes datasetSeq1 = new Attributes();
		datasetSeq1.setString(Tag.ReferencedSOPClassUID, VR.UI, "12345");
		datasetSeq1.setString(Tag.ReferencedFrameNumber, VR.UI, "12345");
		Sequence dicomElemSeq12 = datasetSeq1.newSequence(Tag.PurposeOfReferenceCodeSequence, 1);
		dicomElemSeq1.add(datasetSeq1);
		final Attributes datasetSeq12 = new Attributes();
		datasetSeq12.setString(Tag.CodeValue, VR.SH, "1111");
		datasetSeq12.setString(Tag.CodingSchemeDesignator, VR.SH, "1111");
		dicomElemSeq12.add(datasetSeq12);

		dataset2.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset2.setString(Tag.PatientID, VR.LO, "10987654321");
		dataset2.setString(Tag.PatientName, VR.PN, "toto");
		dataset2.setString(Tag.PatientBirthDate, VR.DA, "20200101");
		dataset2.setString(Tag.PatientSex, VR.CS, "M");
		dataset2.setString(Tag.IssuerOfPatientID, VR.LO, "12345678910");
		dataset2.setString(Tag.PatientAge, VR.AS, "075Y");
		Sequence dicomElemSeq2 = dataset2.newSequence(Tag.ReferencedImageSequence, 1);
		final Attributes datasetSeq2 = new Attributes();
		datasetSeq2.setString(Tag.ReferencedSOPClassUID, VR.UI, "2.25.278659998382609075216063956388837522977");
		datasetSeq2.setString(Tag.ReferencedFrameNumber, VR.UI, "2.25.278659998382609075216063956388837522977");
		Sequence dicomElemSeq22 = datasetSeq2.newSequence(Tag.PurposeOfReferenceCodeSequence, 1);
		dicomElemSeq2.add(datasetSeq2);
		final Attributes datasetSeq22 = new Attributes();
		datasetSeq22.setString(Tag.CodeValue, VR.UI, "2.25.313557030369376654019214873435380124495");
		datasetSeq22.setString(Tag.CodingSchemeDesignator, VR.UI, "2.25.313557030369376654019214873435380124495");
		dicomElemSeq22.add(datasetSeq22);

		final ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");

		final ProfileElementEntity profileElementEntity1 = new ProfileElementEntity("UID", "action.on.specific.tags",
				null, "U", null, 0, profileEntity);
		profileElementEntity1.addIncludedTag(new IncludedTagEntity("(0008,1140)", profileElementEntity1));

		profileEntity.addProfilePipe(profileElementEntity1);
		Profile profile = new Profile(profileEntity);
		profile.applyAction(dataset1, dataset1, defaultHMAC, null, null, null);
		assertTrue(DicomObjectTools.dicomObjectEquals(dataset2, dataset1));
	}

	@Test
	void xProfile() {
		final Attributes dataset1 = new Attributes();
		final Attributes dataset2 = new Attributes();

		dataset1.setString(Tag.PatientName, VR.PN, "TEST-Expr-AddAction");
		dataset1.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset1.setString(Tag.PatientAge, VR.AS, "075Y");

		dataset2.setString(Tag.PatientName, VR.PN, "TEST-Expr-AddAction");
		dataset2.setString(Tag.StudyInstanceUID, VR.UI, "12345");

		final ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		final ProfileElementEntity profileElementEntity = new ProfileElementEntity("Remove tag",
				"action.on.specific.tags", null, "X", null, 0, profileEntity);
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(0010,1010)", profileElementEntity));
		profileEntity.addProfilePipe(profileElementEntity);
		Profile profile = new Profile(profileEntity);
		profile.applyAction(dataset1, dataset1, defaultHMAC, null, null, null);
		assertTrue(DicomObjectTools.dicomObjectEquals(dataset2, dataset1));
	}

	@Test
	void zProfile() {
		final Attributes dataset1 = new Attributes();
		final Attributes dataset2 = new Attributes();

		dataset1.setString(Tag.PatientName, VR.PN, "TEST-Expr-AddAction");
		dataset1.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset1.setString(Tag.PatientAge, VR.AS, "075Y");

		dataset2.setString(Tag.PatientName, VR.PN, "TEST-Expr-AddAction");
		dataset2.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset2.setNull(Tag.PatientAge, VR.AS);

		ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		ProfileElementEntity profileElementEntity = new ProfileElementEntity("Replace tag by null",
				"action.on.specific.tags", null, "Z", null, 0, profileEntity);
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(0010,1010)", profileElementEntity));
		profileEntity.addProfilePipe(profileElementEntity);
		Profile profile = new Profile(profileEntity);
		profile.applyAction(dataset1, dataset1, defaultHMAC, null, null, null);
		assertTrue(DicomObjectTools.dicomObjectEquals(dataset2, dataset1));
	}

	@Test
	void shiftDateProfile() {
		// SHIFT days: 365, seconds:60
		final Attributes dataset1 = new Attributes();
		final Attributes dataset2 = new Attributes();

		dataset1.setString(Tag.PatientName, VR.PN, "TEST-Expr-AddAction");
		dataset1.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset1.setString(Tag.PatientAge, VR.AS, "069Y");
		dataset1.setString(Tag.PatientBirthDate, VR.DA, "20080822");
		dataset1.setString(Tag.AcquisitionDateTime, VR.DT, "20080729131503");
		dataset1.setString(Tag.InstanceCreationTime, VR.TM, "131735.000000");

		dataset2.setString(Tag.PatientName, VR.PN, "TEST-Expr-AddAction");
		dataset2.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset2.setString(Tag.PatientAge, VR.AS, "070Y");
		dataset2.setString(Tag.PatientBirthDate, VR.DA, "20070823");
		dataset2.setString(Tag.AcquisitionDateTime, VR.DT, "20070730131403.000000");
		dataset2.setString(Tag.InstanceCreationTime, VR.TM, "131635.000000");

		ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		ProfileElementEntity profileElementEntity = new ProfileElementEntity("Shift Date with argumentEntities",
				"action.on.dates", null, null, "shift", 0, profileEntity);
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(xxxx,xxxx)", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("seconds", "60", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("days", "365", profileElementEntity));
		profileEntity.addProfilePipe(profileElementEntity);
		Profile profile = new Profile(profileEntity);
		profile.applyAction(dataset1, dataset1, defaultHMAC, null, null, null);
		assertTrue(DicomObjectTools.dicomObjectEquals(dataset2, dataset1));
	}

	@Test
	void shiftRangeProfile() {
		// SHIFT range with hmackey: HmacKeyToTEST -> days: 57, seconds: 9
		final String projectSecret = "xN[LtKL!H5RUuQ}6";
		byte[] HMAC_KEY = { 85, 55, -40, -90, -102, 57, -5, -89, -77, -86, 22, -64, 89, -36, 2, 50 };
		final String PatientID = "TEST-SHIFT-RANGE";
		final HashContext hashContext = new HashContext(HMAC_KEY, PatientID);
		final HMAC hmac = new HMAC(hashContext);
		final Attributes dataset1 = new Attributes();
		final Attributes dataset2 = new Attributes();

		dataset1.setString(Tag.PatientName, VR.PN, "TEST-Expr-AddAction");
		dataset1.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset1.setString(Tag.PatientAge, VR.AS, "069Y");
		dataset1.setString(Tag.PatientBirthDate, VR.DA, "20080822");
		dataset1.setString(Tag.AcquisitionDateTime, VR.DT, "20080729131503");
		dataset1.setString(Tag.InstanceCreationTime, VR.TM, "131735.000000");

		dataset2.setString(Tag.PatientName, VR.PN, "TEST-Expr-AddAction");
		dataset2.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset2.setString(Tag.PatientAge, VR.AS, "069Y");
		dataset2.setString(Tag.PatientBirthDate, VR.DA, "20080626");
		dataset2.setString(Tag.AcquisitionDateTime, VR.DT, "20080602131454.000000");
		dataset2.setString(Tag.InstanceCreationTime, VR.TM, "131726.000000");

		ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		ProfileElementEntity profileElementEntity = new ProfileElementEntity("Shift Date with argumentEntities",
				"action.on.dates", null, null, "shift_range", 0, profileEntity);
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(xxxx,xxxx)", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("max_seconds", "60", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("min_days", "50", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("max_days", "100", profileElementEntity));

		profileEntity.addProfilePipe(profileElementEntity);
		Profile profile = new Profile(profileEntity);
		profile.applyAction(dataset1, dataset1, hmac, null, null, null);
		assertTrue(DicomObjectTools.dicomObjectEquals(dataset2, dataset1));
	}

	@Test
	void xandZProfile() {
		final Attributes dataset1 = new Attributes();
		final Attributes dataset2 = new Attributes();

		dataset1.setString(Tag.PatientName, VR.PN, "TEST-Expr-AddAction");
		dataset1.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset1.setString(Tag.PatientAge, VR.AS, "075Y");

		dataset2.setNull(Tag.PatientName, VR.PN);
		dataset2.setNull(Tag.StudyInstanceUID, VR.UI);
		dataset2.setString(Tag.PatientAge, VR.AS, "075Y");
		dataset2.remove(Tag.PatientAge);

		final ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		final ProfileElementEntity profileElementEntity = new ProfileElementEntity("Remove tag",
				"action.on.specific.tags", null, "X", null, 0, profileEntity);
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(0010,1010)", profileElementEntity));
		profileEntity.addProfilePipe(profileElementEntity);
		final ProfileElementEntity profileElementEntity2 = new ProfileElementEntity("Replace by null",
				"action.on.specific.tags", null, "Z", null, 1, profileEntity);
		profileElementEntity2.addIncludedTag(new IncludedTagEntity("(xxxx,xxxx)", profileElementEntity2));
		profileEntity.addProfilePipe(profileElementEntity2);
		Profile profile = new Profile(profileEntity);
		profile.applyAction(dataset1, dataset1, defaultHMAC, null, null, null);
		assertTrue(DicomObjectTools.dicomObjectEquals(dataset2, dataset1));
	}

	@Test
	void kPrivateTagsAndXProfile() {
		final Attributes dataset1 = new Attributes();
		final Attributes dataset2 = new Attributes();

		dataset1.setString(Tag.PatientName, VR.PN, "TEST-Expr-AddAction");
		dataset1.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset1.setString(Tag.PatientAge, VR.AS, "069Y");
		dataset1.setString(Tag.PatientBirthDate, VR.DA, "20080822");
		dataset1.setString(Tag.AcquisitionDateTime, VR.DT, "20080729131503");
		dataset1.setString(Tag.InstanceCreationTime, VR.TM, "131735.000000");
		dataset1.setString(0x70531200, VR.LO, "Private TagEntity");
		dataset1.setString(0x70534200, VR.LO, "Private TagEntity");
		dataset1.setString(0x70531209, VR.LO, "Private TagEntity");
		dataset1.setString(0x70534209, VR.LO, "Private TagEntity");
		dataset1.setString(0x70534205, VR.LO, "Private TagEntity"); // it's a private tag
																	// but it's not in
																	// scope

		dataset2.setString(Tag.PatientName, VR.PN, "TEST-Expr-AddAction");
		dataset2.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset2.setString(Tag.PatientAge, VR.AS, "069Y");
		dataset2.setString(Tag.PatientBirthDate, VR.DA, "20080822");
		dataset2.setString(Tag.AcquisitionDateTime, VR.DT, "20080729131503");
		dataset2.setString(Tag.InstanceCreationTime, VR.TM, "131735.000000");
		dataset2.setString(0x70531200, VR.LO, "Private TagEntity");
		dataset2.setString(0x70534200, VR.LO, "Private TagEntity");
		dataset2.setString(0x70531209, VR.LO, "Private TagEntity");
		dataset2.setString(0x70534209, VR.LO, "Private TagEntity");
		dataset2.remove(Tag.PatientName);
		dataset2.remove(Tag.StudyInstanceUID);
		dataset2.remove(Tag.PatientAge);
		dataset2.remove(Tag.PatientBirthDate);
		dataset2.remove(Tag.AcquisitionDateTime);
		dataset2.remove(Tag.InstanceCreationTime);

		final ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		final ProfileElementEntity profileElementEntity = new ProfileElementEntity("Keep tag", "action.on.privatetags",
				null, "K", null, 0, profileEntity);
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(7053,xx00)", profileElementEntity));
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(7053,xx09)", profileElementEntity));
		profileEntity.addProfilePipe(profileElementEntity);
		final ProfileElementEntity profileElementEntity2 = new ProfileElementEntity("Remove tag",
				"action.on.specific.tags", null, "X", null, 1, profileEntity);
		profileElementEntity2.addIncludedTag(new IncludedTagEntity("(xxxx,xxxx)", profileElementEntity2));
		profileEntity.addProfilePipe(profileElementEntity2);
		Profile profile = new Profile(profileEntity);
		profile.applyAction(dataset1, dataset1, defaultHMAC, null, null, null);
		assertTrue(DicomObjectTools.dicomObjectEquals(dataset2, dataset1));
	}

	@Test
	void expressionProfile() {
		final Attributes dataset1 = new Attributes();
		final Attributes dataset2 = new Attributes();

		dataset1.setString(Tag.PatientName, VR.PN, "TEST-Expr-AddAction");
		dataset1.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset1.setString(Tag.PatientAge, VR.AS, "075Y");

		dataset2.setString(Tag.PatientName, VR.PN, "TEST-Expr-AddAction");
		dataset2.setString(Tag.StudyInstanceUID, VR.UI, "12345");

		ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		ProfileElementEntity profileElementEntity = new ProfileElementEntity("Expr", "expression.on.tags", null, null,
				null, 0, profileEntity);
		profileElementEntity
			.addArgument(new ArgumentEntity("expr", "stringValue == '075Y'? Remove() : Keep()", profileElementEntity));
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(xxxx,xxxx)", profileElementEntity));

		profileEntity.addProfilePipe(profileElementEntity);
		Profile profile = new Profile(profileEntity);
		profile.applyAction(dataset1, dataset1, defaultHMAC, null, null, null);
		assertTrue(DicomObjectTools.dicomObjectEquals(dataset2, dataset1));
	}

	@Test
	void expressionProfileComputePatientAge() {
		final Attributes dataset1 = new Attributes();

		dataset1.setString(Tag.PatientName, VR.PN, "TEST-Expr-ComputePatientName");
		dataset1.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset1.setString(Tag.PatientBirthDate, VR.DA, "20080822");
		dataset1.setString(Tag.PatientAge, VR.AS, "");
		dataset1.setString(Tag.AcquisitionDate, VR.DA, "20250512");

		ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		ProfileElementEntity profileElementEntity = new ProfileElementEntity("Expr", "expression.on.tags", null, null,
				null, 0, profileEntity);
		profileElementEntity.addArgument(new ArgumentEntity("expr", "ComputePatientAge()", profileElementEntity));
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(0010,1010)", profileElementEntity));

		profileEntity.addProfilePipe(profileElementEntity);
		Profile profile = new Profile(profileEntity);
		profile.applyAction(dataset1, dataset1, defaultHMAC, null, null, null);
		assertEquals("016Y", dataset1.getString(Tag.PatientAge));
	}

	@Test
	void expressionProfileComputePatientAge_missingAcquisitionDate() {
		final Attributes dataset1 = new Attributes();

		dataset1.setString(Tag.PatientName, VR.PN, "TEST-Expr-ComputePatientName");
		dataset1.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset1.setString(Tag.PatientBirthDate, VR.DA, "20080822");
		dataset1.setString(Tag.PatientAge, VR.AS, "");

		ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		ProfileElementEntity profileElementEntity = new ProfileElementEntity("Expr", "expression.on.tags", null, null,
				null, 0, profileEntity);
		profileElementEntity.addArgument(new ArgumentEntity("expr", "ComputePatientAge()", profileElementEntity));
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(0010,1010)", profileElementEntity));

		profileEntity.addProfilePipe(profileElementEntity);
		Profile profile = new Profile(profileEntity);
		profile.applyAction(dataset1, dataset1, defaultHMAC, null, null, null);
		assertNull(dataset1.getString(Tag.PatientAge));
	}

	@Test
	void expressionProfileComputePatientAge_overwriteExistingAge() {
		final Attributes dataset1 = new Attributes();

		dataset1.setString(Tag.PatientName, VR.PN, "TEST-Expr-ComputePatientName");
		dataset1.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset1.setString(Tag.PatientBirthDate, VR.DA, "20080822");
		dataset1.setString(Tag.PatientAge, VR.AS, "027Y");
		dataset1.setString(Tag.AcquisitionDate, VR.DA, "20250512");

		ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		ProfileElementEntity profileElementEntity = new ProfileElementEntity("Expr", "expression.on.tags", null, null,
				null, 0, profileEntity);
		profileElementEntity.addArgument(new ArgumentEntity("expr", "ComputePatientAge()", profileElementEntity));
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(0010,1010)", profileElementEntity));

		profileEntity.addProfilePipe(profileElementEntity);
		Profile profile = new Profile(profileEntity);
		profile.applyAction(dataset1, dataset1, defaultHMAC, null, null, null);
		// The patient age is voluntarily incorrect, should be replaced by the computed
		// value i.e. 016Y
		assertEquals("016Y", dataset1.getString(Tag.PatientAge));
	}

}
