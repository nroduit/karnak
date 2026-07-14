/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profilepipe;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;

/**
 * Defines which DICOM tags are considered "sensitive" and should be checked by the
 * external de-identification image API.
 */
public final class SensitiveTagDefinition {

	/**
	 * Simple record that pairs a human-readable tag name with its dcm4che integer tag.
	 *
	 * @param name the key that will appear in the JSON sent to the API
	 * @param tag the dcm4che Tag constant (e.g. {@code Tag.PatientName})
	 */
	public record SensitiveTag(String name, int tag) {
	}

	/**
	 * The list of DICOM tags whose values should be sent to the de-identification image
	 * API so it can detect if they appear burned into the pixel data.
	 */
	public static final List<SensitiveTag> SENSITIVE_TAGS = List.of(
			new SensitiveTag("AccessionNumber", Tag.AccessionNumber),
			new SensitiveTag("InstitutionName", Tag.InstitutionName),
			new SensitiveTag("OperatorsName", Tag.OperatorsName), new SensitiveTag("PatientAge", Tag.PatientAge),
			new SensitiveTag("PatientBirthDate", Tag.PatientBirthDate), new SensitiveTag("PatientID", Tag.PatientID),
			new SensitiveTag("PatientName", Tag.PatientName), new SensitiveTag("PatientSex", Tag.PatientSex),
			new SensitiveTag("PerformingPhysicianName", Tag.PerformingPhysicianName),
			new SensitiveTag("PerformedProcedureStepID", Tag.PerformedProcedureStepID),
			new SensitiveTag("ReferringPhysicianName", Tag.ReferringPhysicianName),
			new SensitiveTag("StudyDate", Tag.StudyDate));

	private SensitiveTagDefinition() {
	}

	/**
	 * Reads every sensitive tag value from the given DICOM attributes and returns them as
	 * a name→value map.
	 * @param dcm the DICOM attributes to read from
	 * @return an ordered map of tag name → tag value for all sensitive tags
	 * @see #SENSITIVE_TAGS
	 */
	public static Map<String, String> extractSensitiveData(Attributes dcm) {
		Map<String, String> sensitiveData = new LinkedHashMap<>();
		for (SensitiveTag entry : SENSITIVE_TAGS) {
			String value = dcm.getString(entry.tag());
			if (value != null) {
				sensitiveData.put(entry.name(), value);
			}
		}
		return sensitiveData;
	}

}
