/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.kheops;

// TODO: to uncomment when SwitchingAlbum will become a service and inject kheopsApi in
// its
// constructor
/// *
// * Copyright (c) 2021 Karnak Team and other contributors.
// *
// * This program and the accompanying materials are made available under the terms of the
// Eclipse
// * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0, or the
// Apache
// * License, Version 2.0 which is available at
// https://www.apache.org/licenses/LICENSE-2.0.
// *
// * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// */
//
// import java.io.IOException;
// import java.util.ArrayList;
// import java.util.List;
// import org.dcm4che6.data.DicomObject;
// import org.dcm4che6.data.Tag;
// import org.dcm4che6.data.VR;
// import org.dcm4che6.internal.DicomObjectImpl;
// import org.json.JSONObject;
// import org.junit.Assert;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.karnak.backend.api.KheopsApi;
// import org.karnak.backend.data.entity.DestinationEntity;
// import org.karnak.backend.data.entity.KheopsAlbumsEntity;
// import org.karnak.backend.data.entity.ProjectEntity;
// import org.karnak.backend.model.kheops.MetadataSwitching;
// import org.mockito.Mockito;
// import org.springframework.boot.test.context.SpringBootTest;
//
// @SpringBootTest
// class SwitchingAlbumServiceTest {
//
// // Service
// private SwitchingAlbumService switchingAlbumService;
// private final KheopsApi kheopsApiMock = Mockito.mock(KheopsApi.class);
//
// @BeforeEach
// public void setUp() {
//
// // Build mocked service
// switchingAlbumService = new SwitchingAlbumService(kheopsApiMock);
// }
//
// @Test
// void when_no_valid_source_and_destination_should_not_update_album()
// throws IOException, InterruptedException {
// // Init data
// DestinationEntity destinationEntity = new DestinationEntity();
// KheopsAlbumsEntity kheopsAlbumsEntity = new KheopsAlbumsEntity();
// DicomObject dicomObject = new DicomObjectImpl();
// dicomObject.setString(Tag.StudyInstanceUID, VR.SH, "studyInstanceUID");
// dicomObject.setString(Tag.SeriesInstanceUID, VR.SH, "seriesInstanceUID");
// dicomObject.setString(Tag.SOPInstanceUID, VR.SH, "sopInstanceUID");
// kheopsAlbumsEntity.setAuthorizationSource("authorizationSource");
// kheopsAlbumsEntity.setAuthorizationDestination("authorizationDestination");
// kheopsAlbumsEntity.setCondition("true");
// destinationEntity.setDesidentification(true);
// ProjectEntity projectEntity = new ProjectEntity();
// byte[] tabByte = new byte[10];
// tabByte[0] = 1;
// projectEntity.setSecret(tabByte);
// destinationEntity.setProjectEntity(projectEntity);
// kheopsAlbumsEntity.setUrlAPI("http://karnak.com");
// kheopsAlbumsEntity.setId(1L);
//
// // Mock
// JSONObject jsonObject = new JSONObject();
// jsonObject.put("active", true);
// // jsonObject.put("scope", "read-send-write");
// jsonObject.put("scope", "scope");
// Mockito.when(
// kheopsApiMock.tokenIntrospect(
// Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
// .thenReturn(jsonObject);
//
// // Call service
// switchingAlbumService.apply(destinationEntity, kheopsAlbumsEntity, dicomObject);
//
// // Test results
// Assert.assertNotNull(switchingAlbumService.getSwitchingAlbumToDo());
// Assert.assertEquals(1, switchingAlbumService.getSwitchingAlbumToDo().size());
// Assert.assertNotNull(switchingAlbumService.getSwitchingAlbumToDo().get(1L));
// Assert.assertEquals(0, switchingAlbumService.getSwitchingAlbumToDo().get(1L).size());
// }
//
// @Test
// void when_valid_source_and_destination_should_update_album()
// throws IOException, InterruptedException {
// // Init data
// DestinationEntity destinationEntity = new DestinationEntity();
// KheopsAlbumsEntity kheopsAlbumsEntity = new KheopsAlbumsEntity();
// DicomObject dicomObject = new DicomObjectImpl();
// dicomObject.setString(Tag.StudyInstanceUID, VR.SH, "studyInstanceUID");
// dicomObject.setString(Tag.SeriesInstanceUID, VR.SH, "seriesInstanceUID");
// dicomObject.setString(Tag.SOPInstanceUID, VR.SH, "sopInstanceUID");
// kheopsAlbumsEntity.setAuthorizationSource("authorizationSource");
// kheopsAlbumsEntity.setAuthorizationDestination("authorizationDestination");
// kheopsAlbumsEntity.setCondition("true");
// destinationEntity.setDesidentification(true);
// ProjectEntity projectEntity = new ProjectEntity();
// byte[] tabByte = new byte[10];
// tabByte[0] = 1;
// projectEntity.setSecret(tabByte);
// destinationEntity.setProjectEntity(projectEntity);
// kheopsAlbumsEntity.setUrlAPI("http://karnak.com");
// kheopsAlbumsEntity.setId(1L);
//
// // Mock
// JSONObject jsonObject = new JSONObject();
// jsonObject.put("active", true);
// jsonObject.put("scope", "read-send-write");
// Mockito.when(
// kheopsApiMock.tokenIntrospect(
// Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
// .thenReturn(jsonObject);
//
// // Call service
// switchingAlbumService.apply(destinationEntity, kheopsAlbumsEntity, dicomObject);
//
// // Test results
// Assert.assertNotNull(switchingAlbumService.getSwitchingAlbumToDo());
// Assert.assertEquals(1, switchingAlbumService.getSwitchingAlbumToDo().size());
// Assert.assertNotNull(switchingAlbumService.getSwitchingAlbumToDo().get(1L));
// Assert.assertNotNull(switchingAlbumService.getSwitchingAlbumToDo().get(1L).get(0));
// Assert.assertNotNull(
// ((MetadataSwitching) switchingAlbumService.getSwitchingAlbumToDo().get(1L).get(0))
// .getStudyInstanceUID());
// Assert.assertNotNull(
// ((MetadataSwitching) switchingAlbumService.getSwitchingAlbumToDo().get(1L).get(0))
// .getSeriesInstanceUID());
// Assert.assertNotNull(
// ((MetadataSwitching) switchingAlbumService.getSwitchingAlbumToDo().get(1L).get(0))
// .getSOPinstanceUID());
// }
//
// @Test
// void should_apply_metadata_switching() throws IOException, InterruptedException {
//
// // Init data
// KheopsAlbumsEntity kheopsAlbumsEntity = new KheopsAlbumsEntity();
// kheopsAlbumsEntity.setId(1L);
// DicomObject dicomObject = new DicomObjectImpl();
// dicomObject.setString(Tag.AffectedSOPInstanceUID, VR.SH, "affectedSOPInstanceUID");
// kheopsAlbumsEntity.setAuthorizationSource("authorizationSource");
// kheopsAlbumsEntity.setAuthorizationDestination("authorizationDestination");
// kheopsAlbumsEntity.setUrlAPI("http://karnak.com");
// List<MetadataSwitching> metadataSwitchings = new ArrayList<>();
// MetadataSwitching metadataSwitching =
// new MetadataSwitching("studyInstanceUID", "seriesInstanceUID",
// "affectedSOPInstanceUID");
// metadataSwitchings.add(metadataSwitching);
//
// switchingAlbumService.getSwitchingAlbumToDo().putIfAbsent(1L, metadataSwitchings);
//
// // Mock
// Mockito.when(
// kheopsApiMock.shareSerie(
// Mockito.anyString(),
// Mockito.anyString(),
// Mockito.anyString(),
// Mockito.anyString(),
// Mockito.anyString()))
// .thenReturn(300);
//
// // Call service
// switchingAlbumService.applyAfterTransfer(kheopsAlbumsEntity, dicomObject);
//
// // Test result
// Assert.assertTrue(metadataSwitching.isApplied());
// }
// }
