/*
 * Copyright (c) 2024-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.forwardnode;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.group.GroupSequenceProvider;
import org.karnak.backend.data.validator.DestinationGroupSequenceProvider.DestinationDicomGroup;
import org.karnak.backend.data.validator.DestinationGroupSequenceProvider.DestinationStowGroup;
import org.karnak.backend.data.validator.DestinationModelGroupSequenceProvider;
import org.karnak.backend.enums.DestinationType;
import org.karnak.backend.enums.PseudonymType;

/**
 * API model exposed by the REST layer for a destination.
 */
@GroupSequenceProvider(value = DestinationModelGroupSequenceProvider.class)
@Getter
@Setter
public class DestinationModel implements Serializable {

	@Serial
	private static final long serialVersionUID = -1840723683898837460L;

	private UUID uuid;

	private String description;

	@NotNull(message = "Type is mandatory")
	private DestinationType destinationType;

	private boolean activate;

	private String condition;

	private boolean activateTagMorphing;

	private boolean desidentification;

	// Project (uuid) used for de-identification, when desidentification is enabled.
	private UUID deIdentificationProjectUuid;

	// Project (uuid) used for tag morphing, when activateTagMorphing is enabled.
	private UUID tagMorphingProjectUuid;

	private String issuerByDefault;

	private boolean skipIssuerOfPatientId;

	private PseudonymType pseudonymType;

	private String tag;

	private String delimiter;

	private Integer position;

	private String pseudonymUrl;

	private String responsePath;

	private String body;

	private String method;

	private String authConfig;

	private Boolean savePseudonym;

	private boolean filterBySOPClasses;

	private boolean activateNotification;

	private boolean buildConformanceReport;

	private boolean checkValueConformity;

	private boolean deepSequenceValidation;

	private boolean virtualDestination;

	private String conformanceReportNotify;

	private String notify;

	private String notifyObjectErrorPrefix;

	private String notifyObjectRejectionPrefix;

	private String notifyObjectPattern;

	private String notifyObjectValues;

	private Integer notifyInterval;

	// DICOM properties
	// the AETitle of the destination node.
	// mandatory[type=dicom]
	@NotBlank(groups = DestinationDicomGroup.class, message = "AETitle is mandatory")
	@Size(groups = DestinationDicomGroup.class, max = 16, message = "AETitle has more than 16 characters")
	private String aeTitle;

	// the host or IP of the destination node.
	// mandatory[type=dicom]
	@NotBlank(groups = DestinationDicomGroup.class, message = "Hostname is mandatory")
	private String hostname;

	// the port of the destination node.
	// mandatory[type=dicom]
	@NotNull(groups = DestinationDicomGroup.class, message = "Port is mandatory")
	@Min(groups = DestinationDicomGroup.class, value = 1, message = "Port should be between 1 and 65535")
	@Max(groups = DestinationDicomGroup.class, value = 65535, message = "Port should be between 1 and 65535")
	private Integer port;

	private Boolean useaetdest;

	// STOW properties
	// the destination STOW-RS URL.
	// mandatory[type=stow]
	@NotBlank(groups = DestinationStowGroup.class, message = "URL is mandatory")
	private String url;

	private String headers;

	private String transferSyntax;

	private boolean transcodeOnlyUncompressed;

	@Min(groups = DestinationDicomGroup.class, value = 1, message = "Concurrent connections must be at least 1")
	@Max(groups = DestinationDicomGroup.class, value = 50, message = "Concurrent connections must be 50 or less")
	private Integer concurrentConnections;

	private boolean http2;

	private boolean transferInProgress;

	private LocalDateTime lastTransfer;

	private LocalDateTime emailLastCheck;

}
