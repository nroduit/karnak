/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.gateway;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.UID;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.DicomSourceNodeEntity;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.data.entity.KheopsAlbumsEntity;
import org.karnak.backend.data.entity.VersionEntity;
import org.karnak.backend.data.repo.DestinationRepo;
import org.karnak.backend.data.repo.ForwardNodeRepo;
import org.karnak.backend.data.repo.VersionRepo;
import org.karnak.backend.dicom.CommonStorageSopClasses;
import org.karnak.backend.dicom.DicomForwardDestination;
import org.karnak.backend.dicom.ForwardDestination;
import org.karnak.backend.dicom.ForwardDicomNode;
import org.karnak.backend.dicom.NullForwardDestination;
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
import org.karnak.backend.service.profilepipe.DeidentifyImageService;
import org.karnak.backend.util.SystemPropertyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.weasis.core.util.LangUtil;
import org.weasis.core.util.StringUtil;
import org.weasis.core.util.annotations.Generated;
import org.weasis.dicom.param.AdvancedParams;
import org.weasis.dicom.param.AttributeEditor;
import org.weasis.dicom.param.AttributeEditorContext;
import org.weasis.dicom.param.ConnectOptions;
import org.weasis.dicom.param.DicomNode;
import org.weasis.dicom.param.DicomProgress;
import org.weasis.dicom.param.ListenerParams;
import org.weasis.dicom.param.TlsOptions;
import org.weasis.dicom.tool.DicomListener;

@Service
@Slf4j
@Generated()
@NullUnmarked
public class GatewaySetUpService {

	// Repositories
	private final ForwardNodeRepo forwardNodeRepo;

	private final VersionRepo versionRepo;

	private final DestinationRepo destinationRepo;

	// Live routing table, shared by reference with CStoreSCPService (via
	// GatewayService.init) and iterated by C-STORE threads while the UI and the
	// configuration refresh mutate it. Concurrent map plus copy-on-write value
	// lists keep those readers safe; it is refreshed in place, never cleared.
	private final Map<ForwardDicomNode, List<ForwardDestination>> destMap;

	private final DeidentifyImageService deidentifyImageService;

	@Value("${forward.prenegotiate-sop-classes:true}")
	private boolean preNegotiateSopClasses;

	@Value("${forward.pool.max-per-destination:1}")
	private int poolMaxPerDestination;

	@Value("${forward.pool.max-ops-invoked:16}")
	private int poolMaxOpsInvoked;

	@Getter
	private final String listenerAET;

	@Getter
	private final int listenerPort;

	@Getter
	private final Boolean listenerTLS;

	@Getter
	private final String clientKey;

	@Getter
	private final String clientKeyPwd;

	@Getter
	private final String truststorePwd;

	@Getter
	private final String truststore;

	// Version gateway setup for this instance
	private long gatewaySetUpVersion;

	@Autowired
	public GatewaySetUpService(final ForwardNodeRepo forwardNodeRepo, final VersionRepo versionRepo,
			final DestinationRepo destinationRepo, final DeidentifyImageService deidentifyImageService)
			throws Exception {
		this.forwardNodeRepo = forwardNodeRepo;
		this.versionRepo = versionRepo;
		this.destinationRepo = destinationRepo;
		this.deidentifyImageService = deidentifyImageService;
		this.destMap = new ConcurrentHashMap<>();

		listenerAET = SystemPropertyUtil.retrieveSystemProperty("DICOM_LISTENER_AET", "KARNAK-GATEWAY");
		listenerPort = SystemPropertyUtil.retrieveIntegerSystemProperty("DICOM_LISTENER_PORT", 11119);
		listenerTLS = LangUtil.emptyToFalse(SystemPropertyUtil.retrieveSystemProperty("DICOM_LISTENER_TLS", "false"));

		clientKey = SystemPropertyUtil.retrieveSystemProperty("TLS_KEYSTORE_PATH", null);
		clientKeyPwd = SystemPropertyUtil.retrieveSystemProperty("TLS_KEYSTORE_SECRET", null);
		truststore = SystemPropertyUtil.retrieveSystemProperty("TLS_TRUSTSTORE_PATH", null);
		truststorePwd = SystemPropertyUtil.retrieveSystemProperty("TLS_TRUSTSTORE_SECRET", null);

		String localAET = SystemPropertyUtil.retrieveSystemProperty("LOCAL_NODE_AE_TITLE", "KARNAK-LOCAL");
		Integer localPort = SystemPropertyUtil.retrieveIntegerSystemProperty("LOCAL_NODE_PORT", null);
		String folder = SystemPropertyUtil.retrieveSystemProperty("LOCAL_NODE_STORAGE_PATH", null);
		String filePattern = SystemPropertyUtil.retrieveSystemProperty("LOCAL_NODE_FILEPATH_PATTERN",
				"{00100010}/{00080060}/{0020000E}/{00080018}.dcm");

		if (localPort != null && StringUtil.hasText(folder)) {
			DicomNode localNode = new DicomNode(localAET, null, localPort);
			log.info("Local DICOM Node configured: {}", localNode);
			DicomListener listener = new DicomListener(Path.of(folder));
			ListenerParams params = new ListenerParams(null, true, filePattern, null);
			listener.start(localNode, params);
		}

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

	/**
	 * Advanced parameters for outgoing destination associations, with the async C-STORE
	 * window ({@code maxOpsInvoked}) taken from configuration so it can be balanced
	 * against the pool size.
	 */
	private AdvancedParams getDestinationAdvancedParameters() {
		AdvancedParams params = getDefaultAdvancedParameters();
		params.getConnectOptions().setMaxOpsInvoked(Math.max(1, poolMaxOpsInvoked));
		return params;
	}

	@Override
	public String toString() {
		return "Hostname=" + DicomNode.convertToIP(null) + " AETitle=" + listenerAET + " Port=" + listenerPort;
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

	public List<ForwardDestination> getDestinations(ForwardDicomNode fwdSrcNode) {
		if (fwdSrcNode == null) {
			return Collections.emptyList();
		}
		return destMap.getOrDefault(fwdSrcNode, Collections.emptyList());
	}

	public Map<ForwardDicomNode, List<ForwardDestination>> getDestinations() {
		return destMap;
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
				if (dstNode.isVirtualDestination()) {
					// Report-only destination: process the dataset through the editors so
					// the conformance report reflects what would be sent, but route the
					// DICOM to devnull instead of forwarding it.
					NullForwardDestination fwd = new NullForwardDestination(dstNode.getId(), fwdSrcNode, editors);
					fwd.setBuildConformanceReport(dstNode.isBuildConformanceReport());
					fwd.setCheckValueConformity(dstNode.isCheckValueConformity());
					fwd.setDeepSequenceValidation(dstNode.isDeepSequenceValidation());
					fwd.setDeidentified(dstNode.isDesidentification());
					fwd.setImageIdentityCheck(dstNode.isImageIdentityCheck());
					dstList.add(fwd);
				}
				else if (dstNode.getDestinationType() == DestinationType.stow) {
					WebForwardDestination fwd = new WebForwardDestination(dstNode.getId(), fwdSrcNode, dstNode.getUrl(),
							parseHeaders(dstNode.getHeaders()), progress, editors, dstNode.getTransferSyntax(),
							dstNode.isTranscodeOnlyUncompressed(),
							dstNode.isHttp2() ? HttpClient.Version.HTTP_2 : HttpClient.Version.HTTP_1_1);
					fwd.setBuildConformanceReport(dstNode.isBuildConformanceReport());
					fwd.setCheckValueConformity(dstNode.isCheckValueConformity());
					fwd.setDeepSequenceValidation(dstNode.isDeepSequenceValidation());
					fwd.setDeidentified(dstNode.isDesidentification());
					fwd.setImageIdentityCheck(dstNode.isImageIdentityCheck());

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
							getDestinationAdvancedParameters(), fwdSrcNode, destinationNode, dstNode.getUseaetdest(),
							progress, editors, dstNode.getTransferSyntax(), dstNode.isTranscodeOnlyUncompressed(),
							resolvePoolSize(dstNode));
					dest.setBuildConformanceReport(dstNode.isBuildConformanceReport());
					dest.setCheckValueConformity(dstNode.isCheckValueConformity());
					dest.setDeepSequenceValidation(dstNode.isDeepSequenceValidation());
					dest.setDeidentified(dstNode.isDesidentification());
					dest.setImageIdentityCheck(dstNode.isImageIdentityCheck());

					preNegotiateCommonSopClasses(dest);
					dstList.add(dest);
				}
			}
		}
		catch (IOException e) {
			log.error("Cannot build ForwardDestination", e);
		}
	}

	/**
	 * Effective connection-pool size for a destination: its per-destination value when
	 * set, otherwise the global {@code forward.pool.max-per-destination} default. Never
	 * below 1.
	 */
	private int resolvePoolSize(DestinationEntity dstNode) {
		Integer perDestination = dstNode.getConcurrentConnections();
		int size = perDestination != null ? perDestination : poolMaxPerDestination;
		return Math.max(1, size);
	}

	private void preNegotiateCommonSopClasses(DicomForwardDestination dest) {
		if (!preNegotiateSopClasses) {
			return;
		}
		// Every association in the pool must negotiate the same presentation contexts.
		dest.getStreamSCUs().forEach(scu -> {
			for (String sopClassUid : CommonStorageSopClasses.UIDS) {
				// addData adds the SOP Class with Explicit + Implicit VR Little Endian
				scu.addData(sopClassUid, UID.ExplicitVRLittleEndian);
			}
		});
	}

	/**
	 * Parses the destination headers ({@code <key>/<value>} XML) into a single-entry map.
	 */
	static Map<String, String> parseHeaders(@Nullable String headers) {
		Map<String, String> map = new HashMap<>();
		if (!StringUtil.hasText(headers)) {
			return map;
		}
		Document doc = Jsoup.parse(headers);
		Elements keys = doc.getElementsByTag("key");
		Elements values = doc.getElementsByTag("value");
		// one <key>/<value> pair per header
		for (int i = 0; i < keys.size(); i++) {
			String key = keys.get(i).text().trim();
			if (StringUtil.hasText(key)) {
				map.put(key, i < values.size() ? values.get(i).text().trim() : "");
			}
		}
		return map;
	}

	/**
	 * Adds a KHEOPS switching-album editor when the destination has albums, and returns
	 * it.
	 */
	private SwitchingAlbum applySwitchingAlbumEditor(DestinationEntity dstNode, List<AttributeEditor> editors,
			List<KheopsAlbumsEntity> kheopsAlbumEntities) {
		SwitchingAlbum switchingAlbum = new SwitchingAlbum();
		if (kheopsAlbumEntities != null && !kheopsAlbumEntities.isEmpty()) {
			editors.add(switchingEditor(dstNode, kheopsAlbumEntities, switchingAlbum));
		}
		return switchingAlbum;
	}

	/** Editor that switches the destination's KHEOPS albums for each instance. */
	private AttributeEditor switchingEditor(DestinationEntity dstNode, List<KheopsAlbumsEntity> kheopsAlbumEntities,
			SwitchingAlbum switchingAlbum) {
		return (Attributes dcm, AttributeEditorContext context) -> kheopsAlbumEntities
			.forEach(kheopsAlbums -> switchingAlbum.apply(dstNode, kheopsAlbums, dcm));
	}

	private void applyStreamRegistryEditor(List<AttributeEditor> editors) {
		editors.add(new StreamRegistryEditor());
	}

	/** Adds a SOP-class filter editor when filtering is enabled on the destination. */
	private void applyFilterEditor(DestinationEntity dstNode, List<AttributeEditor> editors) {
		if (dstNode.isFilterBySOPClasses()) {
			editors.add(new FilterEditor(dstNode.getSOPClassUIDEntityFilters()));
		}
	}

	/** Adds a condition editor when the destination defines one. */
	private void applyConditionEditor(DestinationEntity dstNode, List<AttributeEditor> editors) {
		if (!dstNode.getCondition().isEmpty()) {
			editors.add(new ConditionEditor(dstNode.getCondition()));
		}
	}

	/**
	 * Adds the de-identification editor when the destination has an active de-id profile.
	 */
	private void applyDeIdentifyEditor(DestinationEntity dstNode, List<AttributeEditor> editors) {
		if (dstNode.getDeIdentificationProjectEntity() != null
				&& dstNode.getDeIdentificationProjectEntity().getProfileEntity() != null
				&& dstNode.isDesidentification()) {
			editors.add(new DeIdentifyEditor(dstNode, deidentifyImageService));
		}
	}

	/**
	 * Adds the tag-morphing editor when the destination has an active tag-morphing
	 * profile.
	 */
	private void applyTagMorphingEditor(DestinationEntity dstNode, List<AttributeEditor> editors) {
		if (dstNode.getTagMorphingProjectEntity() != null
				&& dstNode.getTagMorphingProjectEntity().getProfileEntity() != null
				&& dstNode.isActivateTagMorphing()) {
			editors.add(new TagMorphingEditor(dstNode, deidentifyImageService));
		}
	}

	public void reloadGatewayPersistence() {
		// Build the whole new routing table aside first: constructing the
		// destinations opens SCU devices and takes time, and C-STORE threads keep
		// resolving their forward node against destMap meanwhile.
		Map<ForwardDicomNode, List<ForwardDestination>> fresh = new HashMap<>();
		List<ForwardNodeEntity> list = new ArrayList<>(forwardNodeRepo.findAll());
		for (ForwardNodeEntity forwardNodeEntity : list) {
			ForwardDicomNode fwdSrcNode = new ForwardDicomNode(forwardNodeEntity.getFwdAeTitle(), null,
					forwardNodeEntity.getId());
			addAcceptedSourceNodes(fwdSrcNode, forwardNodeEntity);
			fresh.put(fwdSrcNode, addDestinationNodes(fwdSrcNode, forwardNodeEntity));
		}
		// Then swap entry by entry instead of clear-and-rebuild, so the table never
		// goes empty under live traffic. The remove before each put matters:
		// ForwardDicomNode equality is AET-based and the accepted source nodes live
		// in the key object, so a plain put would keep the stale key - and its
		// stale authorization list - when an equal one is already present.
		destMap.keySet().retainAll(fresh.keySet());
		fresh.forEach((node, dstList) -> {
			destMap.remove(node);
			destMap.put(node, dstList);
		});
	}

	public void update(NodeEvent event) {
		NodeEventType type = event.getEventType();
		Object src = event.getSource();
		Long id = event.getForwardNode().getId();
		String aet = event.getForwardNode().getFwdAeTitle();
		ForwardDicomNode fwdNode = destMap.keySet().stream().filter(f -> id.equals(f.getId())).findFirst().orElse(null);
		if (fwdNode == null) {
			fwdNode = new ForwardDicomNode(aet, null, id);
			destMap.put(fwdNode, new CopyOnWriteArrayList<>());
		}
		switch (src) {
			case DicomSourceNodeEntity srcNode -> {
				if (type == NodeEventType.ADD) {
					addAcceptedSourceNode(fwdNode, srcNode);
				}
				else if (type == NodeEventType.REMOVE) {
					fwdNode.getAcceptedSourceNodes().removeIf(s -> srcNode.getId().equals(s.getId()));
				}
				else if (type == NodeEventType.UPDATE) {
					fwdNode.getAcceptedSourceNodes().removeIf(s -> srcNode.getId().equals(s.getId()));
					addAcceptedSourceNode(fwdNode, srcNode);
				}
			}
			case DestinationEntity dstNode -> {
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
			case ForwardNodeEntity fw -> {
				if (type == NodeEventType.ADD) {
					addAcceptedSourceNodes(fwdNode, fw);
					destMap.put(fwdNode, addDestinationNodes(fwdNode, fw));
				}
				else if (type == NodeEventType.REMOVE) {
					destMap.remove(fwdNode);
				}
				else if (type == NodeEventType.UPDATE && !aet.equals(fwdNode.getAet())) {
					ForwardDicomNode newFwdNode = new ForwardDicomNode(aet, null, id);
					newFwdNode.getAcceptedSourceNodes().addAll(fwdNode.getAcceptedSourceNodes());
					destMap.put(newFwdNode, destMap.remove(fwdNode));
				}
			}
			case null, default -> reloadGatewayPersistence();
		}
	}

	private void addAcceptedSourceNodes(ForwardDicomNode fwdSrcNode, ForwardNodeEntity forwardNodeEntity) {
		for (DicomSourceNodeEntity srcNode : forwardNodeEntity.getSourceNodes()) {
			addAcceptedSourceNode(fwdSrcNode, srcNode);
		}
	}

	/** Registers a source node, honoring its "Check the hostname" setting. */
	private static void addAcceptedSourceNode(ForwardDicomNode fwdNode, DicomSourceNodeEntity srcNode) {
		boolean checkHostname = Boolean.TRUE.equals(srcNode.getCheckHostname())
				&& StringUtil.hasText(srcNode.getHostname());
		fwdNode.addAcceptedSourceNode(srcNode.getId(), srcNode.getAeTitle(), srcNode.getHostname(), checkHostname);
	}

	/**
	 * Destination list for a forward node. Copy-on-write because C-STORE threads iterate
	 * it while UI events add and remove destinations in place.
	 */
	private List<ForwardDestination> addDestinationNodes(ForwardDicomNode fwdSrcNode,
			ForwardNodeEntity forwardNodeEntity) {
		List<ForwardDestination> dstList = new CopyOnWriteArrayList<>();
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
		// Retrieve the last gateway version
		VersionEntity lastVersion = versionRepo.findTopByOrderByIdDesc();

		// Check if refresh needed: the current version of the gateway setup for
		// this instance is lower than the last version in DB
		if (lastVersion != null && gatewaySetUpVersion < lastVersion.getGatewaySetup()
				&& destinationRepo.findAll().stream().noneMatch(DestinationEntity::isTransferInProgress)) {
			// Check no transfer is in progress
			// Rebuild the configuration; the reload swaps the routing table in place
			// (no clear), so a transfer racing the in-progress heuristic above still
			// resolves its forward node.
			reloadGatewayPersistence();

			// Update the current instance version
			gatewaySetUpVersion = lastVersion.getGatewaySetup();
		}
	}

}
