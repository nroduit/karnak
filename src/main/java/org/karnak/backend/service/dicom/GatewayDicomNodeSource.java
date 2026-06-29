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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.repo.DestinationRepo;
import org.karnak.backend.enums.DestinationType;
import org.karnak.backend.util.DicomNodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.weasis.core.util.StringUtil;
import org.weasis.dicom.param.DicomNode;
import org.weasis.dicom.param.DicomNodeSource;

/**
 * {@link DicomNodeSource} that exposes every DICOM destination configured in the gateway
 * as a read-only group, so the configured forwarding targets can be reused by the echo,
 * monitor and node picker tools without being re-declared by hand.
 *
 * <p>
 * DICOMweb (STOW-RS) destinations have no AE Title / host / port and are skipped;
 * destinations sharing the same AE Title, host and port are reported once. The nodes
 * carry the destination description (falling back to the AE Title) and no id, marking
 * them as not individually editable.
 */
@Component
public class GatewayDicomNodeSource implements DicomNodeSource {

	private final DestinationRepo destinationRepo;

	@Autowired
	public GatewayDicomNodeSource(DestinationRepo destinationRepo) {
		this.destinationRepo = destinationRepo;
	}

	@Override
	public String getGroupName() {
		return DicomNodeUtil.GATEWAY_DESTINATIONS_GROUP_NAME;
	}

	@Override
	public List<DicomNode> getNodes() {
		var nodes = new ArrayList<DicomNode>();
		Set<String> seen = new HashSet<>();
		for (DestinationEntity destination : destinationRepo.findAll()) {
			if (destination.getDestinationType() != DestinationType.dicom) {
				continue;
			}
			String aeTitle = destination.getAeTitle();
			String hostname = destination.getHostname();
			Integer port = destination.getPort();
			if (!StringUtil.hasText(aeTitle) || !StringUtil.hasText(hostname) || port == null || port <= 0) {
				continue;
			}
			if (!seen.add(aeTitle + '\\' + hostname + '\\' + port)) {
				continue;
			}
			String description = StringUtil.hasText(destination.getDescription()) ? destination.getDescription()
					: aeTitle;
			nodes.add(new DicomNode(aeTitle, hostname, port, description));
		}
		return nodes;
	}

}
