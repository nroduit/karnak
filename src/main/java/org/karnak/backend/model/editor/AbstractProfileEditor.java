/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.editor;

import org.dcm4che3.data.Attributes;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.service.profilepipe.DeidentifyImageService;
import org.karnak.backend.service.profilepipe.Profile;
import org.weasis.dicom.param.AttributeEditor;
import org.weasis.dicom.param.AttributeEditorContext;
import org.weasis.dicom.param.AttributeEditorContext.Abort;

/**
 * Base for editors applying a {@link Profile} to a destination's project, unless the
 * transfer was aborted.
 */
abstract class AbstractProfileEditor implements AttributeEditor {

	protected final Profile profile;

	protected final DestinationEntity destinationEntity;

	protected final ProfileEntity profileEntity;

	protected final ProjectEntity projectEntity;

	protected AbstractProfileEditor(DestinationEntity destinationEntity, ProjectEntity projectEntity,
			DeidentifyImageService deidentifyImageService) {
		this.destinationEntity = destinationEntity;
		this.projectEntity = projectEntity;
		this.profileEntity = projectEntity.getProfileEntity();
		this.profile = new Profile(profileEntity, deidentifyImageService);
	}

	@Override
	public void apply(Attributes dcm, AttributeEditorContext context) {
		if (context.getAbort() != Abort.FILE_EXCEPTION) {
			applyProfile(dcm, context);
		}
	}

	protected abstract void applyProfile(Attributes dcm, AttributeEditorContext context);

}
