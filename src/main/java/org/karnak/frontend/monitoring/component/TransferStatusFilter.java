/*
 * Copyright (c) 2022-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.monitoring.component;

import java.time.LocalDateTime;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.karnak.backend.enums.TransferStatusType;

public class TransferStatusFilter {

	private String studyUid;

	private String serieUid;

	private String sopInstanceUid;

	private TransferStatusType transferStatusType;

	private LocalDateTime start;

	private LocalDateTime end;

	public TransferStatusFilter() {
		this.studyUid = "";
		this.serieUid = "";
		this.sopInstanceUid = "";
		this.transferStatusType = TransferStatusType.ALL;
	}

	public String getStudyUid() {
		return studyUid;
	}

	public void setStudyUid(String studyUid) {
		this.studyUid = studyUid;
	}

	public String getSerieUid() {
		return serieUid;
	}

	public void setSerieUid(String serieUid) {
		this.serieUid = serieUid;
	}

	public String getSopInstanceUid() {
		return sopInstanceUid;
	}

	public void setSopInstanceUid(String sopInstanceUid) {
		this.sopInstanceUid = sopInstanceUid;
	}

	public TransferStatusType getTransferStatusType() {
		return transferStatusType;
	}

	public void setTransferStatusType(TransferStatusType transferStatusType) {
		this.transferStatusType = transferStatusType;
	}

	public LocalDateTime getStart() {
		return start;
	}

	public void setStart(LocalDateTime start) {
		this.start = start;
	}

	public LocalDateTime getEnd() {
		return end;
	}

	public void setEnd(LocalDateTime end) {
		this.end = end;
	}

	public boolean hasFilter() {
		return StringUtils.isNotBlank(studyUid) || StringUtils.isNotBlank(serieUid)
				|| StringUtils.isNotBlank(sopInstanceUid) || !Objects.equals(TransferStatusType.ALL, transferStatusType)
				|| start != null || end != null;
	}

}
