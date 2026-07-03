/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;

/**
 * One persisted DICOM Echo check (DICOM C-ECHO + network reachability) as run from the
 * DICOM Echo view. Rows are kept newest-first and pruned to a configurable limit. The
 * check outcome is flattened into plain columns so the history can be rendered without
 * reconstructing the (non-persistable) DICOM domain objects.
 */
@Entity(name = "DicomNodeCheckHistory")
@Table(name = "dicom_node_check_history")
@Getter
@Setter
@Builder
@NoArgsConstructor // required by JPA
@AllArgsConstructor // required by @Builder
@NullUnmarked
public class DicomNodeCheckHistoryEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Column(name = "checked_at", nullable = false)
	private Instant checkedAt;

	@Column(name = "calling_ae_title", length = 16)
	private String callingAeTitle;

	@Column(name = "called_description")
	private String calledDescription;

	@Column(name = "called_ae_title", nullable = false, length = 16)
	private String calledAeTitle;

	@Column(name = "called_hostname", nullable = false)
	private String calledHostname;

	@Column(name = "called_port", nullable = false)
	private int calledPort;

	@Column(name = "echo_successful", nullable = false)
	private boolean echoSuccessful;

	@Column(name = "echo_status_hex", length = 8)
	private String echoStatusHex;

	@Column(name = "echo_status_message", length = 1024)
	private String echoStatusMessage;

	@Column(name = "echo_error_message", length = 1024)
	private String echoErrorMessage;

	@Column(name = "echo_rejection_reason", length = 1024)
	private String echoRejectionReason;

	@Column(name = "echo_verification_unsupported_message", length = 1024)
	private String echoVerificationUnsupportedMessage;

	@Column(name = "remote_impl_version_name", length = 64)
	private String remoteImplVersionName;

	@Column(name = "remote_impl_class_uid", length = 64)
	private String remoteImplClassUid;

	@Column(name = "connection_ms")
	private Long connectionMs;

	@Column(name = "execution_ms")
	private Long executionMs;

	@Column(name = "network_reachable", nullable = false)
	private boolean networkReachable;

	@Column(name = "port_open", nullable = false)
	private boolean portOpen;

	@Column(name = "network_hostname_message", length = 1024)
	private String networkHostnameMessage;

	@Column(name = "network_port_message", length = 1024)
	private String networkPortMessage;

	@Column(name = "network_quality_message", length = 1024)
	private String networkQualityMessage;

}