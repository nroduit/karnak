/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.dicom;

import java.time.LocalDate;
import org.karnak.backend.enums.Modality;

public class WorkListQueryData {

	private static final String DEFAULT_VALUE_FOR_CALLING_AET = "DCM-TOOLS";

	private String callingAet;

	private String workListAet;

	private String workListHostname;

	private Integer workListPort;

	private String scheduledStationAet;

	private Modality scheduledModality;

	private String patientId;

	private String admissionId;

	private LocalDate scheduledFrom;

	private LocalDate scheduledTo;

	private String patientName;

	private String accessionNumber;

	public WorkListQueryData() {
		callingAet = DEFAULT_VALUE_FOR_CALLING_AET;
		scheduledModality = Modality.ALL;
	}

	public String getWorkListAet() {
		return workListAet;
	}

	public void setWorkListAet(String workListAet) {
		this.workListAet = workListAet;
	}

	public String getWorkListHostname() {
		return workListHostname;
	}

	public void setWorkListHostname(String workListHostname) {
		this.workListHostname = workListHostname;
	}

	public Integer getWorkListPort() {
		return workListPort;
	}

	public void setWorkListPort(Integer workListPort) {
		this.workListPort = workListPort;
	}

	public String getCallingAet() {
		return callingAet;
	}

	public void setCallingAet(String callingAet) {
		this.callingAet = callingAet;
	}

	public String getScheduledStationAet() {
		return scheduledStationAet;
	}

	public void setScheduledStationAet(String scheduledStationAet) {
		this.scheduledStationAet = scheduledStationAet;
	}

	public Modality getScheduledModality() {
		return scheduledModality;
	}

	public void setScheduledModality(Modality scheduledModality) {
		this.scheduledModality = scheduledModality;
	}

	public String getPatientId() {
		return patientId;
	}

	public void setPatientId(String patientId) {
		this.patientId = patientId;
	}

	public String getAdmissionId() {
		return admissionId;
	}

	public void setAdmissionId(String admissionId) {
		this.admissionId = admissionId;
	}

	public LocalDate getScheduledFrom() {
		return scheduledFrom;
	}

	public void setScheduledFrom(LocalDate scheduledFrom) {
		this.scheduledFrom = scheduledFrom;
	}

	public LocalDate getScheduledTo() {
		return scheduledTo;
	}

	public void setScheduledTo(LocalDate scheduledTo) {
		this.scheduledTo = scheduledTo;
	}

	public String getPatientName() {
		return patientName;
	}

	public void setPatientName(String patientName) {
		this.patientName = patientName;
	}

	public String getAccessionNumber() {
		return accessionNumber;
	}

	public void setAccessionNumber(String accessionNumber) {
		this.accessionNumber = accessionNumber;
	}

	public void reset() {
		callingAet = DEFAULT_VALUE_FOR_CALLING_AET;
		scheduledModality = Modality.ALL;
	}

}
