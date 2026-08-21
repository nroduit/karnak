/*
 * Copyright (c) 2022-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.patient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.jspecify.annotations.NullUnmarked;
import org.springframework.validation.annotation.Validated;

@Setter
@Getter
@ToString
@NullUnmarked
@Validated
@NoArgsConstructor
public class PatientModel implements Serializable {

	@Serial
	private static final long serialVersionUID = -6906583906530083181L;

	private static final char NAME_SEPARATOR = '^';

	private static final String NAME_SEPARATOR_REGEX = "\\^";

	private UUID uuid;

	@NotBlank(message = "Pseudonym is mandatory")
	private String pseudonym;

	@NotBlank(message = "Patient id is mandatory")
	private String patientId;

	private String patientName;

	private String patientFirstName;

	private String patientLastName;

	private LocalDate patientBirthDate;

	private String patientSex;

	private String issuerOfPatientId;

	@JsonIgnore
	private Long projectID;

	@NotNull(message = "Project uuid is mandatory")
	private UUID projectUUID;

	public PatientModel(String pseudonym, String patientId, String patientName, String patientFirstName,
						String patientLastName, LocalDate patientBirthDate, String patientSex, String issuerOfPatientId,
						Long projectID, UUID projectUUID) {
		this.pseudonym = pseudonym;
		this.patientId = patientId;
		this.patientName = patientName;
		this.patientFirstName = emptyStringIfNull(patientFirstName);
		this.patientLastName = emptyStringIfNull(patientLastName);
		this.patientBirthDate = patientBirthDate;
		this.patientSex = patientSex;
		this.issuerOfPatientId = issuerOfPatientId;
		this.projectID = projectID;
		this.projectUUID = projectUUID;
	}

	public PatientModel(String pseudonym, String patientId, String patientFirstName, String patientLastName,
						String issuerOfPatientId, Long projectID, UUID projectUUID) {
		this.pseudonym = pseudonym;
		this.patientId = patientId;
		this.patientFirstName = emptyStringIfNull(patientFirstName);
		this.patientLastName = emptyStringIfNull(patientLastName);
		this.patientName = createPatientName(patientFirstName, patientLastName);
		this.issuerOfPatientId = issuerOfPatientId;
		this.projectID = projectID;
		this.projectUUID = projectUUID;
	}

	public PatientModel(String pseudonym, String patientId, String patientFirstName, String patientLastName,
						LocalDate patientBirthDate, String patientSex, String issuerOfPatientId) {
		this.pseudonym = pseudonym;
		this.patientId = patientId;
		this.patientFirstName = emptyStringIfNull(patientFirstName);
		this.patientLastName = emptyStringIfNull(patientLastName);
		this.patientName = createPatientName(patientFirstName, patientLastName);
		this.issuerOfPatientId = issuerOfPatientId;
		this.patientBirthDate = patientBirthDate;
		this.patientSex = patientSex;
	}

    protected static String createPatientName(String patientFirstName, String patientLastName) {
		if (patientFirstName == null || patientFirstName.isEmpty()) {
			return patientLastName;
		}
		return (patientLastName == null ? "" : patientLastName) + NAME_SEPARATOR + patientFirstName;
	}

	protected static String createPatientLastName(String patientName) {
		return patientName.split(NAME_SEPARATOR_REGEX)[0];
	}

	protected static String createPatientFirstName(String patientName) {
		String[] parts = patientName.split(NAME_SEPARATOR_REGEX);
		return parts.length > 1 ? parts[1] : "";
	}

	public void updatePatientName(String patientName) {
		this.patientName = patientName;
		this.patientFirstName = createPatientFirstName(patientName);
		this.patientLastName = createPatientLastName(patientName);
	}

	public void updatePatientLastName(String patientLastName) {
		this.patientLastName = emptyStringIfNull(patientLastName);
		this.patientName = createPatientName(patientFirstName, patientLastName);
	}

	public void updatePatientFirstName(String patientFirstName) {
		this.patientFirstName = emptyStringIfNull(patientFirstName);
		this.patientName = createPatientName(patientFirstName, patientLastName);
	}

	private static String emptyStringIfNull(String value) {
		return value == null ? "" : value;
	}

	public void updatePatientModel(@Valid PatientModel patientModel) {
		this.setPseudonym(patientModel.getPseudonym());
		this.setPatientId(patientModel.getPatientId());
		this.setPatientFirstName(patientModel.getPatientFirstName());
		this.setPatientLastName(patientModel.getPatientLastName());
		this.setPatientSex(patientModel.getPatientSex());
		this.setIssuerOfPatientId(patientModel.getIssuerOfPatientId());
	}
}
