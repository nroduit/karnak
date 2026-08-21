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

import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.enums.PseudonymType;

/**
 * Maps between {@link DestinationEntity} and {@link DestinationModel}. Nested
 * relations (forward node, projects, SOP class filters) and the
 * resource id are not exposed by this mapper: they are managed through their own
 * endpoints/services or conveyed by the URL path.
 */
public final class DestinationMapper {

	private DestinationMapper() {
	}

	public static DestinationModel toModel(DestinationEntity entity) {
		if (entity == null) {
			return null;
		}
		DestinationModel model = new DestinationModel();
		model.setUuid(entity.getUuid());
		model.setDescription(entity.getDescription());
		model.setDestinationType(entity.getDestinationType());
		model.setActivate(entity.isActivate());
		model.setCondition(entity.getCondition());
		model.setActivateTagMorphing(entity.isActivateTagMorphing());
		model.setTagMorphingProjectUuid(
				entity.getTagMorphingProjectEntity() != null
						? entity.getTagMorphingProjectEntity().getUuid() : null);
		model.setDesidentification(entity.isDesidentification());
		model.setDeIdentificationProjectUuid(
				entity.getDeIdentificationProjectEntity() != null
						? entity.getDeIdentificationProjectEntity().getUuid() : null);
		model.setIssuerByDefault(entity.getIssuerByDefault());
		model.setSkipIssuerOfPatientId(entity.isSkipIssuerOfPatientId());
		model.setPseudonymType(entity.getPseudonymType());
		model.setTag(entity.getTag());
		model.setDelimiter(entity.getDelimiter());
		model.setPosition(entity.getPosition());
		model.setPseudonymUrl(entity.getPseudonymUrl());
		model.setResponsePath(entity.getResponsePath());
		model.setBody(entity.getBody());
		model.setMethod(entity.getMethod());
		model.setAuthConfig(entity.getAuthConfig());
		model.setSavePseudonym(entity.getSavePseudonym());
		model.setFilterBySOPClasses(entity.isFilterBySOPClasses());
		model.setActivateNotification(entity.isActivateNotification());
		model.setBuildConformanceReport(entity.isBuildConformanceReport());
		model.setCheckValueConformity(entity.isCheckValueConformity());
		model.setDeepSequenceValidation(entity.isDeepSequenceValidation());
		model.setVirtualDestination(entity.isVirtualDestination());
		model.setConformanceReportNotify(entity.getConformanceReportNotify());
		model.setNotify(entity.getNotify());
		model.setNotifyObjectErrorPrefix(entity.getNotifyObjectErrorPrefix());
		model.setNotifyObjectRejectionPrefix(entity.getNotifyObjectRejectionPrefix());
		model.setNotifyObjectPattern(entity.getNotifyObjectPattern());
		model.setNotifyObjectValues(entity.getNotifyObjectValues());
		model.setNotifyInterval(entity.getNotifyInterval());
		model.setAeTitle(entity.getAeTitle());
		model.setHostname(entity.getHostname());
		model.setPort(entity.getPort());
		model.setUseaetdest(
				entity.getUseaetdest() != null ? entity.getUseaetdest() : Boolean.FALSE);
		model.setUrl(entity.getUrl());
		model.setHeaders(entity.getHeaders());
		model.setTransferSyntax(entity.getTransferSyntax());
		model.setTranscodeOnlyUncompressed(entity.isTranscodeOnlyUncompressed());
		model.setConcurrentConnections(entity.getConcurrentConnections());
		model.setHttp2(entity.isHttp2());
		model.setTransferInProgress(entity.isTransferInProgress());
		model.setLastTransfer(entity.getLastTransfer());
		model.setEmailLastCheck(entity.getEmailLastCheck());
		return model;
	}

	public static DestinationEntity toEntity(DestinationModel model) {
		if (model == null) {
			return null;
		}
		DestinationEntity entity = new DestinationEntity();
		entity.setDescription(defaultString(model.getDescription()));
		entity.setDestinationType(model.getDestinationType());
		entity.setActivate(model.isActivate());
		entity.setCondition(defaultString(model.getCondition()));
		entity.setActivateTagMorphing(model.isActivateTagMorphing());
		entity.setDesidentification(model.isDesidentification());
		entity.setIssuerByDefault(defaultString(model.getIssuerByDefault()));
		entity.setSkipIssuerOfPatientId(model.isSkipIssuerOfPatientId());
		entity.setPseudonymType(
				model.getPseudonymType() != null ? model.getPseudonymType() : PseudonymType.CACHE_EXTID);
		entity.setTag(model.getTag());
		entity.setDelimiter(model.getDelimiter());
		entity.setPosition(model.getPosition());
		entity.setPseudonymUrl(model.getPseudonymUrl());
		entity.setResponsePath(model.getResponsePath());
		entity.setBody(model.getBody());
		entity.setMethod(model.getMethod());
		entity.setAuthConfig(model.getAuthConfig());
		entity.setSavePseudonym(
				model.getSavePseudonym() != null ? model.getSavePseudonym() : Boolean.FALSE);
		entity.setFilterBySOPClasses(model.isFilterBySOPClasses());
		entity.setActivateNotification(model.isActivateNotification());
		entity.setBuildConformanceReport(model.isBuildConformanceReport());
		entity.setCheckValueConformity(model.isCheckValueConformity());
		entity.setDeepSequenceValidation(model.isDeepSequenceValidation());
		entity.setVirtualDestination(model.isVirtualDestination());
		entity.setConformanceReportNotify(model.getConformanceReportNotify());
		entity.setNotify(defaultString(model.getNotify()));
		entity.setNotifyObjectErrorPrefix(defaultString(model.getNotifyObjectErrorPrefix()));
		entity.setNotifyObjectRejectionPrefix(defaultString(model.getNotifyObjectRejectionPrefix()));
		entity.setNotifyObjectPattern(defaultString(model.getNotifyObjectPattern()));
		entity.setNotifyObjectValues(defaultString(model.getNotifyObjectValues()));
		entity.setNotifyInterval(model.getNotifyInterval());
		entity.setAeTitle(defaultString(model.getAeTitle()));
		entity.setHostname(defaultString(model.getHostname()));
		entity.setPort(model.getPort());
		entity.setUseaetdest(
				model.getUseaetdest() != null ? model.getUseaetdest() : Boolean.FALSE);
		entity.setUrl(defaultString(model.getUrl()));
		entity.setHeaders(defaultString(model.getHeaders()));
		entity.setTransferSyntax(model.getTransferSyntax());
		entity.setTranscodeOnlyUncompressed(model.isTranscodeOnlyUncompressed());
		entity.setConcurrentConnections(
				model.getConcurrentConnections() != null ? model.getConcurrentConnections() : 1);
		entity.setHttp2(model.isHttp2());
		entity.setTransferInProgress(model.isTransferInProgress());
		entity.setLastTransfer(model.getLastTransfer());
		entity.setEmailLastCheck(model.getEmailLastCheck());
		return entity;
	}

	/**
	 * Several TextField-bound String columns are NOT NULL (or the UI binder rejects
	 * null for that widget); fall back to an empty string, mirroring
	 * {@link DestinationEntity}'s own constructor defaults, instead of propagating a
	 * null value coming from an incomplete payload.
	 */
	private static String defaultString(String value) {
		return value != null ? value : "";
	}

}
