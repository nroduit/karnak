/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.weasis.core.util.annotations.Generated;

@Component
@Generated()
public class ApplicationContextProvider implements ApplicationContextAware {

	private static @Nullable ApplicationContext context;

	public static <T> T bean(Class<T> beanType) {
		return Objects.requireNonNull(context).getBean(beanType);
	}

	public static Object bean(String name) {
		return Objects.requireNonNull(context).getBean(name);
	}

	@Override
	public void setApplicationContext(@SuppressWarnings("NullableProblems") ApplicationContext ac) {
		context = ac;
	}

}