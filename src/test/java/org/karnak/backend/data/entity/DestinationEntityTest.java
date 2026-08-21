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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.karnak.backend.enums.DestinationType;
import org.karnak.backend.enums.PseudonymType;

@DisplayNameGeneration(ReplaceUnderscores.class)
class DestinationEntityTest {

	@Nested
	class Factories {

		@Test
		void of_dicom_empty_is_a_dicom_destination_with_defaults() {
			DestinationEntity entity = DestinationEntity.ofDicomEmpty();

			assertEquals(DestinationType.dicom, entity.getDestinationType());
			assertTrue(entity.isActivate());
			assertEquals(PseudonymType.CACHE_EXTID, entity.getPseudonymType());
		}

		@Test
		void of_dicom_sets_the_connection_fields() {
			DestinationEntity entity = DestinationEntity.ofDicom("desc", "AET", "host", 104, Boolean.TRUE);

			assertEquals("desc", entity.getDescription());
			assertEquals("AET", entity.getAeTitle());
			assertEquals("host", entity.getHostname());
			assertEquals(104, entity.getPort());
			assertTrue(entity.getUseaetdest());
		}

		@Test
		void of_stow_empty_is_a_stow_destination() {
			assertEquals(DestinationType.stow, DestinationEntity.ofStowEmpty().getDestinationType());
		}

		@Test
		void of_stow_sets_the_web_fields() {
			DestinationEntity entity = DestinationEntity.ofStow("desc", "http://stow", "h=1");

			assertEquals("desc", entity.getDescription());
			assertEquals("http://stow", entity.getUrl());
			assertEquals("h=1", entity.getHeaders());
		}

		@Test
		void stow_destination_defaults_to_http_1_1() {
			// HTTP/1.1 is the safe default: HTTP/2 trips reverse-proxy per-connection
			// request caps.
			assertFalse(DestinationEntity.ofStowEmpty().isHttp2());
		}

	}

	@Test
	void exposes_its_properties_through_setters() {
		DestinationEntity entity = new DestinationEntity();
		LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);
		entity.setId(1L);
		entity.setDescription("d");
		entity.setDestinationType(DestinationType.dicom);
		entity.setActivate(false);
		entity.setCondition("cond");
		entity.setActivateTagMorphing(true);
		entity.setDesidentification(true);
		entity.setIssuerByDefault("issuer");
		entity.setPseudonymType(PseudonymType.EXTID_IN_TAG);
		entity.setTag("(0010,0010)");
		entity.setDelimiter("^");
		entity.setPosition(2);
		entity.setPseudonymUrl("http://pseudo");
		entity.setResponsePath("$.pid");
		entity.setBody("body");
		entity.setMethod("POST");
		entity.setAuthConfig("auth");
		entity.setSavePseudonym(Boolean.TRUE);
		entity.setFilterBySOPClasses(true);
		entity.setActivateNotification(true);
		entity.setNotify("a@b.com");
		entity.setNotifyObjectErrorPrefix("ERR");
		entity.setNotifyObjectRejectionPrefix("REJ");
		entity.setNotifyObjectPattern("pat");
		entity.setNotifyObjectValues("PatientID");
		entity.setNotifyInterval(45);
		entity.setAeTitle("AET");
		entity.setHostname("host");
		entity.setPort(104);
		entity.setUseaetdest(Boolean.TRUE);
		entity.setUrl("http://stow");
		entity.setHeaders("h");
		entity.setTransferSyntax("1.2.840.10008.1.2.1");
		entity.setTranscodeOnlyUncompressed(true);
		entity.setHttp2(true);
		entity.setTransferInProgress(true);
		entity.setLastTransfer(now);
		entity.setEmailLastCheck(now);

		assertEquals(1L, entity.getId());
		assertEquals("d", entity.getDescription());
		assertEquals(DestinationType.dicom, entity.getDestinationType());
		assertFalse(entity.isActivate());
		assertEquals("cond", entity.getCondition());
		assertTrue(entity.isActivateTagMorphing());
		assertTrue(entity.isDesidentification());
		assertEquals("issuer", entity.getIssuerByDefault());
		assertEquals(PseudonymType.EXTID_IN_TAG, entity.getPseudonymType());
		assertEquals("(0010,0010)", entity.getTag());
		assertEquals("^", entity.getDelimiter());
		assertEquals(2, entity.getPosition());
		assertEquals("http://pseudo", entity.getPseudonymUrl());
		assertEquals("$.pid", entity.getResponsePath());
		assertEquals("body", entity.getBody());
		assertEquals("POST", entity.getMethod());
		assertEquals("auth", entity.getAuthConfig());
		assertTrue(entity.getSavePseudonym());
		assertTrue(entity.isFilterBySOPClasses());
		assertTrue(entity.isActivateNotification());
		assertEquals("a@b.com", entity.getNotify());
		assertEquals("ERR", entity.getNotifyObjectErrorPrefix());
		assertEquals("REJ", entity.getNotifyObjectRejectionPrefix());
		assertEquals("pat", entity.getNotifyObjectPattern());
		assertEquals("PatientID", entity.getNotifyObjectValues());
		assertEquals(45, entity.getNotifyInterval());
		assertEquals("AET", entity.getAeTitle());
		assertEquals("host", entity.getHostname());
		assertEquals(104, entity.getPort());
		assertTrue(entity.getUseaetdest());
		assertEquals("http://stow", entity.getUrl());
		assertEquals("h", entity.getHeaders());
		assertEquals("1.2.840.10008.1.2.1", entity.getTransferSyntax());
		assertTrue(entity.isTranscodeOnlyUncompressed());
		assertTrue(entity.isHttp2());
		assertTrue(entity.isTransferInProgress());
		assertEquals(now, entity.getLastTransfer());
		assertEquals(now, entity.getEmailLastCheck());
	}

	@Test
	void exposes_its_relationship_collections() {
		DestinationEntity entity = new DestinationEntity();
		entity.setForwardNodeEntity(new ForwardNodeEntity());
		entity.setDeIdentificationProjectEntity(new ProjectEntity());
		entity.setTagMorphingProjectEntity(new ProjectEntity());
		entity.setKheopsAlbumEntities(List.of(new KheopsAlbumsEntity()));
		entity.setSOPClassUIDEntityFilters(Set.of(new SOPClassUIDEntity("ciod", "uid", "CT Image Storage")));

		assertEquals(1, entity.getKheopsAlbumEntities().size());
		assertEquals(1, entity.getSOPClassUIDEntityFilters().size());
		assertEquals(Set.of("CT Image Storage"), entity.retrieveSOPClassUIDFiltersName());
		assertEquals("ForwardNode", forwardClassName(entity));
	}

	private static String forwardClassName(DestinationEntity entity) {
		return entity.getForwardNodeEntity() != null ? "ForwardNode" : "none";
	}

	@Test
	void matches_filter_on_text_and_port() {
		DestinationEntity entity = DestinationEntity.ofDicom("scanner", "AET", "10.0.0.1", 104, Boolean.FALSE);

		assertTrue(entity.matchesFilter("scan"));
		assertTrue(entity.matchesFilter("AET"));
		assertTrue(entity.matchesFilter("10.0"));
		assertTrue(entity.matchesFilter("104"));
		assertFalse(entity.matchesFilter("absent"));
	}

	@Nested
	class StringRepresentations {

		@Test
		void to_string_for_a_dicom_destination_mentions_the_aetitle() {
			DestinationEntity entity = DestinationEntity.ofDicom("d", "AET", "host", 104, Boolean.FALSE);

			assertTrue(entity.toString().contains("aeTitle=AET"));
		}

		@Test
		void to_string_for_a_stow_destination_mentions_the_url() {
			DestinationEntity entity = DestinationEntity.ofStow("d", "http://stow", "h");

			assertTrue(entity.toString().contains("url=http://stow"));
		}

		@Test
		void to_string_for_an_unknown_type_falls_back() {
			DestinationEntity entity = new DestinationEntity();

			assertTrue(entity.toString().contains("Destination [id="));
		}

		@Test
		void retrieve_string_reference_depends_on_the_type() {
			assertEquals("AET",
					DestinationEntity.ofDicom("d", "AET", "host", 104, Boolean.FALSE).retrieveStringReference());
			DestinationEntity stow = DestinationEntity.ofStow("d", "http://stow", "h");
			assertTrue(stow.retrieveStringReference().startsWith("http://stow:"));
			assertEquals("Type of destination is unknown", new DestinationEntity().retrieveStringReference());
		}

		@Test
		void dicom_notification_string_lists_host_aet_and_port() {
			DestinationEntity entity = DestinationEntity.ofDicom("d", "AET", "host", 104, Boolean.FALSE);

			assertEquals("Host=host AET=AET Port=104", entity.toStringDicomNotificationDestination());
		}

	}

}