/*
 * Copyright (c) 2022-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.config;

import java.time.Duration;
import org.karnak.backend.model.patient.PatientModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.weasis.core.util.annotations.Generated;

@Configuration
@Profile("!portable")
@Generated
public class RedisConfiguration {

	@Value("${cache-api.ttl}")
	private int ttl;

	@Bean
	ChannelTopic topic() {
		return new ChannelTopic("patient:queue");
	}

	@Bean(name = "redisTemplate")
	public RedisTemplate<String, PatientModel> redisTemplate(RedisConnectionFactory connectionFactory) {
		JacksonJsonRedisSerializer<PatientModel> serializer = new JacksonJsonRedisSerializer<>(PatientModel.class);

		RedisTemplate<String, PatientModel> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(serializer);

		return template;
	}

	@Bean
	public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
		// ExternalIDCache
		return builder -> builder
			.withCacheConfiguration("externalId.cache",
					RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(7)))
			.withCacheConfiguration("endpoint.cache",
					RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(ttl)));
	}

}
