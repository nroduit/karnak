/*
 * Copyright (c) 2021 Karnak Team and other contributors.
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
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
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.repo.DestinationRepo;
import org.karnak.backend.dicom.ForwardDestination;
import org.karnak.backend.dicom.ForwardDicomNode;
import org.karnak.backend.dicom.Params;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.weasis.dicom.param.DicomNode;

@Service
@Slf4j
public class CStoreSCPService extends BasicCStoreSCP {

	// Service
	private final DestinationRepo destinationRepo;

	private final ForwardService forwardService;

	private Map<ForwardDicomNode, List<ForwardDestination>> destinations;

	private volatile int priority;

	private volatile int status;

	// Scheduled service for updating status transfer in progress
	private ScheduledFuture isDelayOver;

	private final ScheduledExecutorService executorService;

	@Autowired
	public CStoreSCPService(final DestinationRepo destinationRepo, final ForwardService forwardService) {
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
		Optional<ForwardDicomNode> sourceNode = destinations.keySet()
			.stream()
			.filter(n -> n.getForwardAETitle().equals(as.getCalledAET()))
			.findFirst();
		if (sourceNode.isEmpty()) {
			throw new IllegalStateException("Cannot find the forward AeTitle " + as.getCalledAET());
		}
		ForwardDicomNode fwdNode = sourceNode.get();
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
	 * Update transfer status: if there is a transfer in progress set status to true and
	 * schedule a thread which will set back status to false in a certain delay. If a
	 * transfer is still in progress after the end of the delay, set status to true and an
	 * other delay is scheduled.
	 * @param destinations Destinations to update
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
	 * Update the transfer status of a destination
	 * @param destination Destination to retrieve
	 * @param status Status to update
	 */
	private void updateTransferStatus(ForwardDestination destination, boolean status) {
		// Retrieve the destination entity
		Optional<DestinationEntity> destinationEntityOptional = destinationRepo.findById(destination.getId());

		if (destinationEntityOptional.isPresent()) {
			// Update the destination transfer status if destination has been found and
			// destination
			// is active
			DestinationEntity destinationEntity = destinationEntityOptional.get();
			if (destinationEntity.isActivate()) {
				destinationEntity.setTransferInProgress(status);
				destinationEntity.setLastTransfer(LocalDateTime.now(ZoneId.of("CET")));
				destinationRepo.save(destinationEntity);
			}
		}
	}

	public Map<ForwardDicomNode, List<ForwardDestination>> getDestinations() {
		return destinations;
	}

	public void setDestinations(Map<ForwardDicomNode, List<ForwardDestination>> destinations) {
		this.destinations = destinations;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

}
