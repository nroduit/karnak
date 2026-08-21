/*
 * Copyright (c) 2022-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.monitoring;

import java.time.LocalDateTime;
import java.util.UUID;
import org.karnak.backend.enums.TransferStatusType;
import org.karnak.frontend.monitoring.component.TransferStatusFilter;

/**
 * Search criteria for monitoring queries. All fields are optional; null values mean "no
 * filter" for that criterion. {@code destinationUuid}, {@code studyUid} and
 * {@code serieUid} also carry the hierarchy scope (which destination / study / series to
 * drill into) instead of dedicated path variables, so a single criteria object is enough
 * to call every monitoring endpoint.
 */
public record MonitoringSearchCriteria(
		TransferStatusType statusType,
		UUID destinationUuid,
		String studyUid,
		String serieUid,
		String sopInstanceUid,
		LocalDateTime start,
		LocalDateTime end) {

	/** Returns true when at least one filter criterion is specified. */
	public boolean hasCriteria() {
		return statusType != null || destinationUuid != null || studyUid != null || serieUid != null
				|| sopInstanceUid != null || start != null || end != null;
	}

	/** Builds criteria from the legacy monitoring UI filter. */
	public static MonitoringSearchCriteria from(TransferStatusFilter filter) {
		if (filter == null) {
			return new MonitoringSearchCriteria(null, null, null, null, null, null, null);
		}
		return new MonitoringSearchCriteria(filter.getTransferStatusType(), null, filter.getStudyUid(),
				filter.getSerieUid(), filter.getSopInstanceUid(), filter.getStart(), filter.getEnd());
	}

	/**
	 * Converts (possibly null) criteria to the legacy {@link TransferStatusFilter} used
	 * by the shared predicate/specification builders.
	 */
	public static TransferStatusFilter toTransferStatusFilter(MonitoringSearchCriteria criteria) {
		TransferStatusFilter filter = new TransferStatusFilter();
		if (criteria == null) {
			return filter;
		}
		filter.setStudyUid(criteria.studyUid());
		filter.setSerieUid(criteria.serieUid());
		filter.setSopInstanceUid(criteria.sopInstanceUid());
		filter.setStart(criteria.start());
		filter.setEnd(criteria.end());
		if (criteria.statusType() != null) {
			filter.setTransferStatusType(criteria.statusType());
		}
		return filter;
	}

	/** Copy of this criteria scoped to the given destination. */
	public MonitoringSearchCriteria withDestinationUuid(UUID newDestinationUuid) {
		return new MonitoringSearchCriteria(statusType, newDestinationUuid, studyUid, serieUid, sopInstanceUid, start,
				end);
	}

	/** Copy of this criteria scoped to the given (exact) study UID. */
	public MonitoringSearchCriteria withStudyUid(String newStudyUid) {
		return new MonitoringSearchCriteria(statusType, destinationUuid, newStudyUid, serieUid, sopInstanceUid, start,
				end);
	}

	/** Copy of this criteria scoped to the given (exact) series UID. */
	public MonitoringSearchCriteria withSerieUid(String newSerieUid) {
		return new MonitoringSearchCriteria(statusType, destinationUuid, studyUid, newSerieUid, sopInstanceUid, start,
				end);
	}

}
