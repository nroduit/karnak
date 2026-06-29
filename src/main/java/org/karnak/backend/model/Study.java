/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

public class Study {

	@Getter
	private final String studyInstanceUID;

	private final Map<String, Series> seriesMap;

	@Setter
	@Getter
	private String patientID;

	@Setter
	@Getter
	private String @Nullable [] otherPatientIDs;

	@Setter
	@Getter
	private @Nullable String studyDescription;

	@Setter
	@Getter
	private @Nullable String accessionNumber;

	@Setter
	@Getter
	private @Nullable LocalDateTime studyDate;

	@Setter
	@Getter
	private long timeStamp;

	public Study(String studyInstanceUID, String patientID) {
		this.studyInstanceUID = Objects.requireNonNull(studyInstanceUID, "studyInstanceUID cannot be null!");
		this.patientID = patientID == null ? "" : patientID;
		this.studyDescription = "";
		this.seriesMap = new HashMap<>();
	}

	public void addSeries(Series s) {
		if (s != null) {
			seriesMap.put(s.getSeriesInstanceUID(), s);
		}
	}

	public @Nullable Series removeSeries(String seriesUID) {
		return seriesMap.remove(seriesUID);
	}

	public boolean isEmpty() {
		return seriesMap.values().stream().allMatch(Series::isEmpty);
	}

	public @Nullable Series getSeries(String seriesUID) {
		return seriesMap.get(seriesUID);
	}

	public Collection<Series> getSeries() {
		return seriesMap.values();
	}

	public Set<Entry<String, Series>> getEntrySet() {
		return seriesMap.entrySet();
	}

	@Override
	public int hashCode() {
		return 31 + studyInstanceUID.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return studyInstanceUID.equals(((Study) obj).studyInstanceUID);
	}

}
