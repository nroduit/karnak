/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.gateway;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.net.URL;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.repo.DestinationRepo;
import org.karnak.backend.dicom.GatewayParams;
import org.karnak.backend.model.event.NodeEvent;
import org.karnak.backend.service.DicomGatewayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.weasis.core.util.FileUtil;
import org.weasis.core.util.StringUtil;

@Service
@Slf4j
public class GatewayService implements ApplicationListener<ContextRefreshedEvent> {

	private final GatewaySetUpService gatewaySetUpService;

	private final DicomGatewayService gateway;

	// Repository
	private final DestinationRepo destinationRepo;

	@Autowired
	public GatewayService(final GatewaySetUpService gatewaySetUpService, final DicomGatewayService dicomGatewayService,
			final DestinationRepo destinationRepo) {
		this.gatewaySetUpService = gatewaySetUpService;
		this.gateway = dicomGatewayService;
		this.destinationRepo = destinationRepo;
	}

	public void initGateway() {
		try {
			String[] acceptedCallingAETitles = GatewayParams
				.getAcceptedCallingAETitles(gatewaySetUpService.getDestinations());
			GatewayParams gparams = new GatewayParams(gatewaySetUpService.getAdvancedParams(), false, null,
					acceptedCallingAETitles);
			gateway.init(gatewaySetUpService.getDestinations());
			gateway.start(gatewaySetUpService.getCallingDicomNode(), gparams);
			log.info("Karnak DICOM gateway servlet is running: {}", gatewaySetUpService);
		}
		catch (Exception e) {
			log.error("Cannot start DICOM gateway", e);
		}
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		log.info("Application Event: {}", event);
	}

	@EventListener
	public void reloadOutboundNodes(NodeEvent event) {
		gatewaySetUpService.update(event);

		// Refresh the version of the gateway set up
		gatewaySetUpService.refreshVersionGatewaySetUp();
	}

	@PreDestroy
	public void destroy() {
		if (gateway != null) {
			gateway.stop();
		}

		// Reset status transfer in progress on destination
		List<DestinationEntity> destinationEntities = destinationRepo.findAll();
		destinationEntities.forEach(d -> d.setTransferInProgress(false));
		destinationRepo.saveAll(destinationEntities);

		log.info("{}", "Gateway has been stopped");
		String dir = System.getProperty("dicom.native.codec");
		if (StringUtil.hasText(dir)) {
			FileUtil.delete(new File(dir));
		}
	}

	@PostConstruct
	public void init() {
		log.info("{}", "Start the gateway manager running as a background process");
		// try {
		// URL resource = this.getClass().getResource("/lib");
		// NativeLibraryManager.initNativeLibs(resource);
		// }
		// catch (Exception e1) {
		// throw new IllegalStateException("Cannot register DICOM native librairies", e1);
		// }
		initGateway();
	}

}
