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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import org.karnak.backend.cache.Patient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Profile("!portable")
public class RedisConfiguration {

	@Value("${cache-api.ttl}")
	private int ttl;

	@Bean
	ChannelTopic topic() {
		return new ChannelTopic("patient:queue");
	}

	@Bean(name = "redisTemplate")
	public RedisTemplate<String, Patient> redisTemplate(RedisConnectionFactory connectionFactory) {

		ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module())
			.registerModule(new JavaTimeModule());

		Jackson2JsonRedisSerializer<Patient> serializer = new Jackson2JsonRedisSerializer<>(objectMapper,
				Patient.class);

		RedisTemplate<String, Patient> template = new RedisTemplate<>();
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
