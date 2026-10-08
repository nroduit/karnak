/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class TransferMonitoringNotificationTest {

	private static TransferMonitoringNotification sample() {
		TransferMonitoringNotification notification = new TransferMonitoringNotification();
		notification.setSubject("subject");
		notification.setFrom("from@karnak");
		notification.setTo("to@karnak");
		notification.setPatientId("PID");
		notification.setStudyUid("1.2.3");
		notification.setAccessionNumber("ACC");
		notification.setStudyDescription("brain");
		notification.setStudyDate(LocalDateTime.of(2026, 6, 11, 9, 0));
		notification.setSource("SRC");
		notification.setDestination("DST");
		notification.setSerieSummaryNotifications(List.of(new SerieSummaryNotification()));
		return notification;
	}

	@Test
	void stores_and_exposes_all_fields() {
		TransferMonitoringNotification notification = sample();

		assertEquals("subject", notification.getSubject());
		assertEquals("from@karnak", notification.getFrom());
		assertEquals("to@karnak", notification.getTo());
		assertEquals("PID", notification.getPatientId());
		assertEquals("1.2.3", notification.getStudyUid());
		assertEquals("ACC", notification.getAccessionNumber());
		assertEquals("brain", notification.getStudyDescription());
		assertEquals(LocalDateTime.of(2026, 6, 11, 9, 0), notification.getStudyDate());
		assertEquals("SRC", notification.getSource());
		assertEquals("DST", notification.getDestination());
		assertEquals(1, notification.getSerieSummaryNotifications().size());
	}

	@Test
	void considers_two_instances_with_the_same_content_equal() {
		assertEquals(sample(), sample());
		assertEquals(sample().hashCode(), sample().hashCode());
	}

	@Test
	void differs_when_a_field_changes() {
		TransferMonitoringNotification other = sample();
		other.setSubject("changed");

		assertNotEquals(sample(), other);
	}

}