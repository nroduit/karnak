/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.standard;

public class Module {

	public static final String MANDATORY = "M";

	private final String id;

	private final String usage;

	private final String informationEntity;

	public Module(String id, String usage, String informationEntity) {
		this.id = id;
		this.usage = usage;
		this.informationEntity = informationEntity;
	}

	public String getId() {
		return id;
	}

	public static boolean moduleIsMandatory(Module module) {
		return module.getUsage().equals(MANDATORY);
	}

	public String getUsage() {
		return usage;
	}

	public String getInformationEntity() {
		return informationEntity;
	}

}
