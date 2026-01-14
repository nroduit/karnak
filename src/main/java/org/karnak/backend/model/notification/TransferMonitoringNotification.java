/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Model used to build transfer monitoring notification
 */
public class TransferMonitoringNotification {

	private String subject;

	private String from;

	private String to;

	private String patientId;

	private String studyUid;

	private String accessionNumber;

	private String studyDescription;

	private LocalDateTime studyDate;

	private String source;

	private String destination;

	private List<SerieSummaryNotification> serieSummaryNotifications;

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getPatientId() {
		return patientId;
	}

	public void setPatientId(String patientId) {
		this.patientId = patientId;
	}

	public String getStudyUid() {
		return studyUid;
	}

	public void setStudyUid(String studyUid) {
		this.studyUid = studyUid;
	}

	public String getAccessionNumber() {
		return accessionNumber;
	}

	public void setAccessionNumber(String accessionNumber) {
		this.accessionNumber = accessionNumber;
	}

	public String getStudyDescription() {
		return studyDescription;
	}

	public void setStudyDescription(String studyDescription) {
		this.studyDescription = studyDescription;
	}

	public LocalDateTime getStudyDate() {
		return studyDate;
	}

	public void setStudyDate(LocalDateTime studyDate) {
		this.studyDate = studyDate;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public List<SerieSummaryNotification> getSerieSummaryNotifications() {
		return serieSummaryNotifications;
	}

	public void setSerieSummaryNotifications(List<SerieSummaryNotification> serieSummaryNotifications) {
		this.serieSummaryNotifications = serieSummaryNotifications;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TransferMonitoringNotification that = (TransferMonitoringNotification) o;
		return Objects.equals(subject, that.subject) && Objects.equals(from, that.from) && Objects.equals(to, that.to)
				&& Objects.equals(patientId, that.patientId) && Objects.equals(studyUid, that.studyUid)
				&& Objects.equals(accessionNumber, that.accessionNumber)
				&& Objects.equals(studyDescription, that.studyDescription) && Objects.equals(studyDate, that.studyDate)
				&& Objects.equals(source, that.source) && Objects.equals(destination, that.destination)
				&& Objects.equals(serieSummaryNotifications, that.serieSummaryNotifications);
	}

	@Override
	public int hashCode() {
		return Objects.hash(subject, from, to, patientId, studyUid, accessionNumber, studyDescription, studyDate,
				source, destination, serieSummaryNotifications);
	}

}
