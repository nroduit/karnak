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

import org.karnak.backend.model.project.ProjectDeleteBlockersModel;

/**
 * Raised when {@link ProjectService#remove} is called while destinations still reference
 * the project.
 */
public class ProjectDeleteBlockedException extends RuntimeException {

	private final ProjectDeleteBlockersModel blockers;

	public ProjectDeleteBlockedException(ProjectDeleteBlockersModel blockers) {
		super("Project delete blocked by referencing destinations");
		this.blockers = blockers;
	}

	public ProjectDeleteBlockersModel getBlockers() {
		return blockers;
	}

}
