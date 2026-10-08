/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.config;

import java.util.Objects;
import java.util.Properties;
import org.jspecify.annotations.Nullable;
import org.karnak.backend.util.SystemPropertyUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.weasis.core.util.StringUtil;

@Configuration
public class EmailConfig {

	@Bean
	public JavaMailSender getJavaMailSender() {
		String mailSmtpPort = Objects
			.requireNonNullElse(SystemPropertyUtil.retrieveSystemProperty("MAIL_SMTP_PORT", "0"), "0");
		@Nullable String mailSmtpUser = SystemPropertyUtil.retrieveSystemProperty("MAIL_SMTP_USER", null);
		@Nullable String mailAuthType = SystemPropertyUtil.retrieveSystemProperty("MAIL_SMTP_TYPE", null);

		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender
			.setHost(Objects.requireNonNullElse(SystemPropertyUtil.retrieveSystemProperty("MAIL_SMTP_HOST", null), ""));
		mailSender.setPort(Integer.parseInt(mailSmtpPort));
		mailSender.setUsername(Objects.requireNonNullElse(mailSmtpUser, ""));
		mailSender.setPassword(
				Objects.requireNonNullElse(SystemPropertyUtil.retrieveSystemProperty("MAIL_SMTP_SECRET", null), ""));
		mailSender.setDefaultEncoding("utf-8");

		Properties props = mailSender.getJavaMailProperties();
		props.put("mail.transport.protocol", "smtp");
		// Authentication value should be "SSL" or "STARTTLS"
		if (StringUtil.hasText(mailAuthType)) {
			configureAuthentication(props, Objects.requireNonNullElse(mailAuthType, ""), mailSmtpPort,
					Objects.requireNonNullElse(mailSmtpUser, ""));
		}
		return mailSender;
	}

	/** Enables SMTP auth, using SSL or STARTTLS depending on {@code mailAuthType}. */
	private static void configureAuthentication(Properties props, String mailAuthType, String mailSmtpPort,
			String mailSmtpUser) {
		props.put("mail.smtp.auth", "true");
		if (Objects.equals("SSL", mailAuthType)) {
			props.put("mail.smtp.socketFactory.port", mailSmtpPort);
			props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			props.put("mail.smtp.ssl.checkserveridentity", true);
		}
		else {
			props.put("mail.smtp.starttls.enable", "true");
		}
		props.setProperty("mail.smtp.submitter", mailSmtpUser);
	}

}
