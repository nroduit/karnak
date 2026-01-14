/*
 * Copyright (c) 2022-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.cache;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

public class Patient implements Serializable {

	@Serial
	private static final long serialVersionUID = -6906583906530083181L;

	private static final Character SPLIT_CHAR_PATIENT_NAME = '^';

	private String pseudonym;

	private String patientId;

	private String patientName;

	private String patientFirstName;

	private String patientLastName;

	private LocalDate patientBirthDate;

	private String patientSex;

	private String issuerOfPatientId;

	private Long projectID;

	public Patient(String pseudonym, String patientId, String patientName, String patientFirstName,
			String patientLastName, LocalDate patientBirthDate, String patientSex, String issuerOfPatientId,
			Long projectID) {
		this.pseudonym = pseudonym;
		this.patientId = patientId;
		this.patientName = patientName;
		this.patientFirstName = emptyStringIfNull(patientFirstName);
		this.patientLastName = emptyStringIfNull(patientLastName);
		this.patientBirthDate = patientBirthDate;
		this.patientSex = patientSex;
		this.issuerOfPatientId = issuerOfPatientId;
		this.projectID = projectID;
	}

	public Patient(String pseudonym, String patientId, String patientFirstName, String patientLastName,
			String issuerOfPatientId, Long projectID) {
		this.pseudonym = pseudonym;
		this.patientId = patientId;
		this.patientFirstName = emptyStringIfNull(patientFirstName);
		this.patientLastName = emptyStringIfNull(patientLastName);
		this.patientName = createPatientName(patientFirstName, patientLastName);
		this.issuerOfPatientId = issuerOfPatientId;
		this.projectID = projectID;
	}

	public Patient(String pseudonym, String patientId, String patientFirstName, String patientLastName,
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

	public String getPseudonym() {
		return pseudonym;
	}

	public void setPseudonym(String pseudonym) {
		this.pseudonym = pseudonym;
	}

	public String getPatientId() {
		return patientId;
	}

	public void setPatientId(String patientId) {
		this.patientId = patientId;
	}

	public String getPatientName() {
		return patientName;
	}

	public void setPatientName(String patientName) {
		this.patientName = patientName;
	}

	public String getPatientFirstName() {
		return patientFirstName;
	}

	public void setPatientFirstName(String patientFirstName) {
		this.patientFirstName = patientFirstName;
	}

	public String getPatientLastName() {
		return patientLastName;
	}

	public void setPatientLastName(String patientLastName) {
		this.patientLastName = patientLastName;
	}

	public LocalDate getPatientBirthDate() {
		return patientBirthDate;
	}

	public void setPatientBirthDate(LocalDate patientBirthDate) {
		this.patientBirthDate = patientBirthDate;
	}

	public String getPatientSex() {
		return patientSex;
	}

	public void setPatientSex(String patientSex) {
		this.patientSex = patientSex;
	}

	public String getIssuerOfPatientId() {
		return issuerOfPatientId;
	}

	public void setIssuerOfPatientId(String issuerOfPatientId) {
		this.issuerOfPatientId = issuerOfPatientId;
	}

	public Long getProjectID() {
		return projectID;
	}

	public void setProjectID(Long projectID) {
		this.projectID = projectID;
	}

	protected static String createPatientName(String patientFirstName, String patientLastName) {
		if (patientFirstName == null || patientFirstName.equals("")) {
			return patientLastName;
		}
		return String.format("%s%c%s", patientLastName == null ? "" : patientLastName, SPLIT_CHAR_PATIENT_NAME,
				patientFirstName);
	}

	protected static String createPatientLastName(String patientName) {
		return patientName.split(String.format("\\%c", SPLIT_CHAR_PATIENT_NAME))[0];
	}

	protected static String createPatientFirstName(String patientName) {
		String[] patientNameSplitted = patientName.split(String.format("\\%c", SPLIT_CHAR_PATIENT_NAME));
		if (patientNameSplitted.length > 1) {
			return patientNameSplitted[1];
		}
		return "";
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

	@Override
	public String toString() {
		return "Patient{" + "pseudonym='" + pseudonym + '\'' + ", patientId='" + patientId + '\'' + ", patientName='"
				+ patientName + '\'' + ", patientFirstName='" + patientFirstName + '\'' + ", patientLastName='"
				+ patientLastName + '\'' + ", patientBirthDate=" + patientBirthDate + ", patientSex='" + patientSex
				+ '\'' + ", issuerOfPatientId='" + issuerOfPatientId + '\'' + ", projectID=" + projectID + '}';
	}

}
