/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.data.entity.TransferSeriesReasonEntity;
import org.karnak.backend.data.entity.TransferSeriesStatusEntity;
import org.karnak.backend.data.repo.DestinationRepo;
import org.karnak.backend.data.repo.ForwardNodeRepo;
import org.karnak.backend.data.repo.TransferSeriesReasonRepo;
import org.karnak.backend.data.repo.TransferSeriesStatusRepo;
import org.karnak.backend.enums.TransferStatusType;
import org.karnak.backend.model.monitoring.DestinationActivityModel;
import org.karnak.backend.model.monitoring.ErrorBreakdownModel;
import org.karnak.backend.model.monitoring.MonitoringSearchCriteria;
import org.karnak.backend.model.monitoring.NodeActivityModel;
import org.karnak.backend.model.monitoring.SeriesActivityModel;
import org.karnak.backend.model.monitoring.StudyActivityModel;
import org.karnak.frontend.monitoring.component.TransferStatusFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * Exercises the monitoring aggregation queries against a real (H2) persistence context. A
 * small fixture of {@code transfer_series_status} rows (two studies under one forward
 * node, split across two destinations) is persisted, then each aggregation level is read
 * back and the grouped counters / ordering are asserted. {@link TransferStatusFilter}
 * variants additionally cover the shared predicate branches.
 */
@DataJpaTest
@Import(MonitoringAggregationService.class)
@DisplayNameGeneration(ReplaceUnderscores.class)
class MonitoringAggregationServiceTest {

	private static final String STUDY_1 = "1.2.1";

	private static final String STUDY_2 = "1.2.2";

	private static final String SERIE_OK = "1.2.1.1";

	private static final String SERIE_ERR = "1.2.1.2";

	private static final LocalDateTime DAY1 = LocalDateTime.of(2026, 1, 10, 8, 0);

	private static final LocalDateTime DAY2 = LocalDateTime.of(2026, 1, 20, 8, 0);

	@Autowired
	private MonitoringAggregationService service;

	@Autowired
	private ForwardNodeRepo forwardNodeRepo;

	@Autowired
	private DestinationRepo destinationRepo;

	@Autowired
	private TransferSeriesStatusRepo statusRepo;

	@Autowired
	private TransferSeriesReasonRepo reasonRepo;

	private Long forwardNodeId;

	private ForwardNodeEntity forwardNode;

	private UUID forwardNodeUuid;

	private UUID destDeidUuid;

	private UUID destMorphUuid;

	@BeforeEach
	void seed() {
		ForwardNodeEntity node = ForwardNodeEntity.ofEmpty();
		node.setFwdAeTitle("FWD-AET");
		node.setUuid(UUID.fromString("11111111-2222-3333-4444-555555555555"));
		node.setFwdDescription("forward node");

		DestinationEntity deid = DestinationEntity.ofDicom("primary", "AET-DEID", "host-a", 104, null);
		deid.setDesidentification(true);
		deid.setUuid(UUID.fromString("22222222-3333-4444-5555-666666666666"));
		deid.setActivateTagMorphing(false);

		DestinationEntity morph = DestinationEntity.ofDicom(null, "AET-MORPH", "host-b", 105, null);
		morph.setDesidentification(false);
		morph.setUuid(UUID.fromString("33333333-4444-5555-6666-777777777777"));
		morph.setActivateTagMorphing(true);

		node.addDestination(deid);
		node.addDestination(morph);
		node = forwardNodeRepo.saveAndFlush(node);
		destinationRepo.saveAndFlush(deid);
		destinationRepo.saveAndFlush(morph);

		forwardNodeId = node.getId();
		forwardNode = node;
		forwardNodeUuid = node.getUuid();
		destDeidUuid = deid.getUuid();
		destMorphUuid = morph.getUuid();

		// Study 1 on the de-identified destination: one clean series (with a re-send),
		// one failing series (with one excluded/filtered instance).
		persistStatus(deid, STUDY_1, SERIE_OK, "CT", 10, 10, 0, 1, 0, DAY1);
		Long errRowId = persistStatus(deid, STUDY_1, SERIE_ERR, "CT", 5, 3, 2, 0, 1, DAY2);
		// Study 2 on the tag-morphing destination: clean, with two re-sends.
		persistStatus(morph, STUDY_2, "1.2.2.1", "MR", 4, 4, 0, 2, 0, DAY2);

		// Two error outcomes carry "timeout" (one of them a retry); the single excluded
		// outcome carries "association-rejected" — mirroring the series counters.
		reasonRepo.saveAndFlush(new TransferSeriesReasonEntity(errRowId, "timeout", 2, 0, 1));
		reasonRepo.saveAndFlush(new TransferSeriesReasonEntity(errRowId, "association-rejected", 0, 1, 0));
	}

	private Long persistStatus(DestinationEntity destination, String studyUid, String serieUid, String modality, long instances,
			long sent, long errors, long retries, long excluded, LocalDateTime seen) {
		TransferSeriesStatusEntity row = new TransferSeriesStatusEntity();
		row.setForwardNodeId(forwardNodeId);
		row.setForwardNodeEntity(forwardNode);
		row.setDestinationId(destination.getId());
		row.setDestinationEntity(destination);
		row.setStudyUidOriginal(studyUid);
		row.setStudyUidToSend(studyUid);
		row.setSerieUidOriginal(serieUid);
		row.setSerieUidToSend(serieUid);
		row.setModality(modality);
		row.setInstances(instances);
		row.setSent(sent);
		row.setErrors(errors);
		row.setRetries(retries);
		row.setExcluded(excluded);
		row.setFirstSeen(seen);
		row.setLastSeen(seen);
		return statusRepo.saveAndFlush(row).getId();
	}

	private TransferStatusFilter noFilter() {
		return new TransferStatusFilter();
	}

	private MonitoringSearchCriteria criteria(TransferStatusFilter filter) {
		return MonitoringSearchCriteria.from(filter);
	}

	@Test
	void list_destinations_groups_counters_and_orders_errors_first() {
		List<DestinationActivityModel> result = service.searchDestinations(criteria(noFilter()));

		assertEquals(2, result.size());
		// The de-identified destination has the failing series, so it sorts first.
		DestinationActivityModel first = result.get(0);
		assertEquals(destDeidUuid, first.destinationUuid());
		assertEquals("FWD-AET", first.forwardAet());
		assertEquals("AET-DEID (primary)", first.destinationLabel());
		assertEquals(1, first.studies());
		assertEquals(2, first.series());
		assertEquals(15, first.instances());
		assertEquals(13, first.sent());
		assertEquals(2, first.errors());
		assertEquals(1, first.retries());
		assertEquals(1, first.excluded());

		DestinationActivityModel second = result.get(1);
		assertEquals(destMorphUuid, second.destinationUuid());
		// Blank description falls back to the bare reference (AE Title).
		assertEquals("AET-MORPH", second.destinationLabel());
		assertEquals(0, second.errors());
		assertEquals(4, second.instances());
		assertEquals(2, second.retries());
		assertEquals(0, second.excluded());
	}

	@Test
	void list_studies_for_a_destination_aggregates_series_counts() {
		List<StudyActivityModel> result = service.searchStudies(criteria(noFilter()).withDestinationUuid(destDeidUuid));

		assertEquals(1, result.size());
		StudyActivityModel study = result.getFirst();
		assertEquals(STUDY_1, study.studyUid());
		assertEquals(15, study.instances());
		assertEquals(13, study.sent());
		assertEquals(2, study.errors());
		assertEquals(1, study.retries());
		assertEquals(1, study.excluded());
		assertEquals(DAY1, study.firstSeen());
		assertEquals(DAY2, study.lastSeen());
	}

	@Test
	void list_series_orders_the_failing_series_first() {
		List<SeriesActivityModel> result = service
			.searchSeries(criteria(noFilter()).withDestinationUuid(destDeidUuid).withStudyUid(STUDY_1));

		assertEquals(2, result.size());
		assertEquals(SERIE_ERR, result.get(0).serieUid());
		assertEquals(2, result.get(0).errors());
		assertEquals("CT", result.get(0).modality());
		assertEquals(SERIE_OK, result.get(1).serieUid());
		assertEquals(0, result.get(1).errors());
	}

	@Test
	void list_errors_sums_reason_counters_per_bucket_for_a_series() {
		List<ErrorBreakdownModel> result = service
			.searchErrors(criteria(noFilter()).withDestinationUuid(destDeidUuid).withSerieUid(SERIE_ERR));

		assertEquals(2, result.size());
		assertEquals(2, result.stream().mapToLong(ErrorBreakdownModel::errors).sum());
		assertEquals(1, result.stream().mapToLong(ErrorBreakdownModel::excluded).sum());
		ErrorBreakdownModel timeout = result.stream().filter(e -> "timeout".equals(e.reason())).findFirst().orElseThrow();
		assertEquals(2, timeout.errors());
		assertEquals(0, timeout.excluded());
		assertEquals(1, timeout.retries());
		ErrorBreakdownModel rejected = result.stream()
			.filter(e -> "association-rejected".equals(e.reason()))
			.findFirst()
			.orElseThrow();
		assertEquals(0, rejected.errors());
		assertEquals(1, rejected.excluded());
		assertEquals(0, rejected.retries());
	}

	@Test
	void list_errors_returns_empty_when_no_series_matches() {
		assertTrue(service
			.searchErrors(criteria(noFilter()).withDestinationUuid(destDeidUuid).withSerieUid("no-such-serie"))
			.isEmpty());
	}

	@Test
	void list_node_activity_splits_deidentified_and_tag_morphed_instances() {
		List<NodeActivityModel> result = service.searchNodeActivity(criteria(noFilter()));

		assertEquals(1, result.size());
		NodeActivityModel node = result.getFirst();
		assertEquals(forwardNodeUuid, node.forwardNodeUuid());
		assertEquals("FWD-AET", node.forwardAet());
		assertEquals(2, node.studies());
		assertEquals(3, node.series());
		assertEquals(19, node.instances());
		assertEquals(17, node.sent());
		assertEquals(2, node.errors());
		assertEquals(3, node.retries());
		assertEquals(1, node.excluded());
		// 15 instances on the de-identified destination, 4 on the tag-morphing one.
		assertEquals(15, node.deidentified());
		assertEquals(4, node.tagMorphed());
	}

	@Test
	void error_status_filter_keeps_only_destinations_with_failures() {
		TransferStatusFilter filter = noFilter();
		filter.setTransferStatusType(TransferStatusType.ERROR);

		List<DestinationActivityModel> result = service.searchDestinations(criteria(filter));

		assertEquals(1, result.size());
		assertEquals(destDeidUuid, result.getFirst().destinationUuid());
		assertEquals(2, result.getFirst().errors());
	}

	@Test
	void sent_status_filter_keeps_destinations_that_sent_something() {
		TransferStatusFilter filter = noFilter();
		filter.setTransferStatusType(TransferStatusType.SENT);

		assertEquals(2, service.searchDestinations(criteria(filter)).size());
	}

	@Test
	void study_uid_filter_restricts_the_matched_rows() {
		TransferStatusFilter filter = noFilter();
		filter.setStudyUid(STUDY_2);

		List<DestinationActivityModel> result = service.searchDestinations(criteria(filter));

		assertEquals(1, result.size());
		assertEquals(destMorphUuid, result.getFirst().destinationUuid());
	}

	@Test
	void date_range_filter_excludes_rows_outside_the_window() {
		TransferStatusFilter filter = noFilter();
		filter.setStart(DAY2.minusDays(1));
		filter.setEnd(DAY2.plusDays(1));

		List<NodeActivityModel> result = service.searchNodeActivity(criteria(filter));

		assertEquals(1, result.size());
		// Only the DAY2 rows survive: the failing CT series (5) and the MR series (4).
		assertEquals(9, result.getFirst().instances());
		assertEquals(2, result.getFirst().series());
	}

}