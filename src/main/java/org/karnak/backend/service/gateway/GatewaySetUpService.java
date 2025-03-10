/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.gateway;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.DicomSourceNodeEntity;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.data.entity.KheopsAlbumsEntity;
import org.karnak.backend.data.entity.VersionEntity;
import org.karnak.backend.data.repo.DestinationRepo;
import org.karnak.backend.data.repo.ForwardNodeRepo;
import org.karnak.backend.data.repo.VersionRepo;
import org.karnak.backend.dicom.DicomForwardDestination;
import org.karnak.backend.dicom.ForwardDestination;
import org.karnak.backend.dicom.ForwardDicomNode;
import org.karnak.backend.dicom.WebForwardDestination;
import org.karnak.backend.enums.DestinationType;
import org.karnak.backend.enums.NodeEventType;
import org.karnak.backend.model.editor.ConditionEditor;
import org.karnak.backend.model.editor.DeIdentifyEditor;
import org.karnak.backend.model.editor.FilterEditor;
import org.karnak.backend.model.editor.StreamRegistryEditor;
import org.karnak.backend.model.editor.TagMorphingEditor;
import org.karnak.backend.model.event.NodeEvent;
import org.karnak.backend.service.kheops.SwitchingAlbum;
import org.karnak.backend.util.SystemPropertyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.weasis.core.util.LangUtil;
import org.weasis.core.util.StringUtil;
import org.weasis.dicom.param.AdvancedParams;
import org.weasis.dicom.param.AttributeEditor;
import org.weasis.dicom.param.AttributeEditorContext;
import org.weasis.dicom.param.ConnectOptions;
import org.weasis.dicom.param.DicomNode;
import org.weasis.dicom.param.DicomProgress;
import org.weasis.dicom.param.TlsOptions;

@Service
@Slf4j
public class GatewaySetUpService {

	// Repositories
	private final ForwardNodeRepo forwardNodeRepo;

	private final VersionRepo versionRepo;

	private final DestinationRepo destinationRepo;

	private final Map<ForwardDicomNode, List<ForwardDestination>> destMap;

	private final Path storePath;

	private final String listenerAET;

	private final int listenerPort;

	private final Boolean listenerTLS;

	private final int intervalCheck;

	private final String archiveUrl;

	private final String clientKey;

	private final String clientKeyPwd;

	private final String truststorePwd;

	private final String truststore;

	// Version gateway setup for this instance
	private long gatewaySetUpVersion;

	@Autowired
	public GatewaySetUpService(final ForwardNodeRepo forwardNodeRepo, final VersionRepo versionRepo,
			final DestinationRepo destinationRepo) throws Exception {
		this.forwardNodeRepo = forwardNodeRepo;
		this.versionRepo = versionRepo;
		this.destinationRepo = destinationRepo;
		this.destMap = new HashMap<>();

		String path = SystemPropertyUtil.retrieveSystemProperty("GATEWAY_ARCHIVE_PATH", null); // Only
		// Archive
		// and
		// Pull
		// mode
		storePath = StringUtil.hasText(path) ? Path.of(path) : null;
		intervalCheck = StringUtil
			.getInt(SystemPropertyUtil.retrieveSystemProperty("GATEWAY_PULL_CHECK_INTERNAL", "5")); // Only
		// Pull
		// mode
		archiveUrl = SystemPropertyUtil.retrieveSystemProperty("GATEWAY_ARCHIVE_URL", ""); // Only
		// Archive
		// mode

		listenerAET = SystemPropertyUtil.retrieveSystemProperty("DICOM_LISTENER_AET", "KARNAK-GATEWAY");
		listenerPort = 11119;
		listenerTLS = LangUtil
			.getEmptytoFalse(SystemPropertyUtil.retrieveSystemProperty("DICOM_LISTENER_TLS", "false"));

		clientKey = SystemPropertyUtil.retrieveSystemProperty("TLS_KEYSTORE_PATH", null);
		clientKeyPwd = SystemPropertyUtil.retrieveSystemProperty("TLS_KEYSTORE_SECRET", null);
		truststorePwd = SystemPropertyUtil.retrieveSystemProperty("TLS_TRUSTSTORE_PATH", null);
		truststore = SystemPropertyUtil.retrieveSystemProperty("TLS_TRUSTSTORE_SECRET", null);

		// Init the current version of the gateway setup for this instance
		this.gatewaySetUpVersion = 0L;

		reloadGatewayPersistence();
	}

	private static AdvancedParams getDefaultAdvancedParameters() {
		AdvancedParams params = new AdvancedParams();
		ConnectOptions connectOptions = new ConnectOptions();
		connectOptions.setConnectTimeout(5000);
		connectOptions.setAcceptTimeout(7000);
		// Concurrent DICOM operations
		connectOptions.setMaxOpsInvoked(50);
		connectOptions.setMaxOpsPerformed(50);
		params.setConnectOptions(connectOptions);
		return params;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("Hostname=");
		buf.append(DicomNode.convertToIP(null));
		buf.append(" AETitle=");
		buf.append(listenerAET);
		buf.append(" Port=");
		buf.append(listenerPort);
		return buf.toString();
	}

	public String getArchiveUrl() {
		return archiveUrl;
	}

	public Path getStorePath() {
		return storePath;
	}

	public String getListenerAET() {
		return listenerAET;
	}

	public int getListenerPort() {
		return listenerPort;
	}

	public Boolean getListenerTLS() {
		return listenerTLS;
	}

	public int getIntervalCheck() {
		return intervalCheck;
	}

	public String getClientKey() {
		return clientKey;
	}

	public String getClientKeyPwd() {
		return clientKeyPwd;
	}

	public String getTruststorePwd() {
		return truststorePwd;
	}

	public String getTruststore() {
		return truststore;
	}

	public AdvancedParams getAdvancedParams() {
		AdvancedParams options = getDefaultAdvancedParameters();
		ConnectOptions connectOptions = options.getConnectOptions();
		if (getListenerTLS()) {
			TlsOptions tls = new TlsOptions(false, getClientKey(), "JKS", getClientKeyPwd(), getClientKeyPwd(),
					getTruststore(), "JKS", getTruststorePwd());
			options.setTlsOptions(tls);
		}
		options.setConnectOptions(connectOptions);
		return options;
	}

	public DicomNode getCallingDicomNode() {
		return new DicomNode(getListenerAET(), null, getListenerPort());
	}

	public Optional<ForwardDicomNode> getDestinationNode(String fwdAET) {
		return destMap.keySet().stream().filter(n -> n.getForwardAETitle().equals(fwdAET)).findFirst();
	}

	public List<ForwardDestination> getDestination(String fwdAET) {
		Optional<ForwardDicomNode> node = getDestinationNode(fwdAET);
		node.ifPresent(this::getDestinations);
		return Collections.emptyList();
	}

	public List<ForwardDestination> getDestinations(ForwardDicomNode fwdSrcNode) {
		List<ForwardDestination> destList = null;
		if (fwdSrcNode != null) {
			destList = destMap.get(fwdSrcNode);
		}
		if (destList == null) {
			return Collections.emptyList();
		}
		return destList;
	}

	public Map<ForwardDicomNode, List<ForwardDestination>> getDestinations() {
		return destMap;
	}

	public Set<ForwardDicomNode> getKeys() {
		return destMap.keySet();
	}

	private void addDestinationNode(List<ForwardDestination> dstList, ForwardDicomNode fwdSrcNode,
			DestinationEntity dstNode) {
		try {
			final List<KheopsAlbumsEntity> kheopsAlbumEntities = dstNode.getKheopsAlbumEntities();

			// Apply editors
			List<AttributeEditor> editors = new ArrayList<>();
			applyConditionEditor(dstNode, editors);
			applyFilterEditor(dstNode, editors);
			SwitchingAlbum switchingAlbum = applySwitchingAlbumEditor(dstNode, editors, kheopsAlbumEntities);
			applyStreamRegistryEditor(editors);
			applyDeIdentifyEditor(dstNode, editors);
			applyTagMorphingEditor(dstNode, editors);

			DicomProgress progress = new DicomProgress();

			if (dstNode.isActivate()) {
				if (dstNode.getDestinationType() == DestinationType.stow) {
					// parse headers to hashmap
					HashMap<String, String> map = new HashMap<>();
					String headers = dstNode.getHeaders();
					Document doc = Jsoup.parse(headers);
					String key = doc.getElementsByTag("key").text();
					String value = doc.getElementsByTag("value").text();
					if (StringUtil.hasText(key)) {
						map.put(key, value);
					}

					WebForwardDestination fwd = new WebForwardDestination(dstNode.getId(), fwdSrcNode, dstNode.getUrl(),
							map, progress, editors, dstNode.getTransferSyntax(), dstNode.isTranscodeOnlyUncompressed());

					if (kheopsAlbumEntities != null && !kheopsAlbumEntities.isEmpty()) {
						progress.addProgressListener((DicomProgress dicomProgress) -> {
							Attributes dcm = dicomProgress.getAttributes();
							kheopsAlbumEntities
								.forEach(kheopsAlbums -> switchingAlbum.applyAfterTransfer(kheopsAlbums, dcm));
						});
					}
					dstList.add(fwd);
				}
				else {
					DicomNode destinationNode = new DicomNode(dstNode.getAeTitle(), dstNode.getHostname(),
							dstNode.getPort());
					DicomForwardDestination dest = new DicomForwardDestination(dstNode.getId(),
							getDefaultAdvancedParameters(), fwdSrcNode, destinationNode, dstNode.getUseaetdest(),
							progress, editors, dstNode.getTransferSyntax(), dstNode.isTranscodeOnlyUncompressed());

					dstList.add(dest);
				}
			}
		}
		catch (IOException e) {
			log.error("Cannot build ForwardDestination", e);
		}
	}

	/**
	 * Apply switching album editor
	 * @param dstNode Destination
	 * @param editors List of editors
	 * @param kheopsAlbumEntities kheopsAlbumEntities
	 * @return SwitchingAlbum created
	 */
	private SwitchingAlbum applySwitchingAlbumEditor(DestinationEntity dstNode, List<AttributeEditor> editors,
			List<KheopsAlbumsEntity> kheopsAlbumEntities) {
		SwitchingAlbum switchingAlbum = new SwitchingAlbum();
		if (kheopsAlbumEntities != null && !kheopsAlbumEntities.isEmpty()) {
			editors.add(switchingEditor(dstNode, kheopsAlbumEntities, switchingAlbum));
		}
		return switchingAlbum;
	}

	/**
	 * Switching editor
	 * @param dstNode Destination
	 * @param kheopsAlbumEntities kheopsAlbum Entities
	 * @param switchingAlbum switchingAlbum
	 * @return Editor
	 */
	private AttributeEditor switchingEditor(DestinationEntity dstNode, List<KheopsAlbumsEntity> kheopsAlbumEntities,
			SwitchingAlbum switchingAlbum) {
		return (Attributes dcm, AttributeEditorContext context) -> kheopsAlbumEntities
			.forEach(kheopsAlbums -> switchingAlbum.apply(dstNode, kheopsAlbums, dcm));
	}

	/**
	 * Apply StreamRegistryEditor
	 * @param editors List of editors
	 */
	private void applyStreamRegistryEditor(List<AttributeEditor> editors) {
		editors.add(new StreamRegistryEditor());
	}

	/**
	 * Apply Filter editor
	 * @param dstNode Destination
	 * @param editors List of editors
	 */
	private void applyFilterEditor(DestinationEntity dstNode, List<AttributeEditor> editors) {
		final boolean filterBySOPClassesEnable = dstNode.isFilterBySOPClasses();
		if (filterBySOPClassesEnable) {
			editors.add(new FilterEditor(dstNode.getSOPClassUIDEntityFilters()));
		}
	}

	/**
	 * Apply Condition editor
	 * @param dstNode Destination
	 * @param editors List of editors
	 */
	private void applyConditionEditor(DestinationEntity dstNode, List<AttributeEditor> editors) {
		if (!dstNode.getCondition().isEmpty()) {
			editors.add(new ConditionEditor(dstNode.getCondition()));
		}
	}

	/**
	 * Depending on the destination, apply changes on tags: deidentification or the tag
	 * morphing profiles
	 * @param dstNode Destination
	 * @param editors List of editors
	 */
	private void applyDeIdentifyEditor(DestinationEntity dstNode, List<AttributeEditor> editors) {
		if (dstNode.getDeIdentificationProjectEntity() != null
				&& dstNode.getDeIdentificationProjectEntity().getProfileEntity() != null
				&& dstNode.isDesidentification()) {
			editors.add(new DeIdentifyEditor(dstNode));
		}
	}

	/**
	 * Depending on the destination, apply changes on tags: the tag morphing profiles
	 * @param dstNode Destination
	 * @param editors List of editors
	 */
	private void applyTagMorphingEditor(DestinationEntity dstNode, List<AttributeEditor> editors) {
		if (dstNode.getTagMorphingProjectEntity() != null
				&& dstNode.getTagMorphingProjectEntity().getProfileEntity() != null
				&& dstNode.isActivateTagMorphing()) {
			editors.add(new TagMorphingEditor(dstNode));
		}
	}

	public void reloadGatewayPersistence() {

		List<ForwardNodeEntity> list = new ArrayList<>(forwardNodeRepo.findAll());
		for (ForwardNodeEntity forwardNodeEntity : list) {
			ForwardDicomNode fwdSrcNode = new ForwardDicomNode(forwardNodeEntity.getFwdAeTitle(), null,
					forwardNodeEntity.getId());
			addAcceptedSourceNodes(fwdSrcNode, forwardNodeEntity);
			List<ForwardDestination> dstList = new ArrayList<>(forwardNodeEntity.getDestinationEntities().size());
			for (DestinationEntity dstNode : forwardNodeEntity.getDestinationEntities()) {
				addDestinationNode(dstList, fwdSrcNode, dstNode);
			}
			destMap.put(fwdSrcNode, dstList);
		}
	}

	public void update(NodeEvent event) {
		NodeEventType type = event.getEventType();
		Object src = event.getSource();
		Long id = event.getForwardNode().getId();
		String aet = event.getForwardNode().getFwdAeTitle();
		Optional<ForwardDicomNode> val = destMap.keySet().stream().filter(f -> id.equals(f.getId())).findFirst();
		ForwardDicomNode fwdNode;
		if (val.isEmpty()) {
			fwdNode = new ForwardDicomNode(aet, null, id);
			destMap.put(fwdNode, new ArrayList<>(2));
		}
		else {
			fwdNode = val.get();
		}
		if (src instanceof DicomSourceNodeEntity) {
			DicomSourceNodeEntity srcNode = (DicomSourceNodeEntity) src;
			if (type == NodeEventType.ADD) {
				fwdNode.addAcceptedSourceNode(srcNode.getId(), srcNode.getAeTitle(), srcNode.getHostname());
			}
			else if (type == NodeEventType.REMOVE) {
				fwdNode.getAcceptedSourceNodes().removeIf(s -> srcNode.getId().equals(s.getId()));
			}
			else if (type == NodeEventType.UPDATE) {
				fwdNode.getAcceptedSourceNodes().removeIf(s -> srcNode.getId().equals(s.getId()));
				fwdNode.addAcceptedSourceNode(srcNode.getId(), srcNode.getAeTitle(), srcNode.getHostname());
			}
		}
		else if (src instanceof DestinationEntity) {
			DestinationEntity dstNode = (DestinationEntity) src;
			if (type == NodeEventType.ADD) {
				addDestinationNode(destMap.get(fwdNode), fwdNode, dstNode);
			}
			else if (type == NodeEventType.REMOVE) {
				destMap.get(fwdNode).removeIf(d -> dstNode.getId().equals(d.getId()));
			}
			else if (type == NodeEventType.UPDATE) {
				destMap.get(fwdNode).removeIf(d -> dstNode.getId().equals(d.getId()));
				addDestinationNode(destMap.get(fwdNode), fwdNode, dstNode);
			}
		}
		else if (src instanceof ForwardNodeEntity) {
			ForwardNodeEntity fw = (ForwardNodeEntity) src;
			if (type == NodeEventType.ADD) {
				addAcceptedSourceNodes(fwdNode, fw);
				destMap.put(fwdNode, addDestinationNodes(fwdNode, fw));
			}
			else if (type == NodeEventType.REMOVE) {
				destMap.remove(fwdNode);
			}
			else if (type == NodeEventType.UPDATE && !aet.equals(fwdNode.getAet())) {
				ForwardDicomNode newfwdNode = new ForwardDicomNode(aet, null, id);
				for (DicomNode srcNode : fwdNode.getAcceptedSourceNodes()) {
					newfwdNode.getAcceptedSourceNodes().add(srcNode);
				}
				destMap.put(newfwdNode, destMap.remove(fwdNode));
			}
		}
		else {
			reloadGatewayPersistence();
		}
	}

	private void addAcceptedSourceNodes(ForwardDicomNode fwdSrcNode, ForwardNodeEntity forwardNodeEntity) {
		for (DicomSourceNodeEntity srcNode : forwardNodeEntity.getSourceNodes()) {
			fwdSrcNode.addAcceptedSourceNode(srcNode.getId(), srcNode.getAeTitle(), srcNode.getHostname());
		}
	}

	private List<ForwardDestination> addDestinationNodes(ForwardDicomNode fwdSrcNode,
			ForwardNodeEntity forwardNodeEntity) {
		List<ForwardDestination> dstList = new ArrayList<>(forwardNodeEntity.getDestinationEntities().size());
		for (DestinationEntity dstNode : forwardNodeEntity.getDestinationEntities()) {
			addDestinationNode(dstList, fwdSrcNode, dstNode);
		}
		return dstList;
	}

	/**
	 * When an event on the gateway setup occurs, increment the version in order for other
	 * instance to be notified that a refresh of the configuration should be done
	 */
	public void refreshVersionGatewaySetUp() {
		// Retrieve the last version to increment
		VersionEntity lastVersion = versionRepo.findTopByOrderByIdDesc();
		if (lastVersion == null) {
			// Case nothing in DB
			lastVersion = new VersionEntity();
		}
		// Increment and save the version
		lastVersion.setGatewaySetup(lastVersion.getGatewaySetup() + 1);
		versionRepo.save(lastVersion);

		// Update the current instance version
		gatewaySetUpVersion = lastVersion.getGatewaySetup();
	}

	/**
	 * Check if a refresh of the configuration should be done
	 */
	@Scheduled(fixedRate = 5000)
	public void checkRefreshGatewaySetUp() {
		// Retrieve last gateway version
		VersionEntity lastVersion = versionRepo.findTopByOrderByIdDesc();

		// Check if refresh needed: current version of the gateway setup for this instance
		// is lower than
		// the last version in DB
		if (lastVersion != null && gatewaySetUpVersion < lastVersion.getGatewaySetup()
				&& destinationRepo.findAll().stream().noneMatch(DestinationEntity::isTransferInProgress)) {
			// Check no transfer is in progress
			// Rebuild the configuration
			destMap.clear();
			reloadGatewayPersistence();

			// Update the current instance version
			gatewaySetUpVersion = lastVersion.getGatewaySetup();
		}
	}

}
