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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.data.entity.TransferSeriesReasonEntity;
import org.karnak.backend.data.entity.TransferSeriesStatusEntity;
import org.karnak.backend.data.repo.DestinationRepo;
import org.karnak.backend.data.repo.specification.TransferSeriesPredicates;
import org.karnak.backend.model.monitoring.DestinationActivity;
import org.karnak.backend.model.monitoring.ErrorBreakdown;
import org.karnak.backend.model.monitoring.NodeActivity;
import org.karnak.backend.model.monitoring.SeriesActivity;
import org.karnak.backend.model.monitoring.StudyActivity;
import org.karnak.frontend.monitoring.component.TransferStatusFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregates the per-series {@code transfer_series_status} rows into the monitoring
 * hierarchy (Destination / Study / Series / error breakdown) and into per-forward-node
 * activity for the dashboard. Because the rows are already per series, each level is a
 * light grouped query summing the counters; the same filter predicates as the CSV export
 * are reused ({@link TransferSeriesPredicates}).
 */
@Service
@NullUnmarked
public class MonitoringAggregationService {

	@PersistenceContext
	private EntityManager entityManager;

	private final DestinationRepo destinationRepo;

	@Autowired
	public MonitoringAggregationService(final DestinationRepo destinationRepo) {
		this.destinationRepo = destinationRepo;
	}

	/** Destinations with their aggregated counts, errors first. */
	@Transactional(readOnly = true)
	public List<DestinationActivity> listDestinations(TransferStatusFilter filter) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> query = cb.createTupleQuery();
		Root<TransferSeriesStatusEntity> root = query.from(TransferSeriesStatusEntity.class);
		List<Predicate> predicates = TransferSeriesPredicates.build(root, cb, filter);

		query.multiselect(root.get("destinationId"), cb.countDistinct(root.get("studyUidOriginal")), cb.count(root),
				cb.sum(root.<Long>get("instances")), cb.sum(root.<Long>get("sent")), cb.sum(root.<Long>get("errors")));
		query.where(predicates.toArray(new Predicate[0]));
		query.groupBy(root.get("destinationId"));

		List<Tuple> rows = entityManager.createQuery(query).getResultList();
		Map<Long, DestinationEntity> destinations = loadDestinations(rows);

		List<DestinationActivity> result = new ArrayList<>();
		for (Tuple row : rows) {
			Long destinationId = row.get(0, Long.class);
			DestinationEntity destination = destinations.get(destinationId);
			result.add(new DestinationActivity(destinationId, forwardAet(destination), destinationLabel(destination),
					value(row, 1), value(row, 2), value(row, 3), value(row, 4), value(row, 5)));
		}
		result.sort(Comparator.comparingLong(DestinationActivity::errors)
			.reversed()
			.thenComparing(d -> StringUtils.defaultString(d.destinationLabel())));
		return result;
	}

	/** Studies under a destination, errors first. */
	@Transactional(readOnly = true)
	public List<StudyActivity> listStudies(TransferStatusFilter filter, Long destinationId) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> query = cb.createTupleQuery();
		Root<TransferSeriesStatusEntity> root = query.from(TransferSeriesStatusEntity.class);
		List<Predicate> predicates = TransferSeriesPredicates.build(root, cb, filter);
		predicates.add(equalOrNull(cb, root.get("destinationId"), destinationId));

		query.multiselect(root.get("studyUidOriginal"), cb.greatest(root.<String>get("studyUidToSend")),
				cb.greatest(root.<String>get("studyDescriptionOriginal")),
				cb.greatest(root.<String>get("studyDescriptionToSend")),
				cb.greatest(root.<String>get("patientIdOriginal")), cb.greatest(root.<String>get("patientIdToSend")),
				cb.greatest(root.<String>get("accessionNumberOriginal")),
				cb.greatest(root.<String>get("accessionNumberToSend")),
				cb.greatest(root.<LocalDateTime>get("studyDateOriginal")),
				cb.greatest(root.<LocalDateTime>get("studyDateToSend")), cb.count(root),
				cb.sum(root.<Long>get("instances")), cb.sum(root.<Long>get("sent")), cb.sum(root.<Long>get("errors")),
				cb.least(root.<LocalDateTime>get("firstSeen")), cb.greatest(root.<LocalDateTime>get("lastSeen")));
		query.where(predicates.toArray(new Predicate[0]));
		query.groupBy(root.get("studyUidOriginal"));

		List<StudyActivity> result = entityManager.createQuery(query)
			.getResultList()
			.stream()
			.map(row -> new StudyActivity(row.get(0, String.class), row.get(1, String.class), row.get(2, String.class),
					row.get(3, String.class), row.get(4, String.class), row.get(5, String.class),
					row.get(6, String.class), row.get(7, String.class), row.get(8, LocalDateTime.class),
					row.get(9, LocalDateTime.class), value(row, 10), value(row, 11), value(row, 12), value(row, 13),
					row.get(14, LocalDateTime.class), row.get(15, LocalDateTime.class)))
			.collect(Collectors.toCollection(ArrayList::new));
		result.sort(Comparator.comparingLong(StudyActivity::errors)
			.reversed()
			.thenComparing(s -> StringUtils.defaultString(s.studyUid())));
		return result;
	}

	/** Series under a study of a destination, errors first. */
	@Transactional(readOnly = true)
	public List<SeriesActivity> listSeries(TransferStatusFilter filter, Long destinationId, String studyUid) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> query = cb.createTupleQuery();
		Root<TransferSeriesStatusEntity> root = query.from(TransferSeriesStatusEntity.class);
		List<Predicate> predicates = TransferSeriesPredicates.build(root, cb, filter);
		predicates.add(equalOrNull(cb, root.get("destinationId"), destinationId));
		predicates.add(equalOrNull(cb, root.get("studyUidOriginal"), studyUid));

		query.multiselect(root.get("serieUidOriginal"), cb.greatest(root.<String>get("serieUidToSend")),
				cb.greatest(root.<String>get("serieDescriptionOriginal")),
				cb.greatest(root.<String>get("serieDescriptionToSend")), cb.greatest(root.<String>get("modality")),
				cb.greatest(root.<String>get("sopClassUids")),
				cb.greatest(root.<LocalDateTime>get("serieDateOriginal")),
				cb.greatest(root.<LocalDateTime>get("serieDateToSend")), cb.sum(root.<Long>get("instances")),
				cb.sum(root.<Long>get("sent")), cb.sum(root.<Long>get("errors")),
				cb.least(root.<LocalDateTime>get("firstSeen")), cb.greatest(root.<LocalDateTime>get("lastSeen")));
		query.where(predicates.toArray(new Predicate[0]));
		query.groupBy(root.get("serieUidOriginal"));

		List<SeriesActivity> result = entityManager.createQuery(query)
			.getResultList()
			.stream()
			.map(row -> new SeriesActivity(row.get(0, String.class), row.get(1, String.class), row.get(2, String.class),
					row.get(3, String.class), row.get(4, String.class), row.get(5, String.class),
					row.get(6, LocalDateTime.class), row.get(7, LocalDateTime.class), value(row, 8), value(row, 9),
					value(row, 10), row.get(11, LocalDateTime.class), row.get(12, LocalDateTime.class)))
			.collect(Collectors.toCollection(ArrayList::new));
		result.sort(Comparator.comparingLong(SeriesActivity::errors)
			.reversed()
			.thenComparing(s -> StringUtils.defaultString(s.serieUid())));
		return result;
	}

	/** Distinct error reasons of a series with the number of instances concerned. */
	@Transactional(readOnly = true)
	public List<ErrorBreakdown> listErrors(TransferStatusFilter filter, Long destinationId, String serieUid) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Long> idQuery = cb.createQuery(Long.class);
		Root<TransferSeriesStatusEntity> seriesRoot = idQuery.from(TransferSeriesStatusEntity.class);
		List<Predicate> predicates = TransferSeriesPredicates.build(seriesRoot, cb, filter);
		predicates.add(equalOrNull(cb, seriesRoot.get("destinationId"), destinationId));
		predicates.add(equalOrNull(cb, seriesRoot.get("serieUidOriginal"), serieUid));
		idQuery.select(seriesRoot.get("id")).where(predicates.toArray(new Predicate[0]));
		List<Long> seriesIds = entityManager.createQuery(idQuery).getResultList();
		if (seriesIds.isEmpty()) {
			return List.of();
		}

		CriteriaQuery<Tuple> reasonQuery = cb.createTupleQuery();
		Root<TransferSeriesReasonEntity> reasonRoot = reasonQuery.from(TransferSeriesReasonEntity.class);
		reasonQuery.multiselect(reasonRoot.get("reason"), cb.sum(reasonRoot.<Long>get("count")));
		reasonQuery.where(reasonRoot.get("seriesStatusId").in(seriesIds));
		reasonQuery.groupBy(reasonRoot.get("reason"));

		List<ErrorBreakdown> result = entityManager.createQuery(reasonQuery)
			.getResultList()
			.stream()
			.map(row -> new ErrorBreakdown(row.get(0, String.class), value(row, 1)))
			.collect(Collectors.toCollection(ArrayList::new));
		result.sort(Comparator.comparingLong(ErrorBreakdown::instances).reversed());
		return result;
	}

	/** Per-forward-node activity for the dashboard, busiest first. */
	@Transactional(readOnly = true)
	public List<NodeActivity> listNodeActivity(TransferStatusFilter filter) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> query = cb.createTupleQuery();
		Root<TransferSeriesStatusEntity> root = query.from(TransferSeriesStatusEntity.class);
		Join<TransferSeriesStatusEntity, ForwardNodeEntity> forwardNode = root.join("forwardNodeEntity", JoinType.LEFT);
		Join<TransferSeriesStatusEntity, DestinationEntity> destination = root.join("destinationEntity", JoinType.LEFT);
		List<Predicate> predicates = TransferSeriesPredicates.build(root, cb, filter);

		query.multiselect(root.get("forwardNodeId"), forwardNode.get("fwdAeTitle"),
				cb.countDistinct(root.get("studyUidOriginal")), cb.count(root), cb.sum(root.<Long>get("instances")),
				cb.sum(root.<Long>get("sent")), cb.sum(root.<Long>get("errors")),
				sumInstancesWhenTrue(cb, root, destination.get("desidentification")),
				sumInstancesWhenTrue(cb, root, destination.get("activateTagMorphing")));
		query.where(predicates.toArray(new Predicate[0]));
		query.groupBy(root.get("forwardNodeId"), forwardNode.get("fwdAeTitle"));

		List<NodeActivity> result = entityManager.createQuery(query)
			.getResultList()
			.stream()
			.map(row -> new NodeActivity(row.get(0, Long.class), row.get(1, String.class), value(row, 2), value(row, 3),
					value(row, 4), value(row, 5), value(row, 6), value(row, 7), value(row, 8)))
			.collect(Collectors.toCollection(ArrayList::new));
		result.sort(Comparator.comparingLong(NodeActivity::instances)
			.reversed()
			.thenComparing(n -> StringUtils.defaultString(n.forwardAet())));
		return result;
	}

	// --- helpers ---------------------------------------------------------------------

	/** SUM of instances over rows whose given boolean destination flag is true. */
	private Expression<Long> sumInstancesWhenTrue(CriteriaBuilder cb, Root<TransferSeriesStatusEntity> root,
			Path<?> booleanColumn) {
		return cb
			.sum(cb.<Long>selectCase().when(cb.equal(booleanColumn, true), root.<Long>get("instances")).otherwise(0L));
	}

	private Predicate equalOrNull(CriteriaBuilder cb, Path<?> path, Object value) {
		return value == null ? cb.isNull(path) : cb.equal(path, value);
	}

	/** Reads a numeric aggregate tuple element, treating a null aggregate as 0. */
	private long value(Tuple tuple, int index) {
		Long element = tuple.get(index, Long.class);
		return element == null ? 0L : element;
	}

	private Map<Long, DestinationEntity> loadDestinations(List<Tuple> rows) {
		List<Long> ids = rows.stream().map(row -> row.get(0, Long.class)).filter(java.util.Objects::nonNull).toList();
		return destinationRepo.findAllById(ids)
			.stream()
			.collect(Collectors.toMap(DestinationEntity::getId, Function.identity()));
	}

	private String forwardAet(DestinationEntity destination) {
		return Optional.ofNullable(destination)
			.map(DestinationEntity::getForwardNodeEntity)
			.map(ForwardNodeEntity::getFwdAeTitle)
			.orElse("");
	}

	private String destinationLabel(DestinationEntity destination) {
		if (destination == null) {
			return "Unknown destination";
		}
		String reference = destination.retrieveStringReference();
		String description = destination.getDescription();
		return StringUtils.isBlank(description) ? reference : reference + " (" + description + ")";
	}

}
