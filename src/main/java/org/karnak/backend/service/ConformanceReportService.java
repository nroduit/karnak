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

import jakarta.annotation.PreDestroy;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che3.data.UID;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.repo.DestinationRepo;
import org.karnak.backend.enums.DestinationType;
import org.karnak.backend.model.event.ConformanceCollectEvent;
import org.karnak.backend.model.standard.StandardDICOM;
import org.karnak.backend.model.validation.ConformanceReport;
import org.karnak.backend.model.validation.CuratedValidationRules;
import org.karnak.backend.model.validation.DicomConformanceValidator;
import org.karnak.backend.model.validation.InstanceConformanceData;
import org.karnak.backend.model.validation.InstanceValidationResult;
import org.karnak.backend.model.validation.StudyConformanceAccumulator;
import org.karnak.backend.model.validation.StudyKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Collects the conformance data of forwarded instances (published by ForwardService for
 * destinations with the buildConformanceReport option), validates them against the DICOM
 * standard and emails an HTML report per study once the transfer goes idle.
 */
@Service
@Slf4j
@NullUnmarked
public class ConformanceReportService {

	public static final String TEMPLATE_THYMELEAF = "conformanceReportEmail";

	private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

	// Services
	private final TemplateEngine templateEngine;

	private final JavaMailSender javaMailSender;

	private final StandardDICOM standardDICOM;

	// Repositories
	private final DestinationRepo destinationRepo;

	private final CuratedValidationRules rules;

	private final DicomConformanceValidator validator;

	private final Map<StudyKey, StudyConformanceAccumulator> studies = new ConcurrentHashMap<>();

	// Overridable for tests
	private Clock clock = Clock.systemDefaultZone();

	@Value("${mail.sender}")
	private String mailSender;

	@Value("${spring.application.version:Development}")
	private String karnakVersion;

	// Report is flushed when no new instance arrived for this period
	@Value("${conformance-report.idle-timeout-seconds:300}")
	private long idleTimeoutSeconds;

	// Safety cap: a study trickling in for longer than this is flushed anyway
	@Value("${conformance-report.max-study-lifetime-seconds:14400}")
	private long maxStudyLifetimeSeconds;

	// Sequence recursion depth used when a destination enables deep-sequence validation
	// (SR content tree, functional groups). Must match the snapshot depth in
	// ForwardService
	@Value("${conformance-report.max-sequence-depth:8}")
	private int maxSequenceDepth;

	@Autowired
	public ConformanceReportService(final TemplateEngine templateEngine, final JavaMailSender javaMailSender,
			final StandardDICOM standardDICOM, final DestinationRepo destinationRepo) {
		this.templateEngine = templateEngine;
		this.javaMailSender = javaMailSender;
		this.standardDICOM = standardDICOM;
		this.destinationRepo = destinationRepo;
		this.rules = CuratedValidationRules.load();
		this.validator = new DicomConformanceValidator(standardDICOM, rules);
	}

	/**
	 * Validates the forwarded instance and accumulates the result into its study batch.
	 */
	@Async
	@EventListener
	public void onConformanceCollect(ConformanceCollectEvent event) {
		InstanceConformanceData data = event.getInstanceConformanceData();
		try {
			int depth = data.deepSequenceValidation() ? maxSequenceDepth
					: DicomConformanceValidator.DEFAULT_MAX_SEQUENCE_DEPTH;
			InstanceValidationResult result = data.sent()
					? validator.validate(data.snapshot().metadata(), data.snapshot().bulkPresentTags(),
							data.transferSyntaxUid(), data.checkValueConformity(), depth, data.deidentified())
					: null;
			Instant now = clock.instant();
			// A concurrent flush may close the accumulator between lookup and add: retry
			// with a fresh one (the late instances produce a small follow-up report)
			while (true) {
				StudyConformanceAccumulator accumulator = studies.computeIfAbsent(data.studyKey(),
						key -> new StudyConformanceAccumulator(key, data.sourceAet(), data.deidentified(), rules, now));
				if (accumulator.add(data, result, now)) {
					return;
				}
				studies.remove(data.studyKey(), accumulator);
			}
		}
		catch (Exception e) {
			log.error("Cannot collect conformance data of instance {}", data.sopInstanceUid(), e);
		}
	}

	/**
	 * Flushes the study batches that went idle (or exceeded the maximum lifetime): builds
	 * the report and sends it by email.
	 */
	@Scheduled(fixedDelay = 60 * 1000)
	public void flushIdleStudies() {
		Instant now = clock.instant();
		for (StudyKey key : Set.copyOf(studies.keySet())) {
			StudyConformanceAccumulator accumulator = studies.get(key);
			if (accumulator == null) {
				continue;
			}
			boolean idle = accumulator.getLastUpdatedAt().plusSeconds(idleTimeoutSeconds).isBefore(now);
			boolean expired = accumulator.getCreatedAt().plusSeconds(maxStudyLifetimeSeconds).isBefore(now);
			if (idle || expired) {
				closeAndSend(key, accumulator);
			}
		}
	}

	/** Flushes every pending study, e.g. on shutdown. */
	@PreDestroy
	public void flushAll() {
		studies.forEach(this::closeAndSend);
	}

	private void closeAndSend(StudyKey key, StudyConformanceAccumulator accumulator) {
		if (!studies.remove(key, accumulator)) {
			return;
		}
		try {
			sendReport(accumulator.close());
		}
		catch (Exception e) {
			log.error("Cannot send the conformance report of study {} to destination {}", key.studyInstanceUid(),
					key.destinationId(), e);
		}
	}

	private void sendReport(ConformanceReport report) throws Exception {
		DestinationEntity destinationEntity = destinationRepo.findById(report.key().destinationId()).orElse(null);
		if (destinationEntity == null) {
			log.warn("Conformance report dropped: destination {} no longer exists", report.key().destinationId());
			return;
		}
		// The conformance report has its own recipient list; fall back to the
		// notification emails when it is left blank
		String recipients = StringUtils.defaultIfBlank(destinationEntity.getConformanceReportNotify(),
				destinationEntity.getNotify());
		if (StringUtils.isBlank(recipients)) {
			log.warn("Conformance report of study {} dropped: destination {} has no recipient email",
					report.key().studyInstanceUid(), report.key().destinationId());
			return;
		}

		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
		helper.setSubject(buildSubject(report));
		helper.setText(renderReport(report, destinationEntity), true);
		helper.setTo(InternetAddress.parse(recipients));
		helper.setFrom(mailSender);
		javaMailSender.send(mimeMessage);
		log.info("Conformance report of study {} sent for destination {}", report.key().studyInstanceUid(),
				report.key().destinationId());
	}

	String renderReport(ConformanceReport report, DestinationEntity destinationEntity) {
		Context context = new Context();
		context.setVariable("report", report);
		context.setVariable("destinationDescription", describeDestination(destinationEntity));
		context.setVariable("karnakVersion", karnakVersion);
		context.setVariable("dicomStandardSource", rules.getDicomStandardSource());
		context.setVariable("generatedAt", TIMESTAMP_FORMAT.withZone(ZoneId.systemDefault()).format(clock.instant()));
		context.setVariable("sopClassNames", uidNames(report.sopClassUids()));
		context.setVariable("transferSyntaxNames", uidNames(report.transferSyntaxUids()));
		return templateEngine.process(TEMPLATE_THYMELEAF, context);
	}

	String buildSubject(ConformanceReport report) {
		return "[Karnak] DICOM conformance report - %s (%d errors, %d warnings) - Study %s".formatted(
				report.passed() ? "PASSED" : "FAILED", report.errorCount(), report.warningCount(),
				StringUtils.abbreviate(report.key().studyInstanceUid(), 40));
	}

	private static String describeDestination(DestinationEntity destinationEntity) {
		if (StringUtils.isNotBlank(destinationEntity.getDescription())) {
			return destinationEntity.getDescription();
		}
		return destinationEntity.getDestinationType() == DestinationType.stow ? destinationEntity.getUrl()
				: destinationEntity.getAeTitle();
	}

	private static Map<String, String> uidNames(Set<String> uids) {
		Map<String, String> names = new LinkedHashMap<>();
		for (String uid : uids) {
			String name = UID.nameOf(uid);
			names.put(uid, name == null || "?".equals(name) ? uid : name);
		}
		return names;
	}

	void setClock(Clock clock) {
		this.clock = clock;
	}

	Map<StudyKey, StudyConformanceAccumulator> getStudies() {
		return studies;
	}

}
