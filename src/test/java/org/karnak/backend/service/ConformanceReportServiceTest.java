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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.repo.DestinationRepo;
import org.karnak.backend.model.event.ConformanceCollectEvent;
import org.karnak.backend.model.standard.StandardDICOM;
import org.karnak.backend.model.validation.ImageIdentityCheckInput;
import org.karnak.backend.model.validation.InstanceConformanceData;
import org.karnak.backend.model.validation.MetadataSnapshot;
import org.karnak.backend.service.profilepipe.DeidentifyImageService;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ConformanceReportServiceTest {

	private static final Instant NOW = Instant.parse("2026-06-12T10:00:00Z");

	private static StandardDICOM standardDICOM;

	private final TemplateEngine templateEngineMock = Mockito.mock(TemplateEngine.class);

	private final JavaMailSender javaMailSenderMock = Mockito.mock(JavaMailSender.class);

	private final DestinationRepo destinationRepoMock = Mockito.mock(DestinationRepo.class);

	private final DeidentifyImageService deidentifyImageServiceMock = Mockito.mock(DeidentifyImageService.class);

	private ConformanceReportService service;

	private DestinationEntity destinationEntity;

	@BeforeAll
	static void loadStandard() {
		standardDICOM = new StandardDICOM();
	}

	@BeforeEach
	void setUp() {
		destinationEntity = new DestinationEntity();
		destinationEntity.setId(2L);
		destinationEntity.setNotify("first@karnak.org, second@karnak.org");
		destinationEntity.setAeTitle("DEST_AET");
		when(destinationRepoMock.findById(2L)).thenReturn(Optional.of(destinationEntity));
		when(javaMailSenderMock.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
		when(templateEngineMock.process(Mockito.eq(ConformanceReportService.TEMPLATE_THYMELEAF), any()))
			.thenReturn("<html/>");

		service = new ConformanceReportService(templateEngineMock, javaMailSenderMock, standardDICOM,
				destinationRepoMock, deidentifyImageServiceMock);
		ReflectionTestUtils.setField(service, "mailSender", "karnak@karnak.org");
		ReflectionTestUtils.setField(service, "karnakVersion", "test");
		ReflectionTestUtils.setField(service, "idleTimeoutSeconds", 300L);
		ReflectionTestUtils.setField(service, "maxStudyLifetimeSeconds", 14400L);
		service.setClock(Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private static ConformanceCollectEvent event(String sopInstanceUid) {
		var dcm = new Attributes();
		dcm.setString(Tag.SOPClassUID, VR.UI, UID.CTImageStorage);
		dcm.setString(Tag.SOPInstanceUID, VR.UI, sopInstanceUid);
		dcm.setString(Tag.StudyInstanceUID, VR.UI, "1.2.3.4");
		dcm.setString(Tag.SeriesInstanceUID, VR.UI, "1.2.3.4.5");
		dcm.setString(Tag.PatientID, VR.LO, "PSEUDO-1");
		dcm.setString(Tag.PatientName, VR.PN, "PSEUDO^A");
		dcm.setString(Tag.Modality, VR.CS, "CT");
		return new ConformanceCollectEvent(InstanceConformanceData.of(1L, 2L, "SRC_AET", UID.ExplicitVRLittleEndian,
				true, null, false, false, true, MetadataSnapshot.of(dcm)));
	}

	private static ConformanceCollectEvent eventWithImageIdentity(String sopInstanceUid) {
		var dcm = new Attributes();
		dcm.setString(Tag.SOPClassUID, VR.UI, UID.SecondaryCaptureImageStorage);
		dcm.setString(Tag.SOPInstanceUID, VR.UI, sopInstanceUid);
		dcm.setString(Tag.StudyInstanceUID, VR.UI, "1.2.3.4");
		dcm.setString(Tag.SeriesInstanceUID, VR.UI, "1.2.3.4.5");
		dcm.setString(Tag.PatientID, VR.LO, "PSEUDO-1");
		dcm.setString(Tag.PatientName, VR.PN, "PSEUDO^A");
		dcm.setString(Tag.Modality, VR.CS, "OT");
		var input = new ImageIdentityCheckInput(new byte[] { 1, 2, 3 }, Map.of("PatientName", "Doe^John"),
				UID.ExplicitVRLittleEndian);
		return new ConformanceCollectEvent(InstanceConformanceData.of(1L, 2L, "SRC_AET", UID.ExplicitVRLittleEndian,
				true, null, false, false, true, MetadataSnapshot.of(dcm), input));
	}

	@Test
	void report_is_emailed_to_the_destination_recipients_once_the_study_is_idle() throws Exception {
		service.onConformanceCollect(event("1.2.3.4.5.1"));
		service.onConformanceCollect(event("1.2.3.4.5.2"));
		assertEquals(1, service.getStudies().size());

		service.setClock(Clock.fixed(NOW.plus(Duration.ofSeconds(301)), ZoneOffset.UTC));
		service.flushIdleStudies();

		ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
		verify(javaMailSenderMock).send(messageCaptor.capture());
		assertEquals(2, messageCaptor.getValue().getAllRecipients().length);
		assertTrue(messageCaptor.getValue().getSubject().contains("DICOM conformance report"));
		assertTrue(service.getStudies().isEmpty());
	}

	@Test
	void study_still_active_is_not_flushed() {
		service.onConformanceCollect(event("1.2.3.4.5.1"));

		service.setClock(Clock.fixed(NOW.plus(Duration.ofSeconds(100)), ZoneOffset.UTC));
		service.flushIdleStudies();

		verify(javaMailSenderMock, never()).send(any(MimeMessage.class));
		assertEquals(1, service.getStudies().size());
	}

	@Test
	void study_exceeding_max_lifetime_is_flushed_even_when_active() {
		service.onConformanceCollect(event("1.2.3.4.5.1"));

		// Keep the study active but move past the maximum lifetime
		service.setClock(Clock.fixed(NOW.plus(Duration.ofSeconds(14500)), ZoneOffset.UTC));
		service.onConformanceCollect(event("1.2.3.4.5.2"));
		service.flushIdleStudies();

		verify(javaMailSenderMock).send(any(MimeMessage.class));
		assertTrue(service.getStudies().isEmpty());
	}

	@Test
	void report_is_dropped_when_destination_has_no_notification_email() {
		destinationEntity.setNotify("");
		service.onConformanceCollect(event("1.2.3.4.5.1"));

		service.setClock(Clock.fixed(NOW.plus(Duration.ofSeconds(301)), ZoneOffset.UTC));
		service.flushIdleStudies();

		verify(javaMailSenderMock, never()).send(any(MimeMessage.class));
		assertTrue(service.getStudies().isEmpty());
	}

	@Test
	void report_is_dropped_when_destination_no_longer_exists() {
		when(destinationRepoMock.findById(2L)).thenReturn(Optional.empty());
		service.onConformanceCollect(event("1.2.3.4.5.1"));

		service.setClock(Clock.fixed(NOW.plus(Duration.ofSeconds(301)), ZoneOffset.UTC));
		service.flushIdleStudies();

		verify(javaMailSenderMock, never()).send(any(MimeMessage.class));
	}

	@Test
	void flush_all_sends_every_pending_report() {
		service.onConformanceCollect(event("1.2.3.4.5.1"));

		service.flushAll();

		verify(javaMailSenderMock).send(any(MimeMessage.class));
		assertTrue(service.getStudies().isEmpty());
	}

	@Test
	void real_template_renders_a_self_contained_html_report() throws Exception {
		var resolver = new ClassLoaderTemplateResolver();
		resolver.setPrefix("templates/");
		resolver.setSuffix(".html");
		resolver.setCharacterEncoding("UTF-8");
		// SpEL evaluation, as in the Spring Boot auto-configured engine
		var realEngine = new SpringTemplateEngine();
		realEngine.setTemplateResolver(resolver);
		var realService = new ConformanceReportService(realEngine, javaMailSenderMock, standardDICOM,
				destinationRepoMock, deidentifyImageServiceMock);
		ReflectionTestUtils.setField(realService, "karnakVersion", "test-version");
		realService.setClock(Clock.fixed(NOW, ZoneOffset.UTC));

		realService.onConformanceCollect(event("1.2.3.4.5.1"));
		var accumulator = realService.getStudies().values().iterator().next();
		String html = realService.renderReport(accumulator.close(), destinationEntity);

		assertTrue(html.contains("Karnak DICOM Conformance Report"));
		assertTrue(html.contains("1.2.3.4"));
		assertTrue(html.contains("DEST_AET"));
		assertTrue(html.contains("test-version"));
		assertTrue(html.contains("CT Image Storage"));

		// Dump for manual inspection
		Files.writeString(Path.of("target", "conformance-report-sample.html"), html);
	}

	@Test
	void image_identity_check_lists_detected_tags_in_the_report() {
		when(deidentifyImageServiceMock.callReportingApi(any(), any(), any(), any(), any()))
			.thenReturn(List.of("PatientName", "PatientID"));

		var resolver = new ClassLoaderTemplateResolver();
		resolver.setPrefix("templates/");
		resolver.setSuffix(".html");
		resolver.setCharacterEncoding("UTF-8");
		var realEngine = new SpringTemplateEngine();
		realEngine.setTemplateResolver(resolver);
		var realService = new ConformanceReportService(realEngine, javaMailSenderMock, standardDICOM,
				destinationRepoMock, deidentifyImageServiceMock);
		ReflectionTestUtils.setField(realService, "karnakVersion", "test-version");
		realService.setClock(Clock.fixed(NOW, ZoneOffset.UTC));

		destinationEntity.setImageIdentityCheck(true);
		realService.onConformanceCollect(eventWithImageIdentity("1.2.3.4.5.1"));
		var accumulator = realService.getStudies().values().iterator().next();
		var report = accumulator.close();
		String html = realService.renderReport(report, destinationEntity);

		assertEquals(1, report.detectedIdentityTags().get("PatientName"));
		assertEquals(1, report.detectedIdentityTags().get("PatientID"));
		assertEquals(1, report.imageIdentityCheckedInstances());
		assertTrue(html.contains("Identifying data on image"));
		assertTrue(html.contains("PatientName"));
		assertTrue(html.contains("PatientID"));
	}

	@Test
	void image_identity_check_failure_is_reported_without_detected_tags() {
		when(deidentifyImageServiceMock.callReportingApi(any(), any(), any(), any(), any()))
			.thenThrow(new RuntimeException("service down"));

		service.onConformanceCollect(eventWithImageIdentity("1.2.3.4.5.1"));
		var report = service.getStudies().values().iterator().next().close();

		assertTrue(report.detectedIdentityTags().isEmpty());
		assertEquals(0, report.imageIdentityCheckedInstances());
		assertEquals(1, report.imageIdentityCheckErrors());
	}

}
