/*
 * Copyright (c) 2022-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.data.entity.TransferSeriesReasonEntity;
import org.karnak.backend.data.entity.TransferSeriesStatusEntity;
import org.karnak.backend.data.repo.DestinationRepo;
import org.karnak.backend.data.repo.TransferSeriesReasonRepo;
import org.karnak.backend.data.repo.TransferSeriesStatusRepo;
import org.karnak.backend.data.repo.specification.TransferSeriesSpecifications;
import org.karnak.backend.model.monitoring.DestinationActivityModel;
import org.karnak.backend.model.monitoring.ErrorBreakdownModel;
import org.karnak.backend.model.monitoring.MonitoringSearchCriteria;
import org.karnak.backend.model.monitoring.NodeActivityModel;
import org.karnak.backend.model.monitoring.SeriesActivityModel;
import org.karnak.backend.model.monitoring.StudyActivityModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregates the per-series {@code transfer_series_status} rows into the monitoring
 * hierarchy (Destination / Study / Series / error breakdown) and into per-forward-node
 * activity for the dashboard.
 */
@Service
@NullUnmarked
public class MonitoringAggregationService {

	private final DestinationRepo destinationRepo;

	private final TransferSeriesStatusRepo seriesStatusRepo;

	private final TransferSeriesReasonRepo seriesReasonRepo;

	@Autowired
	public MonitoringAggregationService(final DestinationRepo destinationRepo,
			final TransferSeriesStatusRepo seriesStatusRepo, final TransferSeriesReasonRepo seriesReasonRepo) {
		this.destinationRepo = destinationRepo;
		this.seriesStatusRepo = seriesStatusRepo;
		this.seriesReasonRepo = seriesReasonRepo;
	}

	/** Base filter (search criteria only) shared by every level. */
	private Specification<@NonNull TransferSeriesStatusEntity> baseFilter(MonitoringSearchCriteria criteria) {
		return Specification
			.where(TransferSeriesSpecifications.matchesFilter(MonitoringSearchCriteria.toTransferStatusFilter(criteria)));
	}

	/** Base filter scoped to {@code criteria.destinationUuid()}. */
	private Specification<@NonNull TransferSeriesStatusEntity> destinationScopedFilter(MonitoringSearchCriteria criteria) {
		UUID destinationUuid = criteria == null ? null : criteria.destinationUuid();
		return baseFilter(criteria).and(TransferSeriesSpecifications.hasDestinationUuid(destinationUuid));
	}

	/** Destination-scoped filter further scoped to {@code criteria.studyUid()}. */
	private Specification<@NonNull TransferSeriesStatusEntity> studyScopedFilter(MonitoringSearchCriteria criteria) {
		String studyUid = criteria == null ? null : criteria.studyUid();
		return destinationScopedFilter(criteria).and(TransferSeriesSpecifications.hasStudyUidOriginal(studyUid));
	}

	/** Destination-scoped filter further scoped to {@code criteria.serieUid()}. */
	private Specification<@NonNull TransferSeriesStatusEntity> seriesScopedFilter(MonitoringSearchCriteria criteria) {
		String serieUid = criteria == null ? null : criteria.serieUid();
		return destinationScopedFilter(criteria).and(TransferSeriesSpecifications.hasSerieUidOriginal(serieUid));
	}

	/** Destinations with their aggregated counts, errors first. */
	@Transactional(readOnly = true)
	public List<DestinationActivityModel> searchDestinations(MonitoringSearchCriteria criteria) {
		List<TransferSeriesStatusEntity> rows = seriesStatusRepo.findAll(destinationScopedFilter(criteria));
		Map<UUID, List<TransferSeriesStatusEntity>> byDestination = rows.stream()
			.collect(Collectors.groupingBy(row -> row.getDestinationEntity().getUuid()));
		Map<UUID, DestinationEntity> destinations = loadDestinations(byDestination.keySet());

		return byDestination.entrySet().stream().map(entry -> {
            UUID destinationUuid = entry.getKey();
            List<TransferSeriesStatusEntity> group = entry.getValue();
            DestinationEntity destination = destinations.get(destinationUuid);
            return new DestinationActivityModel(destinationUuid, forwardAet(destination), destinationLabel(destination),
                    distinctStudyCount(group), group.size(), sumLong(group, TransferSeriesStatusEntity::getInstances),
                    sumLong(group, TransferSeriesStatusEntity::getSent), sumLong(group, TransferSeriesStatusEntity::getErrors),
                    sumLong(group, TransferSeriesStatusEntity::getRetries),
                    sumLong(group, TransferSeriesStatusEntity::getExcluded));
        }).sorted(Comparator.comparingLong(DestinationActivityModel::errors)
                .reversed()
                .thenComparing(d -> StringUtils.defaultString(d.destinationLabel()))).collect(Collectors.toCollection(ArrayList::new));
	}

	/** Studies under a destination, errors first. Scoped by {@code criteria.destinationUuid()}. */
	@Transactional(readOnly = true)
	public List<StudyActivityModel> searchStudies(MonitoringSearchCriteria criteria) {
		List<TransferSeriesStatusEntity> rows = seriesStatusRepo.findAll(destinationScopedFilter(criteria));
		Map<String, List<TransferSeriesStatusEntity>> byStudy = rows.stream()
			.collect(Collectors.groupingBy(TransferSeriesStatusEntity::getStudyUidOriginal));

		return byStudy.entrySet().stream().map(entry -> {
            String studyUid = entry.getKey();
            List<TransferSeriesStatusEntity> group = entry.getValue();
            return new StudyActivityModel(studyUid, maxString(group, TransferSeriesStatusEntity::getStudyUidToSend),
                    maxString(group, TransferSeriesStatusEntity::getStudyDescriptionOriginal),
                    maxString(group, TransferSeriesStatusEntity::getStudyDescriptionToSend),
                    maxString(group, TransferSeriesStatusEntity::getPatientIdOriginal),
                    maxString(group, TransferSeriesStatusEntity::getPatientIdToSend),
                    maxString(group, TransferSeriesStatusEntity::getAccessionNumberOriginal),
                    maxString(group, TransferSeriesStatusEntity::getAccessionNumberToSend),
                    maxDateTime(group, TransferSeriesStatusEntity::getStudyDateOriginal),
                    maxDateTime(group, TransferSeriesStatusEntity::getStudyDateToSend), group.size(),
                    sumLong(group, TransferSeriesStatusEntity::getInstances),
                    sumLong(group, TransferSeriesStatusEntity::getSent), sumLong(group, TransferSeriesStatusEntity::getErrors),
                    sumLong(group, TransferSeriesStatusEntity::getRetries),
                    sumLong(group, TransferSeriesStatusEntity::getExcluded),
                    minDateTime(group, TransferSeriesStatusEntity::getFirstSeen),
                    maxDateTime(group, TransferSeriesStatusEntity::getLastSeen));
        }).sorted(Comparator.comparingLong(StudyActivityModel::errors)
                .reversed()
                .thenComparing(s -> StringUtils.defaultString(s.studyUid()))).collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Series under a study of a destination, errors first. Scoped by
	 * {@code criteria.destinationUuid()} and {@code criteria.studyUid()}.
	 */
	@Transactional(readOnly = true)
	public List<SeriesActivityModel> searchSeries(MonitoringSearchCriteria criteria) {
		List<TransferSeriesStatusEntity> rows = seriesStatusRepo.findAll(studyScopedFilter(criteria));
		Map<String, List<TransferSeriesStatusEntity>> bySeries = rows.stream()
			.collect(Collectors.groupingBy(TransferSeriesStatusEntity::getSerieUidOriginal));

		return bySeries.entrySet().stream().map(entry -> {
            String serieUid = entry.getKey();
            List<TransferSeriesStatusEntity> group = entry.getValue();
            return new SeriesActivityModel(serieUid, maxString(group, TransferSeriesStatusEntity::getSerieUidToSend),
                    maxString(group, TransferSeriesStatusEntity::getSerieDescriptionOriginal),
                    maxString(group, TransferSeriesStatusEntity::getSerieDescriptionToSend),
                    maxString(group, TransferSeriesStatusEntity::getModality),
                    maxString(group, TransferSeriesStatusEntity::getSopClassUids),
                    maxDateTime(group, TransferSeriesStatusEntity::getSerieDateOriginal),
                    maxDateTime(group, TransferSeriesStatusEntity::getSerieDateToSend),
                    sumLong(group, TransferSeriesStatusEntity::getInstances),
                    sumLong(group, TransferSeriesStatusEntity::getSent), sumLong(group, TransferSeriesStatusEntity::getErrors),
                    sumLong(group, TransferSeriesStatusEntity::getRetries),
                    sumLong(group, TransferSeriesStatusEntity::getExcluded),
                    minDateTime(group, TransferSeriesStatusEntity::getFirstSeen),
                    maxDateTime(group, TransferSeriesStatusEntity::getLastSeen));
        }).sorted(Comparator.comparingLong(SeriesActivityModel::errors)
                .reversed()
                .thenComparing(s -> StringUtils.defaultString(s.serieUid()))).collect(Collectors.toCollection(ArrayList::new));

	}

	/**
	 * Distinct reasons of a series with their error / excluded outcome counts. Scoped by
	 * {@code criteria.destinationUuid()} and {@code criteria.serieUid()}.
	 */
	@Transactional(readOnly = true)
	public List<ErrorBreakdownModel> searchErrors(MonitoringSearchCriteria criteria) {
		List<Long> seriesIds = seriesStatusRepo.findAll(seriesScopedFilter(criteria))
			.stream()
			.map(TransferSeriesStatusEntity::getId)
			.toList();
		if (seriesIds.isEmpty()) {
			return List.of();
		}

		Map<String, List<TransferSeriesReasonEntity>> byReason = seriesReasonRepo.findBySeriesStatusIdIn(seriesIds)
			.stream()
			.collect(Collectors.groupingBy(TransferSeriesReasonEntity::getReason));

		return byReason.entrySet().stream().map(entry -> {
            String reason = entry.getKey();
            List<TransferSeriesReasonEntity> group = entry.getValue();
            return new ErrorBreakdownModel(reason, group.stream().mapToLong(TransferSeriesReasonEntity::getErrorCount).sum(),
                    group.stream().mapToLong(TransferSeriesReasonEntity::getExcludedCount).sum(),
                    group.stream().mapToLong(TransferSeriesReasonEntity::getRetryCount).sum());
        }).sorted(Comparator.comparingLong((ErrorBreakdownModel e) -> e.errors() + e.excluded())
                .thenComparingLong(ErrorBreakdownModel::errors)
                .reversed()).collect(Collectors.toCollection(ArrayList::new));
	}

	/** Per-forward-node activity for the dashboard, busiest first. */
	@Transactional(readOnly = true)
	public List<NodeActivityModel> searchNodeActivity(MonitoringSearchCriteria criteria) {
		List<TransferSeriesStatusEntity> rows = seriesStatusRepo.findAll(baseFilter(criteria));
		Map<Long, List<TransferSeriesStatusEntity>> byNode = rows.stream()
			.collect(Collectors.groupingBy(TransferSeriesStatusEntity::getForwardNodeId));

		return byNode.values().stream().map(group -> {
            UUID forwardNodeUuid = group.getFirst().getForwardNodeEntity().getUuid();
            return new NodeActivityModel(forwardNodeUuid, forwardAet(group), distinctStudyCount(group), group.size(),
                    sumLong(group, TransferSeriesStatusEntity::getInstances),
                    sumLong(group, TransferSeriesStatusEntity::getSent), sumLong(group, TransferSeriesStatusEntity::getErrors),
                    sumLong(group, TransferSeriesStatusEntity::getRetries),
                    sumLong(group, TransferSeriesStatusEntity::getExcluded), sumInstancesWhenTrue(group,
                    TransferSeriesStatusEntity::getDestinationEntity, DestinationEntity::isDesidentification),
                    sumInstancesWhenTrue(group, TransferSeriesStatusEntity::getDestinationEntity,
                            DestinationEntity::isActivateTagMorphing));
        }).sorted(Comparator.comparingLong(NodeActivityModel::instances)
                .reversed()
                .thenComparing(n -> StringUtils.defaultString(n.forwardAet()))).collect(Collectors.toCollection(ArrayList::new));
	}

	// --- helpers ------

	private static long sumLong(List<TransferSeriesStatusEntity> rows, ToLongFunction<TransferSeriesStatusEntity> extractor) {
		return rows.stream().mapToLong(extractor).sum();
	}

	private static long distinctStudyCount(List<TransferSeriesStatusEntity> rows) {
		return rows.stream().map(TransferSeriesStatusEntity::getStudyUidOriginal).distinct().count();
	}

	/** SUM of instances over rows whose related entity (via {@code relation}) matches {@code test}. */
	private static <T> long sumInstancesWhenTrue(List<TransferSeriesStatusEntity> rows,
			Function<TransferSeriesStatusEntity, T> relation, java.util.function.Predicate<T> test) {
		return rows.stream()
			.filter(row -> Optional.ofNullable(relation.apply(row)).filter(test).isPresent())
			.mapToLong(TransferSeriesStatusEntity::getInstances)
			.sum();
	}

	/** Greatest non-null string value among the rows (mirrors SQL {@code MAX}). */
	private static String maxString(List<TransferSeriesStatusEntity> rows, Function<TransferSeriesStatusEntity, String> extractor) {
		return rows.stream().map(extractor).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
	}

	/** Greatest non-null date among the rows (mirrors SQL {@code MAX}). */
	private static LocalDateTime maxDateTime(List<TransferSeriesStatusEntity> rows,
			Function<TransferSeriesStatusEntity, LocalDateTime> extractor) {
		return rows.stream().map(extractor).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
	}

	/** Least non-null date among the rows (mirrors SQL {@code MIN}). */
	private static LocalDateTime minDateTime(List<TransferSeriesStatusEntity> rows,
			Function<TransferSeriesStatusEntity, LocalDateTime> extractor) {
		return rows.stream().map(extractor).filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
	}

	private Map<UUID, DestinationEntity> loadDestinations(Collection<UUID> uuids) {
		return destinationRepo.findByUuidIn(uuids)
			.stream()
			.collect(Collectors.toMap(DestinationEntity::getUuid, Function.identity()));
	}

	private static String forwardAet(DestinationEntity destination) {
		return Optional.ofNullable(destination)
			.map(DestinationEntity::getForwardNodeEntity)
			.map(ForwardNodeEntity::getFwdAeTitle)
			.orElse("");
	}

	/** Forward node AE Title, read off the first row of the group that carries it (LEFT-join semantics). */
	private static String forwardAet(List<TransferSeriesStatusEntity> rows) {
		return rows.stream()
			.map(TransferSeriesStatusEntity::getForwardNodeEntity)
			.filter(Objects::nonNull)
			.map(ForwardNodeEntity::getFwdAeTitle)
			.filter(Objects::nonNull)
			.findFirst()
			.orElse("");
	}

	private static String destinationLabel(DestinationEntity destination) {
		if (destination == null) {
			return "Unknown destination";
		}
		String reference = destination.retrieveStringReference();
		String description = destination.getDescription();
		return StringUtils.isBlank(description) ? reference : "%s (%s)".formatted(reference, description);
	}

}
