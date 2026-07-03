/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.dicom;

import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.karnak.backend.data.entity.DicomNodeCheckHistoryEntity;
import org.karnak.backend.data.repo.DicomNodeCheckHistoryRepo;
import org.karnak.backend.model.dicom.ConfigNode;
import org.karnak.backend.model.dicom.result.DicomEchoResult;
import org.karnak.backend.model.dicom.result.DicomNodeCheckHistory;
import org.karnak.backend.model.dicom.result.DicomNodeCheckResult;
import org.karnak.backend.model.dicom.result.NetworkCheckResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists and serves the history of DICOM Echo checks. Recording is failure-safe (a DB
 * problem never breaks the check flow) and bounded: after each insert the table is pruned
 * to the configured {@code application.echo.history.limit} (default 200).
 */
@Service
@Slf4j
public class DicomNodeCheckHistoryService {

	private final DicomNodeCheckHistoryRepo repository;

	private final int historyLimit;

	public DicomNodeCheckHistoryService(DicomNodeCheckHistoryRepo repository,
			@Value("${application.echo.history.limit:200}") int historyLimit) {
		this.repository = repository;
		this.historyLimit = Math.max(1, historyLimit);
	}

	/**
	 * Stores one check result, then prunes the oldest rows beyond the configured limit.
	 * Swallows any persistence error so a failed save never disrupts the check itself.
	 */
	@Transactional
	public void record(DicomNodeCheckResult result) {
		try {
			this.repository.save(toEntity(result));
			prune();
		}
		catch (Exception ex) {
			log.error("Could not persist DICOM Echo check history: {}", ex.getMessage(), ex);
		}
	}

	/** Most recent checks first, capped at the configured history limit. */
	@Transactional(readOnly = true)
	public List<DicomNodeCheckHistory> getRecentChecks() {
		return this.repository.findByOrderByCheckedAtDesc(PageRequest.of(0, this.historyLimit))
			.stream()
			.map(DicomNodeCheckHistoryService::toModel)
			.toList();
	}

	private void prune() {
		long count = this.repository.count();
		if (count <= this.historyLimit) {
			return;
		}

		int surplus = (int) Math.min(count - this.historyLimit, Integer.MAX_VALUE);
		List<DicomNodeCheckHistoryEntity> oldest = this.repository
			.findByOrderByCheckedAtAsc(PageRequest.of(0, surplus));

		this.repository.deleteAll(oldest);
	}

	private static DicomNodeCheckHistoryEntity toEntity(DicomNodeCheckResult result) {
		ConfigNode calledNode = result.getCalledNode();
		DicomEchoResult echo = result.getDicomEchoResult();
		NetworkCheckResult network = result.getNetworkCheckResult();

		Instant checkedAt = (result.getCheckedAt() != null) ? result.getCheckedAt() : Instant.now();

		return DicomNodeCheckHistoryEntity.builder()
			.checkedAt(checkedAt)
			.callingAeTitle(result.getCallingAET())
			.calledDescription(calledNode.getName())
			.calledAeTitle(calledNode.getAet())
			.calledHostname(calledNode.getHostname())
			.calledPort(calledNode.getPort())
			.echoSuccessful(echo.isSuccessful())
			.echoStatusHex(echo.getDicomStatusInHex())
			.echoStatusMessage(echo.getDicomStatusMessage())
			.echoErrorMessage(echo.isUnexpectedError() ? echo.getUnexpectedErrorMessage() : null)
			.echoRejectionReason(echo.getRejectionReason())
			.echoVerificationUnsupportedMessage(echo.getVerificationUnsupportedMessage())
			.remoteImplVersionName(echo.getRemoteImplementationVersionName())
			.remoteImplClassUid(echo.getRemoteImplementationClassUid())
			.connectionMs(echo.getConnectionDurationInMs())
			.executionMs(echo.getExecutionDurationInMs())
			.networkReachable(network.isSuccessful())
			.portOpen(network.isPortOpen())
			.networkHostnameMessage(network.getCheckHostnameMessage())
			.networkPortMessage(network.getCheckPortMessage())
			.networkQualityMessage(network.getCheckQualityMessage())
			.build();
	}

	private static DicomNodeCheckHistory toModel(DicomNodeCheckHistoryEntity entity) {
		return DicomNodeCheckHistory.builder()
			.checkedAt(entity.getCheckedAt())
			.callingAeTitle(entity.getCallingAeTitle())
			.calledDescription(entity.getCalledDescription())
			.calledAeTitle(entity.getCalledAeTitle())
			.calledHostname(entity.getCalledHostname())
			.calledPort(entity.getCalledPort())
			.echoSuccessful(entity.isEchoSuccessful())
			.echoStatusHex(entity.getEchoStatusHex())
			.echoStatusMessage(entity.getEchoStatusMessage())
			.echoErrorMessage(entity.getEchoErrorMessage())
			.echoRejectionReason(entity.getEchoRejectionReason())
			.echoVerificationUnsupportedMessage(entity.getEchoVerificationUnsupportedMessage())
			.remoteImplVersionName(entity.getRemoteImplVersionName())
			.remoteImplClassUid(entity.getRemoteImplClassUid())
			.connectionMs(entity.getConnectionMs())
			.executionMs(entity.getExecutionMs())
			.networkReachable(entity.isNetworkReachable())
			.portOpen(entity.isPortOpen())
			.networkHostnameMessage(entity.getNetworkHostnameMessage())
			.networkPortMessage(entity.getNetworkPortMessage())
			.networkQualityMessage(entity.getNetworkQualityMessage())
			.build();
	}

}