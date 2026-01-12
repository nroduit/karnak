/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.IncludedTagEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.data.entity.SecretEntity;
import org.karnak.backend.enums.PseudonymType;
import org.springframework.boot.test.context.SpringBootTest;
import org.weasis.dicom.param.AttributeEditorContext;
import org.weasis.dicom.param.DicomNode;

@SpringBootTest
class DeIdentifyEditorTest {

	@Test
	void should_apply_to_dicom_object() {
		// Init data
		Attributes attributes = new Attributes();
		DicomNode source = new DicomNode("source");
		DicomNode destination = new DicomNode("destination");
		AttributeEditorContext attributeEditorContext = new AttributeEditorContext("tsuid", source, destination);
		DestinationEntity destinationEntity = new DestinationEntity();
		ProfileEntity profileEntity = new ProfileEntity();
		ProjectEntity projectEntity = new ProjectEntity();
		projectEntity.setProfileEntity(profileEntity);
		destinationEntity.setDeIdentificationProjectEntity(projectEntity);
		destinationEntity.setPseudonymType(PseudonymType.EXTID_IN_TAG);
		destinationEntity.setTag("0008,0080");
		destinationEntity.setSavePseudonym(false);
		byte[] tabByte = new byte[16];
		tabByte[0] = 1;
		projectEntity.addActiveSecretEntity(new SecretEntity(tabByte));
		attributes.setString(Tag.PatientID, VR.SH, "patientID");
		attributes.setString(Tag.SeriesInstanceUID, VR.SH, "seriesInstanceUID");
		attributes.setString(Tag.SOPInstanceUID, VR.SH, "sopInstanceUID");
		attributes.setString(Tag.IssuerOfPatientID, VR.SH, "issuerOfPatientID");
		attributes.setString(Tag.PixelData, VR.SH, "pixelData");
		attributes.setString(Tag.SOPClassUID, VR.SH, "1.2.840.10008.5.1.4.1.1.88.74");
		attributes.setString(Tag.BurnedInAnnotation, VR.SH, "YES");
		attributes.setString(Tag.StationName, VR.SH, "stationName");
		attributes.setString(524416, VR.SH, "pseudonym");
		DeIdentifyEditor deIdentifyEditor = new DeIdentifyEditor(destinationEntity);

		// Call method
		deIdentifyEditor.apply(attributes, attributeEditorContext);

		// Test results
		assertEquals("NONE", attributeEditorContext.getAbort().name());
		assertNull(attributeEditorContext.getMaskArea());
	}

	@Test
	void should_exclude_instance() {
		// Init data
		Attributes attributes = new Attributes();
		DicomNode source = new DicomNode("source");
		DicomNode destination = new DicomNode("destination");
		AttributeEditorContext attributeEditorContext = new AttributeEditorContext("tsuid", source, destination);
		DestinationEntity destinationEntity = new DestinationEntity();
		ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		ProjectEntity projectEntity = new ProjectEntity();
		projectEntity.setProfileEntity(profileEntity);
		destinationEntity.setDeIdentificationProjectEntity(projectEntity);
		destinationEntity.setPseudonymType(PseudonymType.EXTID_IN_TAG);
		destinationEntity.setTag("0008,0080");
		destinationEntity.setSavePseudonym(false);
		byte[] tabByte = new byte[16];
		tabByte[0] = 1;

		ProfileElementEntity profileElementEntity = new ProfileElementEntity("Expr", "expression.on.tags", null, null,
				null, 0, profileEntity);
		profileElementEntity.addArgument(new ArgumentEntity("expr",
				"getString(#Tag.BurnedInAnnotation) == 'YES' ? ExcludeInstance() : null", profileElementEntity));
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(xxxx,xxxx)", profileElementEntity));

		profileEntity.addProfilePipe(profileElementEntity);

		projectEntity.addActiveSecretEntity(new SecretEntity(tabByte));
		attributes.setString(Tag.PatientID, VR.SH, "patientID");
		attributes.setString(Tag.SeriesInstanceUID, VR.SH, "seriesInstanceUID");
		attributes.setString(Tag.SOPInstanceUID, VR.SH, "sopInstanceUID");
		attributes.setString(Tag.IssuerOfPatientID, VR.SH, "issuerOfPatientID");
		attributes.setString(Tag.PixelData, VR.SH, "pixelData");
		attributes.setString(Tag.SOPClassUID, VR.SH, "1.2.840.10008.5.1.4.1.1.88.74");
		attributes.setString(Tag.BurnedInAnnotation, VR.SH, "YES");
		attributes.setString(Tag.StationName, VR.SH, "stationName");
		attributes.setString(524416, VR.SH, "pseudonym");
		DeIdentifyEditor deIdentifyEditor = new DeIdentifyEditor(destinationEntity);

		// Call method
		deIdentifyEditor.apply(attributes, attributeEditorContext);

		// Test results
		assertEquals(AttributeEditorContext.Abort.FILE_EXCEPTION, attributeEditorContext.getAbort());
		assertEquals("Instance excluded by profile: Expr", attributeEditorContext.getAbortMessage());
	}

}
