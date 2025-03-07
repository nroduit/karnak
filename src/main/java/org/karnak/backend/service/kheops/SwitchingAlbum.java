/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.kheops;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.json.JSONObject;
import org.karnak.backend.api.KheopsApi;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.KheopsAlbumsEntity;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.data.entity.SecretEntity;
import org.karnak.backend.model.action.ActionItem;
import org.karnak.backend.model.action.UID;
import org.karnak.backend.model.expression.ExprCondition;
import org.karnak.backend.model.expression.ExpressionResult;
import org.karnak.backend.model.kheops.MetadataSwitching;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.model.profiles.CleanPixelData;
import org.karnak.backend.model.profiles.ProfileItem;
import org.karnak.backend.service.profilepipe.Profile;

@Slf4j
public class SwitchingAlbum {

	public static final List<String> MIN_SCOPE_SOURCE = List.of("read", "send");

	public static final List<String> MIN_SCOPE_DESTINATION = List.of("write");

	private final KheopsApi kheopsAPI;

	private final Map<Long, List> switchingAlbumToDo = new WeakHashMap<>();

	public SwitchingAlbum() {
		kheopsAPI = new KheopsApi();
	}

	private static HMAC generateHMAC(DestinationEntity destinationEntity) {
		if (destinationEntity.isDesidentification()) {
			ProjectEntity projectEntity = destinationEntity.getDeIdentificationProjectEntity();
			SecretEntity secretEntity = projectEntity.retrieveActiveSecret();
			return secretEntity != null ? new HMAC(secretEntity.getSecretKey()) : null;
		}
		return null;
	}

	private static String hashUIDonDeidentification(DestinationEntity destinationEntity, String inputUID, HMAC hmac,
			int tag) {
		return destinationEntity.isDesidentification() && hmac != null
				&& getAction(destinationEntity, tag) instanceof UID ? hmac.uidHash(inputUID) : inputUID;
	}

	private static boolean validateCondition(String condition, Attributes dcm) {
		final ExprCondition conditionKheops = new ExprCondition(dcm);
		return (Boolean) ExpressionResult.get(condition, conditionKheops, Boolean.class);
	}

	public static boolean validateIntrospectedToken(JSONObject introspectObject, List<String> validMinScope) {
		boolean valid = true;
		if (!introspectObject.getBoolean("active")) {
			return false;
		}
		final String scope = introspectObject.getString("scope");
		for (String minScope : validMinScope) {
			valid = scope.contains(minScope) && valid;
		}
		return valid;
	}

	public static ActionItem getAction(DestinationEntity destinationEntity, int tag) {
		if (destinationEntity.getDeIdentificationProjectEntity() != null
				&& destinationEntity.getDeIdentificationProjectEntity().getProfileEntity() != null) {
			List<ProfileItem> profileItems = Profile
				.getProfileItems(destinationEntity.getDeIdentificationProjectEntity().getProfileEntity());
			for (ProfileItem profileItem : profileItems.stream().filter(p -> !(p instanceof CleanPixelData)).toList()) {
				try {
					ActionItem action = profileItem.getAction(new Attributes(), new Attributes(), tag,
							new HMAC(HMAC.generateRandomKey()));
					if (action != null) {
						return action;
					}
				}
				catch (Exception e) {
					log.error("Switching KHEOPS, cannot get action for the destination: {}",
							destinationEntity.getDescription(), e);
				}
			}
		}
		return null;
	}

	public void apply(DestinationEntity destinationEntity, KheopsAlbumsEntity kheopsAlbumsEntity, Attributes dcm) {
		String authorizationSource = kheopsAlbumsEntity.getAuthorizationSource();
		String authorizationDestination = kheopsAlbumsEntity.getAuthorizationDestination();
		String condition = kheopsAlbumsEntity.getCondition();
		HMAC hmac = generateHMAC(destinationEntity);
		String studyInstanceUID = hashUIDonDeidentification(destinationEntity, dcm.getString(Tag.StudyInstanceUID),
				hmac, Tag.StudyInstanceUID);
		String seriesInstanceUID = hashUIDonDeidentification(destinationEntity, dcm.getString(Tag.SeriesInstanceUID),
				hmac, Tag.SeriesInstanceUID);
		String sopInstanceUID = dcm.getString(Tag.SOPInstanceUID);
		String urlAPI = kheopsAlbumsEntity.getUrlAPI();
		Long id = kheopsAlbumsEntity.getId();
		if (!switchingAlbumToDo.containsKey(id)) {
			switchingAlbumToDo.put(id, new ArrayList<MetadataSwitching>());
		}
		ArrayList<MetadataSwitching> metadataToDo = (ArrayList<MetadataSwitching>) switchingAlbumToDo.get(id);

		if ((condition == null || condition.isEmpty() || validateCondition(condition, dcm)) && metadataToDo.stream()
			.noneMatch(metadataSwitching -> metadataSwitching.getSeriesInstanceUID().equals(seriesInstanceUID))) {
			final boolean validAuthorizationSource = validateToken(MIN_SCOPE_SOURCE, urlAPI, authorizationSource);
			final boolean validDestinationSource = validateToken(MIN_SCOPE_DESTINATION, urlAPI,
					authorizationDestination);

			if (validAuthorizationSource && validDestinationSource) {
				metadataToDo.add(new MetadataSwitching(studyInstanceUID, seriesInstanceUID, sopInstanceUID));
			}
			else {
				log.warn("Can't validate a token for switching KHEOPS album [{}]. The series [{}] won't be shared.",
						kheopsAlbumsEntity.getId(), seriesInstanceUID);
			}
		}
	}

	private boolean validateToken(List<String> validMinScope, String urlAPI, String introspectToken) {
		try {
			JSONObject responseIntrospect = kheopsAPI.tokenIntrospect(urlAPI, introspectToken, introspectToken);
			return validateIntrospectedToken(responseIntrospect, validMinScope);
		}
		catch (InterruptedException e) {
			log.warn("Session interrupted", e);
			Thread.currentThread().interrupt();
		}
		catch (Exception e) {
			log.error("Invalid token", e);
		}
		return false;
	}

	public void applyAfterTransfer(KheopsAlbumsEntity kheopsAlbumsEntity, Attributes dcm) {
		String sopInstanceUID = dcm.getString(Tag.AffectedSOPInstanceUID);
		if (sopInstanceUID == null) {
			throw new IllegalStateException("AffectedSOPInstanceUID not found");
		}
		Long id = kheopsAlbumsEntity.getId();
		String authorizationSource = kheopsAlbumsEntity.getAuthorizationSource();
		String authorizationDestination = kheopsAlbumsEntity.getAuthorizationDestination();
		String urlAPI = kheopsAlbumsEntity.getUrlAPI();

		ArrayList<MetadataSwitching> metadataToDo = (ArrayList<MetadataSwitching>) switchingAlbumToDo.get(id);
		metadataToDo.forEach(metadataSwitching -> {
			if (metadataSwitching.getSOPinstanceUID().equals(sopInstanceUID) && !metadataSwitching.isApplied()) {
				int status = shareSerie(urlAPI, metadataSwitching.getStudyInstanceUID(),
						metadataSwitching.getSeriesInstanceUID(), authorizationSource, authorizationDestination);
				if (status >= 400 && status <= 599) {
					log.warn("Can't share the serie [{}] for switching KHEOPS album [{}]. The response status is {}",
							metadataSwitching.getSeriesInstanceUID(), id, status);
					metadataSwitching.setApplied(false);
				}
				else {
					metadataSwitching.setApplied(true);
				}
			}
		});
	}

	private int shareSerie(String urlAPI, String studyInstanceUID, String seriesInstanceUID, String authorizationSource,
			String authorizationDestination) {
		try {
			return kheopsAPI.shareSerie(studyInstanceUID, seriesInstanceUID, urlAPI, authorizationSource,
					authorizationDestination);
		}
		catch (InterruptedException e) {
			log.warn("Session interrupted", e);
			Thread.currentThread().interrupt();
		}
		catch (Exception e) {
			log.error("Can't share the serie {} in the study {}", seriesInstanceUID, studyInstanceUID, e);
		}
		return -1;
	}

}
