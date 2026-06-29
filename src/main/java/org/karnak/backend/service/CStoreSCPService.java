/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.PDVInputStream;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicCStoreSCP;
import org.dcm4che3.net.service.DicomServiceException;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.repo.DestinationRepo;
import org.karnak.backend.dicom.ForwardDestination;
import org.karnak.backend.dicom.ForwardDicomNode;
import org.karnak.backend.dicom.Params;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.weasis.core.util.annotations.Generated;
import org.weasis.dicom.param.DicomNode;

@Service
@Slf4j
@Generated()
@NullUnmarked
public class CStoreSCPService extends BasicCStoreSCP {

	// Service
	private final DestinationRepo destinationRepo;

	private final ForwardService forwardService;

	@Setter
	@Getter
	private Map<ForwardDicomNode, List<ForwardDestination>> destinations;

	@Setter
	@Getter
	private volatile int priority;

	@Setter
	@Getter
	private volatile int status;

	// Scheduled service for updating status transfer in progress
	private ScheduledFuture<?> isDelayOver;

	private final ScheduledExecutorService executorService;

	@Autowired
	public CStoreSCPService(DestinationRepo destinationRepo, ForwardService forwardService) {
		super("*");
		this.destinationRepo = destinationRepo;
		this.forwardService = forwardService;
		this.destinations = null;
		this.executorService = Executors.newSingleThreadScheduledExecutor();
		this.isDelayOver = null;
		this.status = 0;
		this.priority = 0;
	}

	public void init(Map<ForwardDicomNode, List<ForwardDestination>> destinations) {
		this.destinations = destinations;
	}

	@Override
	protected void store(Association as, PresentationContext pc, Attributes rq, PDVInputStream data, Attributes rsp)
			throws IOException {
		ForwardDicomNode fwdNode = destinations.keySet()
			.stream()
			.filter(n -> n.getForwardAETitle().equals(as.getCalledAET()))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("Cannot find the forward AeTitle " + as.getCalledAET()));
		List<ForwardDestination> destList = destinations.get(fwdNode);
		if (destList == null || destList.isEmpty()) {
			throw new IllegalStateException("No DICOM destinations for " + fwdNode);
		}

		DicomNode callingNode = DicomNode.buildRemoteDicomNode(as);
		Set<DicomNode> srcNodes = fwdNode.getAcceptedSourceNodes();
		boolean valid = srcNodes.isEmpty() || srcNodes.stream()
			.anyMatch(n -> n.getAet().equals(callingNode.getAet())
					&& (!n.isValidateHostname() || n.equalsHostname(callingNode.getHostname())));
		if (!valid) {
			rsp.setInt(Tag.Status, VR.US, Status.NotAuthorized);
			log.error("Refused: not authorized (124H). Source node: {}. SopUID: {}", callingNode,
					rq.getString(Tag.AffectedSOPInstanceUID));
			return;
		}

		rsp.setInt(Tag.Status, VR.US, status);

		try {
			Params p = new Params(rq.getString(Tag.AffectedSOPInstanceUID), rq.getString(Tag.AffectedSOPClassUID),
					pc.getTransferSyntax(), priority, data, as);

			// Update transfer status of destinations
			updateTransferStatus(destList);

			forwardService.storeMultipleDestination(fwdNode, destList, p);

		}
		catch (Exception e) {
			throw new DicomServiceException(Status.ProcessingFailure, e);
		}
	}

	/**
	 * Flags the destinations as transferring, then schedules clearing the flag after a
	 * delay.
	 */
	private void updateTransferStatus(List<ForwardDestination> destinations) {
		// if delay is over or first iteration
		if (isDelayOver == null || isDelayOver.isDone()) {
			// Set flag transfer in progress
			destinations.forEach(d -> updateTransferStatus(d, true));
			// In a certain delay set back transfer in progress to false
			isDelayOver = executorService.schedule(() -> destinations.forEach(d -> updateTransferStatus(d, false)), 5,
					TimeUnit.SECONDS);
		}
	}

	/**
	 * Persists the transfer-in-progress flag for an active destination when it changes.
	 */
	private void updateTransferStatus(ForwardDestination destination, boolean status) {
		destinationRepo.findById(destination.getId()).ifPresent(destinationEntity -> {
			if (destinationEntity.isActivate() && destinationEntity.isTransferInProgress() != status) {
				destinationEntity.setTransferInProgress(status);
				destinationEntity.setLastTransfer(LocalDateTime.now(ZoneId.of("CET")));
				destinationRepo.save(destinationEntity);
			}
		});
	}

}
