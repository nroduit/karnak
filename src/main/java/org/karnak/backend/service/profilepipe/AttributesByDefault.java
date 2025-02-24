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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.dicom.DateTimeUtils;
import org.karnak.backend.enums.ProfileItemType;

public class AttributesByDefault {

	private AttributesByDefault() {
	}

	public static void setDeidentificationMethodCodeSequence(Attributes dcm, ProjectEntity projectEntity) {
		final ProfileEntity profileEntity = projectEntity.getProfileEntity();
		final List<ProfileElementEntity> profileElementEntities = profileEntity.getProfileElementEntities()
			.stream()
			.sorted(Comparator.comparing(ProfileElementEntity::getPosition))
			.collect(Collectors.toList());

		Sequence deidentificationMethodSequence = dcm.newSequence(Tag.DeidentificationMethodCodeSequence,
				profileElementEntities.size());
		Set<String> listCodeValue = new HashSet<>();

		profileElementEntities.forEach(profileElementEntity -> {
			String codeValue = ProfileItemType.getCodeValue(profileElementEntity.getCodename());
			String codeMeaning = ProfileItemType.getCodeMeaning(profileElementEntity.getCodename());
			if (codeValue != null && listCodeValue.stream().noneMatch(s -> s.equals(codeValue))) {
				Attributes attributes = new Attributes(dcm.bigEndian());
				attributes.setString(Tag.CodeValue, VR.SH, codeValue);
				attributes.setString(Tag.CodingSchemeDesignator, VR.SH, "DCM");
				attributes.setString(Tag.CodeMeaning, VR.LO, codeMeaning);
				deidentificationMethodSequence.add(attributes);
				listCodeValue.add(codeValue);
			}
		});
	}

	public static void setPatientModule(Attributes dcm, String newPatientID, String newPatientName,
			ProjectEntity projectEntity) {
		dcm.setString(Tag.PatientID, VR.LO, newPatientID);
		dcm.setString(Tag.PatientName, VR.PN, newPatientName);
		dcm.setString(Tag.PatientIdentityRemoved, VR.CS, "YES");
		dcm.remove(Tag.DeidentificationMethod);
		setDeidentificationMethodCodeSequence(dcm, projectEntity);
	}

	public static void setSOPCommonModule(Attributes dcm) {
		final LocalDateTime now = LocalDateTime.now(ZoneId.of("CET"));
		dcm.setString(Tag.InstanceCreationDate, VR.DA, DateTimeUtils.formatDA(now));
		dcm.setString(Tag.InstanceCreationTime, VR.TM, DateTimeUtils.formatTM(now));
	}

	public static void setClinicalTrialAttributes(Attributes dcm, ProjectEntity projectEntity, String pseudonym) {
		final ProfileEntity profileEntity = projectEntity.getProfileEntity();

		// Clinical Trial Subject - Module
		dcm.setString(Tag.ClinicalTrialSponsorName, VR.LO, projectEntity.getName());
		dcm.setString(Tag.ClinicalTrialProtocolID, VR.LO, profileEntity.getName());
		dcm.setNull(Tag.ClinicalTrialProtocolName, VR.LO);
		dcm.setNull(Tag.ClinicalTrialSiteID, VR.LO);
		dcm.setNull(Tag.ClinicalTrialSiteName, VR.LO);
		dcm.setString(Tag.ClinicalTrialSubjectID, VR.LO, pseudonym);
		dcm.remove(Tag.ClinicalTrialSubjectReadingID);
		dcm.remove(Tag.ClinicalTrialProtocolEthicsCommitteeName);
		dcm.remove(Tag.ClinicalTrialProtocolEthicsCommitteeApprovalNumber);
	}

}
