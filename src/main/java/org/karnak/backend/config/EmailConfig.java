/*
 * Copyright (c) 2021 Karnak Team and other contributors.
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

		// retrieve system properties
		String mailSmtpPort = SystemPropertyUtil.retrieveSystemProperty("MAIL_SMTP_PORT", "0");
		String mailSmtpUser = SystemPropertyUtil.retrieveSystemProperty("MAIL_SMTP_USER", null);
		String mailAuthType = SystemPropertyUtil.retrieveSystemProperty("MAIL_SMTP_TYPE", null);

		// Configure JavaMailSender
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost(SystemPropertyUtil.retrieveSystemProperty("MAIL_SMTP_HOST", null));
		mailSender.setPort(Integer.parseInt(mailSmtpPort));
		mailSender.setUsername(mailSmtpUser);
		mailSender.setPassword(SystemPropertyUtil.retrieveSystemProperty("MAIL_SMTP_SECRET", null));
		mailSender.setDefaultEncoding("utf-8");

		// Additional properties
		Properties props = mailSender.getJavaMailProperties();
		props.put("mail.transport.protocol", "smtp");

		// Value with authentication should be "SSL" or "STARTTLS"
		if (StringUtil.hasText(mailAuthType)) {
			props.put("mail.smtp.auth", "true");
			if (Objects.equals("SSL", mailAuthType)) {
				props.put("mail.smtp.socketFactory.port", mailSmtpPort); // SSL Port
				props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); // SSL
				// Factory
				// Class
				props.put("mail.smtp.ssl.checkserveridentity", true);
			}
			else {
				props.put("mail.smtp.starttls.enable", "true");
			}
			props.setProperty("mail.smtp.submitter", mailSmtpUser);
		}
		return mailSender;
	}

}
