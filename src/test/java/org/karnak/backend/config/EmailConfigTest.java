/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@DisplayNameGeneration(ReplaceUnderscores.class)
class EmailConfigTest {

	private final EmailConfig emailConfig = new EmailConfig();

	@AfterEach
	void clearProperties() {
		for (String key : new String[] { "MAIL_SMTP_HOST", "MAIL_SMTP_PORT", "MAIL_SMTP_USER", "MAIL_SMTP_TYPE",
				"MAIL_SMTP_SECRET" }) {
			System.clearProperty(key);
		}
	}

	@Test
	void builds_a_sender_from_the_smtp_system_properties() {
		System.setProperty("MAIL_SMTP_HOST", "smtp.example.com");
		System.setProperty("MAIL_SMTP_PORT", "25");
		System.setProperty("MAIL_SMTP_USER", "user@example.com");
		System.setProperty("MAIL_SMTP_SECRET", "pw");

		JavaMailSenderImpl sender = (JavaMailSenderImpl) emailConfig.getJavaMailSender();

		assertEquals("smtp.example.com", sender.getHost());
		assertEquals(25, sender.getPort());
		assertEquals("user@example.com", sender.getUsername());
		assertEquals("pw", sender.getPassword());
		// No auth type configured -> no SMTP auth flag.
		assertNull(sender.getJavaMailProperties().get("mail.smtp.auth"));
	}

	@Test
	void enables_ssl_authentication_when_requested() {
		System.setProperty("MAIL_SMTP_PORT", "465");
		System.setProperty("MAIL_SMTP_USER", "user@example.com");
		System.setProperty("MAIL_SMTP_TYPE", "SSL");

		JavaMailSenderImpl sender = (JavaMailSenderImpl) emailConfig.getJavaMailSender();

		Properties props = sender.getJavaMailProperties();
		assertEquals("true", props.get("mail.smtp.auth"));
		assertEquals("javax.net.ssl.SSLSocketFactory", props.get("mail.smtp.socketFactory.class"));
	}

	@Test
	void falls_back_to_starttls_for_any_other_auth_type() {
		System.setProperty("MAIL_SMTP_PORT", "587");
		System.setProperty("MAIL_SMTP_USER", "user@example.com");
		System.setProperty("MAIL_SMTP_TYPE", "STARTTLS");

		JavaMailSenderImpl sender = (JavaMailSenderImpl) emailConfig.getJavaMailSender();

		Properties props = sender.getJavaMailProperties();
		assertEquals("true", props.get("mail.smtp.auth"));
		assertEquals("true", props.get("mail.smtp.starttls.enable"));
	}

}