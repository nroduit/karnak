/*
 * Copyright (c) 2021 Karnak Team and other contributors.
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.karnak.backend.constant.Notification;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.TransferStatusEntity;
import org.karnak.backend.data.repo.DestinationRepo;
import org.karnak.backend.data.repo.TransferStatusRepo;
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

/**
 * Handle notifications
 */
@Service
@Slf4j
public class NotificationService {

	@Value("${mail.sender}")
	private String mailSender;

	// Services
	private final TemplateEngine templateEngine;

	private final JavaMailSender javaMailSender;

	// Repositories
	private final TransferStatusRepo transferStatusRepo;

	private final DestinationRepo destinationRepo;

	@Autowired
	public NotificationService(final TemplateEngine templateEngine, final JavaMailSender javaMailSender,
			final TransferStatusRepo transferStatusRepo, final DestinationRepo destinationRepo) {
		this.templateEngine = templateEngine;
		this.javaMailSender = javaMailSender;
		this.transferStatusRepo = transferStatusRepo;
		this.destinationRepo = destinationRepo;
	}

	/**
	 * In a regular period of time, check for new inputs in the transfer notifications,
	 * build the notifications and send them via email.
	 */
	@Scheduled(fixedRate = 180 * 1000)
	public void determineNotificationToSend() {
		buildNotificationsToSend().forEach(this::prepareAndSendNotification);
	}

	/**
	 * Build transfer notification to send
	 * @return notifications to send
	 */
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
				// Retrieve all TransferStatusEntities for this destination after the
				// last email check
				List<TransferStatusEntity> transferStatusEntitiesDestinationsLastCheck = retrieveTransferStatusDestinationLastCheck(
						destinationEntity, previousCheck);
				// Gather TransferStatus by Source and Study: <Source , <Study,
				// List<TransferStatus>>>
				Map<Long, Map<String, List<TransferStatusEntity>>> transferStatusBySourceAndStudy = gatherTransferStatusBySourceAndStudy(
						transferStatusEntitiesDestinationsLastCheck);
				// Build the notifications to send
				buildTransferMonitoringNotifications(transferMonitoringNotifications, transferStatusBySourceAndStudy);
			});
		return transferMonitoringNotifications;
	}

	/**
	 * Build transfer notifications to send.
	 * @param transferMonitoringNotifications List of notifications to fill
	 * @param transferStatusBySourceAndStudy Map of TransferStatus by Source and Study:
	 * <br>
	 * <Source , <Study, List<TransferStatus>>>
	 */
	private void buildTransferMonitoringNotifications(
			List<TransferMonitoringNotification> transferMonitoringNotifications,
			Map<Long, Map<String, List<TransferStatusEntity>>> transferStatusBySourceAndStudy) {
		transferStatusBySourceAndStudy.forEach(
				// By source
				(source, transferStatusByStudy) -> {
					transferStatusByStudy.forEach(
							// By study
							(study, transferStatusEntities) -> {
								// Build the transfer notification
								TransferMonitoringNotification transferMonitoringNotification = buildTransferMonitoringNotification(
										transferStatusEntities);
								if (transferMonitoringNotification != null) {
									transferMonitoringNotifications.add(transferMonitoringNotification);
								}
							});
				});
	}

	/**
	 * Build TransferMonitoringNotification depending on transferStatusEntities in
	 * parameter. Calculate number of success/errors, determine distinct reasons of not
	 * transferring and select values to display in the notification (originals or
	 * de-identified) depending on the flag deidentification in the destination.
	 * @param transferStatusEntities TransferStatusEntity to evaluate
	 * @return populated TransferMonitoringNotification
	 */
	private TransferMonitoringNotification buildTransferMonitoringNotification(
			List<TransferStatusEntity> transferStatusEntities) {
		TransferMonitoringNotification transferMonitoringNotification = null;
		Optional<TransferStatusEntity> firstTransferStatusOpt = transferStatusEntities.stream().findFirst();

		if (firstTransferStatusOpt.isPresent()) {
			TransferStatusEntity firstTransferStatus = firstTransferStatusOpt.get();
			transferMonitoringNotification = new TransferMonitoringNotification();

			// Build the serie notification part
			transferMonitoringNotification.setSerieSummaryNotifications(buildSeriesSummaryNotification(
					transferStatusEntities, firstTransferStatus.getDestinationEntity().isDesidentification()));

			// Has at least one file not transferred
			boolean hasAtLeastOneFileNotTransferred = transferMonitoringNotification.getSerieSummaryNotifications()
				.stream()
				.anyMatch(ssm -> ssm.getNbTransferNotSent() > 0);

			// Temporary disable send of notification de-identified and in error
			// TODO: have a discussion with business to know how to handle such cases
			if (hasAtLeastOneFileNotTransferred && firstTransferStatus.getDestinationEntity().isDesidentification()) {
				return null;
			}

			// Check if we should use original or de-identified values
			boolean useOriginalValues = determineUseOfOriginalOrDeIdentifyValues(hasAtLeastOneFileNotTransferred,
					firstTransferStatus.getDestinationEntity().isDesidentification());

			// Set values in transferMonitoringNotification
			buildTransferMonitoringNotificationSetValues(transferMonitoringNotification, firstTransferStatus,
					hasAtLeastOneFileNotTransferred, useOriginalValues);
		}
		return transferMonitoringNotification;
	}

	/**
	 * Set values in transferMonitoringNotification built
	 * @param transferMonitoringNotification TransferMonitoringNotification built
	 * @param transferStatusEntity Transfer status
	 * @param hasAtLeastOneFileNotTransferred Has at least one file not transferred
	 * @param useOriginalValues Flag to know if we should use original values
	 */
	private void buildTransferMonitoringNotificationSetValues(
			TransferMonitoringNotification transferMonitoringNotification, TransferStatusEntity transferStatusEntity,
			boolean hasAtLeastOneFileNotTransferred, boolean useOriginalValues) {
		transferMonitoringNotification
			.setFrom(SystemPropertyUtil.retrieveSystemProperty("MAIL_SMTP_SENDER", mailSender));
		transferMonitoringNotification.setTo(transferStatusEntity.getDestinationEntity().getNotify());
		transferMonitoringNotification.setPatientId(useOriginalValues ? transferStatusEntity.getPatientIdOriginal()
				: transferStatusEntity.getPatientIdToSend());
		transferMonitoringNotification.setStudyUid(useOriginalValues ? transferStatusEntity.getStudyUidOriginal()
				: transferStatusEntity.getStudyUidToSend());
		transferMonitoringNotification.setAccessionNumber(useOriginalValues
				? transferStatusEntity.getAccessionNumberOriginal() : transferStatusEntity.getAccessionNumberToSend());
		transferMonitoringNotification
			.setStudyDescription(useOriginalValues ? transferStatusEntity.getStudyDescriptionOriginal()
					: transferStatusEntity.getStudyDescriptionToSend());
		transferMonitoringNotification.setStudyDate(useOriginalValues ? transferStatusEntity.getStudyDateOriginal()
				: transferStatusEntity.getStudyDateToSend());
		transferMonitoringNotification.setSource(transferStatusEntity.getForwardNodeEntity().getFwdAeTitle());
		transferMonitoringNotification.setDestination(
				Objects.equals(transferStatusEntity.getDestinationEntity().getDestinationType(), DestinationType.dicom)
						? transferStatusEntity.getDestinationEntity().toStringDicomNotificationDestination()
						: transferStatusEntity.getDestinationEntity().getUrl());
		transferMonitoringNotification
			.setSubject(buildSubject(hasAtLeastOneFileNotTransferred, useOriginalValues, transferStatusEntity));
	}

	/**
	 * Build notification subject email
	 * @param hasAtLeastOneFileNotTransferred Flag to know if there is at least one file
	 * not transferred
	 * @param useOriginalValues Check if we should use original or de-identified values
	 * @param transferStatusEntity TransferStatusEntity to evaluate
	 * @return Subject built
	 */
	private String buildSubject(boolean hasAtLeastOneFileNotTransferred, boolean useOriginalValues,
			TransferStatusEntity transferStatusEntity) {
		StringBuilder subject = new StringBuilder();
		if (hasAtLeastOneFileNotTransferred) {
			subject.append(transferStatusEntity.getDestinationEntity().getNotifyObjectErrorPrefix());
			subject.append(Notification.SPACE);
		}
		subject.append(String.format(transferStatusEntity.getDestinationEntity().getNotifyObjectPattern(),
				buildSubjectValues(useOriginalValues, transferStatusEntity)));
		return subject.toString();
	}

	/**
	 * Build list of values to add to the subject
	 * @param useOriginalValues Flag to know if we should use original or de-identified
	 * values
	 * @param transferStatusEntity TransferStatusEntity to evaluate
	 * @return List of values to add to the subject
	 */
	private Object[] buildSubjectValues(boolean useOriginalValues, TransferStatusEntity transferStatusEntity) {
		List<String> subjectValues = new ArrayList<>();

		// Determine the values to add to the subject depending on the params set in the
		// destination
		String[] notifyObjectValues = (transferStatusEntity.getDestinationEntity().getNotifyObjectValues() == null
				? Notification.DEFAULT_SUBJECT_VALUES
				: transferStatusEntity.getDestinationEntity().getNotifyObjectValues())
			.split(Notification.COMMA_SEPARATOR);

		// Select the values to add
		for (String notifyObjectValue : notifyObjectValues) {
			if (Objects.equals(Notification.PARAM_PATIENT_ID, notifyObjectValue)) {
				// Patient ID
				subjectValues.add(buildSubjectValue(useOriginalValues, transferStatusEntity.getPatientIdOriginal(),
						transferStatusEntity.getPatientIdToSend()));
			}
			else if (Objects.equals(Notification.PARAM_STUDY_DESCRIPTION, notifyObjectValue)) {
				// Study description
				subjectValues
					.add(buildSubjectValue(useOriginalValues, transferStatusEntity.getStudyDescriptionOriginal(),
							transferStatusEntity.getStudyDescriptionToSend()));
			}
			else if (Objects.equals(Notification.PARAM_STUDY_INSTANCE_UID, notifyObjectValue)) {
				// Study uid
				subjectValues.add(buildSubjectValue(useOriginalValues, transferStatusEntity.getStudyUidOriginal(),
						transferStatusEntity.getStudyUidToSend()));
			}
			else if (Objects.equals(Notification.PARAM_STUDY_DATE, notifyObjectValue)) {
				// Study date
				subjectValues.add(useOriginalValues
						? transferStatusEntity.getStudyDateOriginal() == null ? Notification.EMPTY_STRING
								: transferStatusEntity.getStudyDateOriginal().toString()
						: transferStatusEntity.getStudyDateToSend() == null ? Notification.EMPTY_STRING
								: transferStatusEntity.getStudyDateToSend().toString());
			}
		}
		return subjectValues.toArray();
	}

	/**
	 * Build subject value, if null set empty string in the subject
	 * @param useOriginalValues Flag to know if we should use original value
	 * @param original Value original
	 * @param toSend Value transformed to send
	 * @return subject value
	 */
	private String buildSubjectValue(boolean useOriginalValues, String original, String toSend) {
		return useOriginalValues ? original == null ? Notification.EMPTY_STRING : original
				: toSend == null ? Notification.EMPTY_STRING : toSend;
	}

	/**
	 * Build transfer serie summary results
	 * @param transferStatusEntities TransferStatus to evaluate
	 * @param isDestinationDeIdentify Flag to know if the destination should be
	 * de-identify
	 * @return Serie summary notifications
	 */
	private List<SerieSummaryNotification> buildSeriesSummaryNotification(
			List<TransferStatusEntity> transferStatusEntities, boolean isDestinationDeIdentify) {
		List<SerieSummaryNotification> serieSummaryNotifications = new ArrayList<>();
		// Group by serie uid
		Map<String, List<TransferStatusEntity>> transferStatusEntitiesBySerieUid = transferStatusEntities.stream()
			.collect(Collectors.groupingBy(TransferStatusEntity::getSerieUidOriginal));
		transferStatusEntitiesBySerieUid.forEach((serieUidOriginal, transfersToEvaluate) -> {
			Optional<TransferStatusEntity> firstTransferStatusEntityOpt = transfersToEvaluate.stream().findFirst();
			if (firstTransferStatusEntityOpt.isPresent()) {
				TransferStatusEntity firstTransferStatusEntity = firstTransferStatusEntityOpt.get();
				// Build serie summary
				serieSummaryNotifications.add(buildSerieSummaryNotification(isDestinationDeIdentify,
						transfersToEvaluate, firstTransferStatusEntity));
			}
		});

		return serieSummaryNotifications;
	}

	/**
	 * Build summary notification for the serie in parameter
	 * @param isDestinationDeIdentify Flag to know if the destination should be
	 * de-identify
	 * @param transfersToEvaluate Transfers to evaluate
	 * @param transferStatusEntity TransferStatus containing general series values to set
	 * @return Summary notification for this serie
	 */
	private SerieSummaryNotification buildSerieSummaryNotification(boolean isDestinationDeIdentify,
			List<TransferStatusEntity> transfersToEvaluate, TransferStatusEntity transferStatusEntity) {
		SerieSummaryNotification serieSummaryNotification = new SerieSummaryNotification();
		// Number transfers sent
		serieSummaryNotification
			.setNbTransferSent(transfersToEvaluate.stream().filter(TransferStatusEntity::isSent).count());
		// Number transfers not sent
		serieSummaryNotification.setNbTransferNotSent(transfersToEvaluate.stream().filter(t -> !t.isSent()).count());
		// Distinct reasons
		serieSummaryNotification.setUnTransferedReasons(transfersToEvaluate.stream()
			.map(TransferStatusEntity::getReason)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet()));
		// Distinct modalities
		serieSummaryNotification.setTransferredModalities(transfersToEvaluate.stream()
			.map(TransferStatusEntity::getModality)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet()));
		// Distinct transfer syntax
		serieSummaryNotification.setTransferredSopClassUid(transfersToEvaluate.stream()
			.map(TransferStatusEntity::getSopClassUid)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet()));

		// Flag to know if we should use original or de-identify values
		boolean useOriginalValues = determineUseOfOriginalOrDeIdentifyValues(
				serieSummaryNotification.getNbTransferNotSent() > 0, isDestinationDeIdentify);
		serieSummaryNotification.setSerieUid(useOriginalValues ? transferStatusEntity.getSerieUidOriginal()
				: transferStatusEntity.getSerieUidToSend());
		serieSummaryNotification
			.setSerieDescription(useOriginalValues ? transferStatusEntity.getSerieDescriptionOriginal()
					: transferStatusEntity.getSerieDescriptionToSend());
		serieSummaryNotification.setSerieDate(useOriginalValues ? transferStatusEntity.getSerieDateOriginal()
				: transferStatusEntity.getSerieDateToSend());
		return serieSummaryNotification;
	}

	/**
	 * Determine if we have to use originals or de-identify values. If any transfer not
	 * sent or flag deidentification for the destination is false: use originals values
	 * @param hasTransferNotSent Flag to know if there are transfer not sent
	 * @param isDestinationDeIdentify Is deidentification requested for the destination
	 * @return true if we have to use originals values, false otherwise
	 */
	private boolean determineUseOfOriginalOrDeIdentifyValues(boolean hasTransferNotSent,
			boolean isDestinationDeIdentify) {
		return hasTransferNotSent || !isDestinationDeIdentify;
	}

	/**
	 * Gather TransferStatusEntities by Source and Study
	 * @param transferStatusEntities Entities to gather
	 * @return Map of <Source , <Study, List<TransferStatus>>>
	 */
	private Map<Long, Map<String, List<TransferStatusEntity>>> gatherTransferStatusBySourceAndStudy(
			List<TransferStatusEntity> transferStatusEntities) {
		return transferStatusEntities.stream()
			.collect(Collectors.groupingBy(TransferStatusEntity::getForwardNodeId,
					Collectors.groupingBy(TransferStatusEntity::getStudyUidOriginal)));
	}

	/**
	 * Retrieve TransferStatusEntities for this destination after the last email check
	 * @param destinationEntity Destination to look for
	 * @param lastCheck Last check for this destination
	 * @return TransferStatusEntity found
	 */
	private List<TransferStatusEntity> retrieveTransferStatusDestinationLastCheck(DestinationEntity destinationEntity,
			LocalDateTime lastCheck) {
		return lastCheck == null ? transferStatusRepo.findByDestinationId(destinationEntity.getId())
				: transferStatusRepo.findByDestinationIdAndTransferDateAfter(destinationEntity.getId(), lastCheck);
	}

	/**
	 * Check if notification should be sent depending on last check of notification
	 * (compare to notification interval of the destination and current date) <br>
	 * A transfer should not be in progress to send the notification and an extra timer of
	 * 10s is added for serie transfers which are not immediately sent each time (a delay
	 * is occurring between transfer of files in the same serie)
	 * @param destinationEntity Destination to evaluate
	 * @return true if the notification can be process, false otherwise
	 */
	private boolean checkDestinationLastVerification(DestinationEntity destinationEntity) {
		return !destinationEntity.isTransferInProgress() && destinationEntity.getLastTransfer() != null
		// Add extra timer delay in order to insure transfer of serie is over
				&& destinationEntity.getLastTransfer()
					.plusSeconds(Notification.EXTRA_TIMER_DELAY)
					.isBefore(LocalDateTime.now(ZoneId.of("CET")))
				&& (destinationEntity.getEmailLastCheck() == null || destinationEntity.getEmailLastCheck()
					.plusSeconds(destinationEntity.getNotifyInterval().longValue())
					.isBefore(LocalDateTime.now(ZoneId.of("CET"))))
				&& destinationEntity.isActivateNotification();
	}

	/**
	 * Prepare and send the notification with values in parameter
	 * @param transferMonitoringNotification Values to add to the thymeleaf template
	 */
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
		javaMailSender.send(mimeMessage);
	}

}
