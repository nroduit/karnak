/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.constant.Notification;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.TransferSeriesReasonEntity;
import org.karnak.backend.data.entity.TransferSeriesStatusEntity;
import org.karnak.backend.data.repo.DestinationRepo;
import org.karnak.backend.data.repo.TransferSeriesReasonRepo;
import org.karnak.backend.data.repo.TransferSeriesStatusRepo;
import org.karnak.backend.enums.DestinationType;
import org.karnak.backend.model.notification.SerieSummaryNotification;
import org.karnak.backend.model.notification.TransferMonitoringNotification;
import org.karnak.backend.util.SystemPropertyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.weasis.core.util.StringUtil;

/**
 * Handle notifications. Reads the aggregated {@code transfer_series_status} rows (one per
 * series) touched since the destination's last email check and reports their current
 * counts/reasons; the notification model objects and Thymeleaf template are unchanged.
 */
@Service
@Slf4j
@NullUnmarked
public class NotificationService {

	// Maximum length of a single reason rendered in the email summary
	private static final int REASON_EMAIL_MAX_LENGTH = 200;

	@Value("${mail.sender}")
	private String mailSender;

	// Services
	private final TemplateEngine templateEngine;

	private final JavaMailSender javaMailSender;

	// Repositories
	private final TransferSeriesStatusRepo transferSeriesStatusRepo;

	private final TransferSeriesReasonRepo transferSeriesReasonRepo;

	private final DestinationRepo destinationRepo;

	@Autowired
	public NotificationService(final TemplateEngine templateEngine, final JavaMailSender javaMailSender,
			final TransferSeriesStatusRepo transferSeriesStatusRepo,
			final TransferSeriesReasonRepo transferSeriesReasonRepo, final DestinationRepo destinationRepo) {
		this.templateEngine = templateEngine;
		this.javaMailSender = javaMailSender;
		this.transferSeriesStatusRepo = transferSeriesStatusRepo;
		this.transferSeriesReasonRepo = transferSeriesReasonRepo;
		this.destinationRepo = destinationRepo;
	}

	/**
	 * In a regular period of time, check for new inputs in the transfer notifications,
	 * build the notifications and send them via email.
	 */
	@Scheduled(fixedRate = 120 * 1000)
	public void determineNotificationToSend() {
		buildNotificationsToSend().stream().map(n -> {
			logNotification(n);
			return n;
		}).filter(TransferMonitoringNotification::isEmailSendingEnabled).forEach(this::prepareAndSendNotification);
	}

	/**
	 * Transfer logs
	 * @param transferMonitoringNotification Transfer to log
	 */
	private void logNotification(TransferMonitoringNotification transferMonitoringNotification) {
		// Transfer logs
		log.info("Transfer processed from {} to {}. Details: {}", transferMonitoringNotification.getSource(),
				transferMonitoringNotification.getDestination(),
				transferMonitoringNotification.getSerieSummaryNotifications()
					.stream()
					.map(SerieSummaryNotification::toString)
					.collect(Collectors.joining(",")));
	}

	/** Builds the transfer notifications to send for every eligible destination. */
	List<TransferMonitoringNotification> buildNotificationsToSend() {
		List<TransferMonitoringNotification> transferMonitoringNotifications = new ArrayList<>();
		destinationRepo
			// Retrieve all destinations
			.findAll()
			.stream()
			// Check if notification should be sent for this destinations
			.filter(this::checkDestinationLastVerification)
			.forEach(destinationEntity -> {
				// Keep previous check date
				LocalDateTime previousCheck = destinationEntity.getEmailLastCheck();
				// Update destination last check date
				destinationEntity.setEmailLastCheck(LocalDateTime.now(ZoneId.of("CET")));
				destinationRepo.save(destinationEntity);
				// Retrieve the series aggregates for this destination touched after the
				// last
				// email check
				List<TransferSeriesStatusEntity> seriesLastCheck = retrieveSeriesDestinationLastCheck(destinationEntity,
						previousCheck);
				// Gather series by Source and Study: <Source , <Study, List<Series>>>
				Map<Long, Map<String, List<TransferSeriesStatusEntity>>> seriesBySourceAndStudy = gatherSeriesBySourceAndStudy(
						seriesLastCheck);
				// Build the notifications to send (reads the previous per-series snapshot
				// to compute the delta)
				buildTransferMonitoringNotifications(transferMonitoringNotifications, seriesBySourceAndStudy);
				// Advance the per-series snapshot to the current counters so the next
				// notification reports only what happens afterwards. Done for every
				// touched
				// series (not only the reported ones), consistent with emailLastCheck
				// being
				// advanced above regardless of send outcome. No transfer is in progress
				// for
				// this destination (checkDestinationLastVerification), so counters are
				// stable here.
				snapshotNotifiedCounters(seriesLastCheck);
			});
		return transferMonitoringNotifications;
	}

	/** Records the current sent / errors / excluded counters as the notified baseline. */
	private void snapshotNotifiedCounters(List<TransferSeriesStatusEntity> series) {
		if (series.isEmpty()) {
			return;
		}
		series.forEach(s -> {
			s.setNotifiedSent(s.getSent());
			s.setNotifiedErrors(s.getErrors());
			s.setNotifiedExcluded(s.getExcluded());
		});
		transferSeriesStatusRepo.saveAll(series);
	}

	/** Builds a notification for each (source, study) group and adds it to the list. */
	private void buildTransferMonitoringNotifications(
			List<TransferMonitoringNotification> transferMonitoringNotifications,
			Map<Long, Map<String, List<TransferSeriesStatusEntity>>> seriesBySourceAndStudy) {
		seriesBySourceAndStudy.values()
			.stream()
			.flatMap(byStudy -> byStudy.values().stream())
			.map(this::buildTransferMonitoringNotification)
			.filter(Objects::nonNull)
			.forEach(transferMonitoringNotifications::add);
	}

	/**
	 * Builds the notification for one study (counts, reasons, original vs de-identified
	 * values).
	 */
	private TransferMonitoringNotification buildTransferMonitoringNotification(
			List<TransferSeriesStatusEntity> studySeries) {
		TransferMonitoringNotification transferMonitoringNotification = null;
		Optional<TransferSeriesStatusEntity> firstSeriesOpt = studySeries.stream().findFirst();

		if (firstSeriesOpt.isPresent()) {
			TransferSeriesStatusEntity firstSeries = firstSeriesOpt.get();
			transferMonitoringNotification = new TransferMonitoringNotification();

			// If email notification should be sent
			transferMonitoringNotification
				.setEmailSendingEnabled(firstSeries.getDestinationEntity().isActivateNotification());

			// Build the serie notification part, keeping only series with something new
			// to
			// report since the last notification (a non-zero sent / not-sent delta). A
			// series touched only by retries or duplicate re-sends has no delta and is
			// dropped; if none remain there is nothing to notify for this study.
			Map<Long, Set<String>> reasonsBySeries = loadReasons(studySeries);
			boolean isDeIdentify = firstSeries.getDestinationEntity().isDesidentification();
			List<SerieSummaryNotification> serieSummaries = studySeries.stream()
				.map(series -> buildSerieSummaryNotification(isDeIdentify, series,
						reasonsBySeries.getOrDefault(series.getId(), Set.of())))
				.filter(ssm -> ssm.getNbTransferSent() > 0 || ssm.getNbTransferNotSent() > 0)
				.toList();
			if (serieSummaries.isEmpty()) {
				return null;
			}
			transferMonitoringNotification.setSerieSummaryNotifications(serieSummaries);

			// Has at least one file not transferred
			boolean hasAtLeastOneFileInError = transferMonitoringNotification.getSerieSummaryNotifications()
				.stream()
				.anyMatch(SerieSummaryNotification::isContainsError);

			boolean hasAtLeastOneFileNotSent = transferMonitoringNotification.getSerieSummaryNotifications()
				.stream()
				.anyMatch(ssm -> ssm.getNbTransferNotSent() > 0);

			// Temporary disable send of notification de-identified and in error
			// TODO: have a discussion with business to know how to handle such cases
			if (hasAtLeastOneFileNotSent && firstSeries.getDestinationEntity().isDesidentification()
					&& firstSeries.getDestinationEntity().isActivateNotification()) {
				return null;
			}

			// Check if we should use original or de-identified values
			boolean useOriginalValues = determineUseOfOriginalOrDeIdentifyValues(hasAtLeastOneFileNotSent,
					firstSeries.getDestinationEntity().isDesidentification());

			// Set values in transferMonitoringNotification
			buildTransferMonitoringNotificationSetValues(transferMonitoringNotification, firstSeries,
					hasAtLeastOneFileInError, hasAtLeastOneFileNotSent, useOriginalValues);
		}
		return transferMonitoringNotification;
	}

	/** Populates the notification fields (recipients, study identifiers, subject). */
	private void buildTransferMonitoringNotificationSetValues(
			TransferMonitoringNotification transferMonitoringNotification, TransferSeriesStatusEntity series,
			boolean hasAtLeastOneFileInError, boolean hasAtLeastOneFileRejected, boolean useOriginalValues) {
		transferMonitoringNotification
			.setFrom(SystemPropertyUtil.retrieveSystemProperty("MAIL_SMTP_SENDER", mailSender));
		transferMonitoringNotification.setTo(series.getDestinationEntity().getNotify());
		transferMonitoringNotification
			.setPatientId(useOriginalValues ? series.getPatientIdOriginal() : series.getPatientIdToSend());
		transferMonitoringNotification
			.setStudyUid(useOriginalValues ? series.getStudyUidOriginal() : series.getStudyUidToSend());
		transferMonitoringNotification.setAccessionNumber(
				useOriginalValues ? series.getAccessionNumberOriginal() : series.getAccessionNumberToSend());
		transferMonitoringNotification.setStudyDescription(
				useOriginalValues ? series.getStudyDescriptionOriginal() : series.getStudyDescriptionToSend());
		transferMonitoringNotification
			.setStudyDate(useOriginalValues ? series.getStudyDateOriginal() : series.getStudyDateToSend());
		transferMonitoringNotification.setSource(series.getForwardNodeEntity().getFwdAeTitle());
		transferMonitoringNotification
			.setDestination(Objects.equals(series.getDestinationEntity().getDestinationType(), DestinationType.dicom)
					? series.getDestinationEntity().toStringDicomNotificationDestination()
					: series.getDestinationEntity().getUrl());
		transferMonitoringNotification
			.setSubject(buildSubject(hasAtLeastOneFileInError, hasAtLeastOneFileRejected, useOriginalValues, series));
	}

	/**
	 * Builds the email subject: optional error/rejection prefix followed by the
	 * configured pattern.
	 */
	private String buildSubject(boolean hasAtLeastOneFileInError, boolean hasAtLeastOneFileRejected,
			boolean useOriginalValues, TransferSeriesStatusEntity series) {
		StringBuilder subject = new StringBuilder();
		if (hasAtLeastOneFileInError) {
			String errorPrefix = series.getDestinationEntity().getNotifyObjectErrorPrefix();
			if (errorPrefix != null && !errorPrefix.isEmpty()) {
				subject.append(errorPrefix);
				subject.append(Notification.SPACE);
			}
		}
		else if (hasAtLeastOneFileRejected) {
			String rejectPrefix = series.getDestinationEntity().getNotifyObjectRejectionPrefix();
			if (rejectPrefix != null && !rejectPrefix.isEmpty()) {
				subject.append(rejectPrefix);
				subject.append(Notification.SPACE);
			}
		}
		subject.append(String.format(series.getDestinationEntity().getNotifyObjectPattern(),
				buildSubjectValues(useOriginalValues, series)));
		return subject.toString();
	}

	/**
	 * Builds the subject placeholder values selected by the destination's
	 * notifyObjectValues.
	 */
	private Object[] buildSubjectValues(boolean useOriginalValues, TransferSeriesStatusEntity series) {
		List<String> subjectValues = new ArrayList<>();

		// Determine the values to add to the subject depending on the params set in the
		// destination
		String[] notifyObjectValues = (series.getDestinationEntity().getNotifyObjectValues() == null
				? Notification.DEFAULT_SUBJECT_VALUES : series.getDestinationEntity().getNotifyObjectValues())
			.split(Notification.COMMA_SEPARATOR);

		// Select the values to add
		for (String notifyObjectValue : notifyObjectValues) {
			if (Objects.equals(Notification.PARAM_PATIENT_ID, notifyObjectValue)) {
				// Patient ID
				subjectValues.add(buildSubjectValue(useOriginalValues, series.getPatientIdOriginal(),
						series.getPatientIdToSend()));
			}
			else if (Objects.equals(Notification.PARAM_STUDY_DESCRIPTION, notifyObjectValue)) {
				// Study description
				subjectValues.add(buildSubjectValue(useOriginalValues, series.getStudyDescriptionOriginal(),
						series.getStudyDescriptionToSend()));
			}
			else if (Objects.equals(Notification.PARAM_STUDY_INSTANCE_UID, notifyObjectValue)) {
				// Study uid
				subjectValues.add(
						buildSubjectValue(useOriginalValues, series.getStudyUidOriginal(), series.getStudyUidToSend()));
			}
			else if (Objects.equals(Notification.PARAM_STUDY_DATE, notifyObjectValue)) {
				// Study date
				subjectValues.add(useOriginalValues
						? series.getStudyDateOriginal() == null ? Notification.EMPTY_STRING
								: series.getStudyDateOriginal().toString()
						: series.getStudyDateToSend() == null ? Notification.EMPTY_STRING
								: series.getStudyDateToSend().toString());
			}
		}
		return subjectValues.toArray();
	}

	/** Returns the original or to-send value, or an empty string when null. */
	private String buildSubjectValue(boolean useOriginalValues, String original, String toSend) {
		return useOriginalValues ? original == null ? Notification.EMPTY_STRING : original
				: toSend == null ? Notification.EMPTY_STRING : toSend;
	}

	/**
	 * Builds the summary for a single series from its aggregated counters and reasons.
	 */
	private SerieSummaryNotification buildSerieSummaryNotification(boolean isDestinationDeIdentify,
			TransferSeriesStatusEntity series, Set<String> reasons) {
		SerieSummaryNotification serieSummaryNotification = new SerieSummaryNotification();
		// Report only the delta since this series was last notified (current counters
		// minus the snapshot taken at the previous notification), so outcomes already
		// reported are not counted again when the row is touched by a later transfer.
		// Not-transferred uses the delivery counters (errors + excluded), the same
		// buckets
		// the monitoring view shows; it is not derived as instances minus sent because
		// retries, duplicate re-sends and blank-UID events inflate the instances counter.
		long sentDelta = Math.max(0, series.getSent() - series.getNotifiedSent());
		long errorsDelta = Math.max(0, series.getErrors() - series.getNotifiedErrors());
		long excludedDelta = Math.max(0, series.getExcluded() - series.getNotifiedExcluded());
		serieSummaryNotification.setNbTransferSent(sentDelta);
		serieSummaryNotification.setNbTransferNotSent(errorsDelta + excludedDelta);
		serieSummaryNotification.setContainsError(errorsDelta > 0);
		// Distinct reasons (truncated for the email summary: the full reason stays in the
		// database and is shown in the monitoring view)
		serieSummaryNotification.setUnTransferedReasons(reasons.stream()
			.filter(Objects::nonNull)
			.map(reason -> StringUtil.getTruncatedString(reason, REASON_EMAIL_MAX_LENGTH, StringUtil.Suffix.THREE_PTS))
			.collect(Collectors.toSet()));
		serieSummaryNotification
			.setTransferredModalities(series.getModality() == null ? Set.of() : Set.of(series.getModality()));
		serieSummaryNotification.setTransferredSopClassUid(splitDistinct(series.getSopClassUids()));

		// Flag to know if we should use original or de-identify values
		boolean useOriginalValues = determineUseOfOriginalOrDeIdentifyValues(
				serieSummaryNotification.getNbTransferNotSent() > 0, isDestinationDeIdentify);
		serieSummaryNotification
			.setSerieUid(useOriginalValues ? series.getSerieUidOriginal() : series.getSerieUidToSend());
		serieSummaryNotification.setSerieDescription(
				useOriginalValues ? series.getSerieDescriptionOriginal() : series.getSerieDescriptionToSend());
		serieSummaryNotification
			.setSerieDate(useOriginalValues ? series.getSerieDateOriginal() : series.getSerieDateToSend());
		return serieSummaryNotification;
	}

	/**
	 * Original values are used when a transfer failed or the destination is not
	 * de-identified.
	 */
	private boolean determineUseOfOriginalOrDeIdentifyValues(boolean hasTransferNotSent,
			boolean isDestinationDeIdentify) {
		return hasTransferNotSent || !isDestinationDeIdentify;
	}

	/** Groups the series rows by source (forward node) then by original study UID. */
	private Map<Long, Map<String, List<TransferSeriesStatusEntity>>> gatherSeriesBySourceAndStudy(
			List<TransferSeriesStatusEntity> series) {
		return series.stream()
			.collect(Collectors.groupingBy(TransferSeriesStatusEntity::getForwardNodeId,
					Collectors.groupingBy(s -> StringUtils.defaultString(s.getStudyUidOriginal()))));
	}

	/** Maps each series id to its distinct error reasons. */
	private Map<Long, Set<String>> loadReasons(List<TransferSeriesStatusEntity> series) {
		List<Long> ids = series.stream().map(TransferSeriesStatusEntity::getId).toList();
		if (ids.isEmpty()) {
			return Map.of();
		}
		return transferSeriesReasonRepo.findBySeriesStatusIdIn(ids)
			.stream()
			.collect(Collectors.groupingBy(TransferSeriesReasonEntity::getSeriesStatusId,
					Collectors.mapping(TransferSeriesReasonEntity::getReason, Collectors.toSet())));
	}

	/** Splits a comma-joined set into distinct non-blank values. */
	private Set<String> splitDistinct(String joined) {
		if (StringUtils.isBlank(joined)) {
			return Set.of();
		}
		return Arrays.stream(joined.split(",")).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
	}

	/** Returns the series rows for the destination touched after the last email check. */
	private List<TransferSeriesStatusEntity> retrieveSeriesDestinationLastCheck(DestinationEntity destinationEntity,
			LocalDateTime lastCheck) {
		return lastCheck == null ? transferSeriesStatusRepo.findByDestinationId(destinationEntity.getId())
				: transferSeriesStatusRepo.findByDestinationIdAndLastSeenAfter(destinationEntity.getId(), lastCheck);
	}

	/**
	 * True when notifications are enabled, no transfer is in progress, and both the extra
	 * series-completion delay and the destination's notify interval have elapsed.
	 */
	private boolean checkDestinationLastVerification(DestinationEntity destinationEntity) {
		return !destinationEntity.isTransferInProgress() && destinationEntity.getLastTransfer() != null
		// Add extra timer delay in order to insure transfer of serie is over
				&& destinationEntity.getLastTransfer()
					.plusSeconds(Notification.EXTRA_TIMER_DELAY)
					.isBefore(LocalDateTime.now(ZoneId.of("CET")))
				&& (destinationEntity.getEmailLastCheck() == null || destinationEntity.getEmailLastCheck()
					.plusSeconds(destinationEntity.getNotifyInterval().longValue())
					.isBefore(LocalDateTime.now(ZoneId.of("CET"))));
	}

	/** Renders the Thymeleaf template and sends the notification email. */
	private void prepareAndSendNotification(TransferMonitoringNotification transferMonitoringNotification) {
		Context context = new Context();
		context.setVariable(Notification.CONTEXT_THYMELEAF, transferMonitoringNotification);
		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
		try {
			helper.setSubject(transferMonitoringNotification.getSubject());
			helper.setText(templateEngine.process(Notification.TEMPLATE_THYMELEAF, context), true);
			helper.setTo(InternetAddress.parse(transferMonitoringNotification.getTo()));
			helper.setFrom(transferMonitoringNotification.getFrom());
		}
		catch (MessagingException e) {
			log.error("Notification error when preparing email to send: {}", e.getMessage());
		}

		try {
			javaMailSender.send(mimeMessage);
		}
		catch (Exception e) {
			log.error("Notification error when sending email: {}", e.getMessage());
		}

	}

}
