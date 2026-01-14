/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.kheops;

public class MetadataSwitching {

	private final String studyInstanceUID;

	private final String seriesInstanceUID;

	private final String SOPinstanceUID;

	private boolean applied;

	public MetadataSwitching(String studyInstanceUID, String seriesInstanceUID, String SOPinstanceUID) {
		this.studyInstanceUID = studyInstanceUID;
		this.seriesInstanceUID = seriesInstanceUID;
		this.SOPinstanceUID = SOPinstanceUID;
		this.applied = false;
	}

	public String getStudyInstanceUID() {
		return studyInstanceUID;
	}

	public String getSeriesInstanceUID() {
		return seriesInstanceUID;
	}

	public String getSOPinstanceUID() {
		return SOPinstanceUID;
	}

	public boolean isApplied() {
		return applied;
	}

	public void setApplied(boolean applied) {
		this.applied = applied;
	}

}
