/*
 * Copyright (c) 2009-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.net.Association;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.karnak.backend.dicom.DicomForwardDestination;
import org.karnak.backend.dicom.ForwardDicomNode;
import org.karnak.backend.dicom.NullForwardDestination;
import org.karnak.backend.dicom.Params;
import org.karnak.backend.dicom.WebForwardDestination;
import org.karnak.backend.exception.AbortException;
import org.karnak.backend.model.event.ConformanceCollectEvent;
import org.karnak.backend.model.event.TransferMonitoringEvent;
import org.karnak.backend.model.monitoring.MonitoringEntry;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.weasis.dicom.param.AttributeEditor;
import org.weasis.dicom.param.AttributeEditorContext.Abort;
import org.weasis.dicom.param.DicomNode;
import org.weasis.dicom.param.DicomProgress;
import org.weasis.dicom.param.DicomState;
import org.weasis.dicom.util.StoreFromStreamSCU;
import org.weasis.dicom.web.DicomStowRS;
import org.weasis.dicom.web.HttpException;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ForwardServiceTest {

	private static final String TS = UID.ExplicitVRLittleEndian;

	private static final String IUID = "1.2.3.4.5";

	private static final String CUID = UID.SecondaryCaptureImageStorage;

	private ApplicationEventPublisher publisher;

	private ForwardService forwardService;

	private ForwardDicomNode fwdNode;

	@BeforeEach
	void setUp() {
		publisher = mock(ApplicationEventPublisher.class);
		forwardService = new ForwardService(publisher);
		fwdNode = new ForwardDicomNode("SOURCE");
	}

	// --- selectTransferSyntax (pure logic)
	// -------------------------------------------------

	@Test
	void select_transfer_syntax_returns_file_syntax_when_supported() {
		Association as = mock(Association.class);
		when(as.getTransferSyntaxesFor(CUID)).thenReturn(Set.of(TS, UID.JPEGLosslessSV1));

		assertEquals(TS, ForwardService.selectTransferSyntax(as, CUID, TS));
	}

	@Test
	void select_transfer_syntax_falls_back_to_explicit_vr_little_endian() {
		Association as = mock(Association.class);
		when(as.getTransferSyntaxesFor(CUID)).thenReturn(Set.of(UID.ExplicitVRLittleEndian));

		assertEquals(UID.ExplicitVRLittleEndian, ForwardService.selectTransferSyntax(as, CUID, UID.JPEGLosslessSV1));
	}

	@Test
	void select_transfer_syntax_falls_back_to_implicit_vr_little_endian() {
		Association as = mock(Association.class);
		when(as.getTransferSyntaxesFor(CUID)).thenReturn(Set.of(UID.JPEGBaseline8Bit));

		assertEquals(UID.ImplicitVRLittleEndian, ForwardService.selectTransferSyntax(as, CUID, UID.JPEGLosslessSV1));
	}

	// --- DICOM transferOther (works from an in-memory Attributes copy)
	// ----------------------

	@Test
	void dicom_transfer_other_fast_path_forwards_and_monitors_success() throws Exception {
		StoreFromStreamSCU scu = readyStreamSCU();
		DicomForwardDestination dest = dicomDestination(scu, List.of());

		forwardService.transferOther(fwdNode, dest, lease(scu), dataset(), params(null));

		verify(scu).cstore(eq(CUID), eq(IUID), anyInt(), any(), eq(TS));
		MonitoringEntry event = captureSingleEvent();
		assertTrue(event.sent());
		assertFalse(event.error());
	}

	@Test
	void dicom_transfer_other_file_exception_is_swallowed_and_not_forwarded() throws Exception {
		StoreFromStreamSCU scu = readyStreamSCU();
		AttributeEditor blocker = (dcm, ctx) -> ctx.setAbort(Abort.FILE_EXCEPTION, "blocked");
		DicomForwardDestination dest = dicomDestination(scu, List.of(blocker));

		// FILE_EXCEPTION must not propagate (other objects keep flowing)
		forwardService.transferOther(fwdNode, dest, lease(scu), dataset(), params(null));

		verify(scu, never()).cstore(anyString(), anyString(), anyInt(), any(), anyString());
		MonitoringEntry event = captureSingleEvent();
		assertFalse(event.sent());
		assertFalse(event.error());
		assertEquals("blocked", event.reason());
	}

	@Test
	void dicom_transfer_other_connection_exception_is_rethrown() {
		StoreFromStreamSCU scu = readyStreamSCU();
		AttributeEditor aborter = (dcm, ctx) -> ctx.setAbort(Abort.CONNECTION_EXCEPTION, "down");
		DicomForwardDestination dest = dicomDestination(scu, List.of(aborter));

		assertThrows(AbortException.class,
				() -> forwardService.transferOther(fwdNode, dest, lease(scu), dataset(), params(null)));
	}

	@Test
	void dicom_transfer_other_when_association_not_ready_records_error_without_throwing() throws Exception {
		StoreFromStreamSCU scu = mock(StoreFromStreamSCU.class);
		when(scu.isReadyForDataTransfer()).thenReturn(false);
		when(scu.selectTransferSyntax(anyString(), anyString())).thenReturn(TS);
		when(scu.getState()).thenReturn(new DicomState(new DicomProgress()));
		DicomForwardDestination dest = dicomDestination(scu, List.of());

		forwardService.transferOther(fwdNode, dest, lease(scu), dataset(), params(null));

		verify(scu, never()).cstore(anyString(), anyString(), anyInt(), any(), anyString());
		MonitoringEntry event = captureSingleEvent();
		assertFalse(event.sent());
		assertTrue(event.error());
	}

	@Test
	void dicom_transfer_other_editor_path_forwards_and_monitors_success() throws Exception {
		StoreFromStreamSCU scu = readyStreamSCU();
		AttributeEditor anonymizer = (dcm, ctx) -> dcm.setString(Tag.PatientID, VR.LO, "ANON");
		DicomForwardDestination dest = dicomDestination(scu, List.of(anonymizer));

		forwardService.transferOther(fwdNode, dest, lease(scu), dataset(), params(null));

		verify(scu).cstore(eq(CUID), eq(IUID), anyInt(), any(), eq(TS));
		assertTrue(captureSingleEvent().sent());
	}

	// --- DICOMWeb transferOther
	// -------------------------------------------------------------

	@Test
	void web_transfer_other_fast_path_uploads_and_monitors_success() throws Exception {
		// Regression for the fix that moved the success notify/monitor out of the editor
		// branch.
		DicomStowRS stow = mock(DicomStowRS.class);
		WebForwardDestination dest = webDestination(stow, List.of());
		Attributes copy = dataset();

		forwardService.transferOther(fwdNode, dest, copy, params(null));

		verify(stow).uploadDicom(eq(copy), eq(TS));
		assertTrue(captureSingleEvent().sent());
	}

	@Test
	void web_transfer_other_conflict_409_is_treated_as_already_present() throws Exception {
		DicomStowRS stow = mock(DicomStowRS.class);
		doThrow(new HttpException("conflict", 409, (Throwable) null)).when(stow)
			.uploadDicom(any(Attributes.class), anyString());
		WebForwardDestination dest = webDestination(stow, List.of());

		// 409 means the object is already stored: success, no exception.
		forwardService.transferOther(fwdNode, dest, dataset(), params(null));

		assertTrue(captureSingleEvent().sent());
	}

	@Test
	void web_transfer_other_file_exception_is_swallowed() throws Exception {
		DicomStowRS stow = mock(DicomStowRS.class);
		AttributeEditor blocker = (dcm, ctx) -> ctx.setAbort(Abort.FILE_EXCEPTION, "blocked");
		WebForwardDestination dest = webDestination(stow, List.of(blocker));

		forwardService.transferOther(fwdNode, dest, dataset(), params(null));

		verify(stow, never()).uploadDicom(any(Attributes.class), anyString());
		MonitoringEntry event = captureSingleEvent();
		assertFalse(event.sent());
	}

	// --- DICOMWeb transfer (first destination, parses the incoming stream)
	// ------------------

	@Test
	void web_transfer_fast_path_uploads_stream_and_monitors_success() throws Exception {
		DicomStowRS stow = mock(DicomStowRS.class);
		WebForwardDestination dest = webDestination(stow, List.of());
		Params p = new Params(IUID, CUID, TS, 0, new ByteArrayInputStream(serialize(dataset())), null);

		forwardService.transfer(fwdNode, dest, null, p);

		verify(stow).uploadDicom(any(InputStream.class), any(Attributes.class));
		assertTrue(captureSingleEvent().sent());
	}

	// --- Virtual (report-only) destination
	// ---------------------------------------------------

	@Test
	void virtual_transfer_discards_dicom_and_monitors_success() throws Exception {
		NullForwardDestination dest = nullDestination(List.of());
		Params p = new Params(IUID, CUID, TS, 0, new ByteArrayInputStream(serializeRaw(dataset())), null);

		forwardService.transferVirtual(fwdNode, dest, null, p);

		MonitoringEntry event = captureSingleEvent();
		assertTrue(event.sent());
		assertFalse(event.error());
	}

	@Test
	void virtual_transfer_applies_editors_before_reporting() throws Exception {
		AttributeEditor anonymizer = (dcm, ctx) -> dcm.setString(Tag.PatientID, VR.LO, "ANON");
		NullForwardDestination dest = nullDestination(List.of(anonymizer));
		Params p = new Params(IUID, CUID, TS, 0, new ByteArrayInputStream(serializeRaw(dataset())), null);

		forwardService.transferVirtual(fwdNode, dest, null, p);

		assertTrue(captureSingleEvent().sent());
	}

	@Test
	void virtual_transfer_file_exception_is_swallowed_and_nothing_sent() throws Exception {
		AttributeEditor blocker = (dcm, ctx) -> ctx.setAbort(Abort.FILE_EXCEPTION, "blocked");
		NullForwardDestination dest = nullDestination(List.of(blocker));
		Params p = new Params(IUID, CUID, TS, 0, new ByteArrayInputStream(serializeRaw(dataset())), null);

		forwardService.transferVirtual(fwdNode, dest, null, p);

		MonitoringEntry event = captureSingleEvent();
		assertFalse(event.sent());
		assertEquals("blocked", event.reason());
	}

	@Test
	void virtual_transfer_publishes_conformance_event_when_report_enabled() throws Exception {
		NullForwardDestination dest = nullDestination(List.of());
		dest.setBuildConformanceReport(true);
		Params p = new Params(IUID, CUID, TS, 0, new ByteArrayInputStream(serializeRaw(dataset())), null);

		forwardService.transferVirtual(fwdNode, dest, null, p);

		ArgumentCaptor<ApplicationEvent> captor = ArgumentCaptor.forClass(ApplicationEvent.class);
		verify(publisher, times(2)).publishEvent(captor.capture());
		assertTrue(captor.getAllValues().stream().anyMatch(ConformanceCollectEvent.class::isInstance));
	}

	// --- storeMultipleDestination fan-out
	// ---------------------------------------------------

	@Test
	void store_multiple_destination_fans_out_to_every_destination() throws Exception {
		DicomStowRS stow1 = mock(DicomStowRS.class);
		DicomStowRS stow2 = mock(DicomStowRS.class);
		WebForwardDestination dest1 = webDestination(stow1, List.of());
		WebForwardDestination dest2 = webDestination(stow2, List.of());
		// With >1 destination the first one takes the editor path, which reads a raw
		// dataset.
		Params p = new Params(IUID, CUID, TS, 0, new ByteArrayInputStream(serializeRaw(dataset())), null);

		forwardService.storeMultipleDestination(fwdNode, List.of(dest1, dest2), p);

		// First destination parses the stream; the second reuses the shared copy.
		verify(stow1).uploadDicom(any(Attributes.class), anyString());
		verify(stow2).uploadDicom(any(Attributes.class), anyString());
		ArgumentCaptor<TransferMonitoringEvent> captor = ArgumentCaptor.forClass(TransferMonitoringEvent.class);
		verify(publisher, times(2)).publishEvent(captor.capture());
		assertTrue(captor.getAllValues()
			.stream()
			.map(e -> (MonitoringEntry) e.getSource())
			.allMatch(MonitoringEntry::sent));
	}

	// --- fixtures & helpers
	// -----------------------------------------------------------------

	private static Attributes dataset() {
		Attributes a = new Attributes();
		a.setString(Tag.SOPClassUID, VR.UI, CUID);
		a.setString(Tag.SOPInstanceUID, VR.UI, IUID);
		a.setString(Tag.PatientID, VR.LO, "PID-1");
		a.setString(Tag.Modality, VR.CS, "OT");
		return a;
	}

	/**
	 * Full stream (preamble + file meta information), consumed by the no-editor fast
	 * path.
	 */
	private static byte[] serialize(Attributes data) throws IOException {
		Attributes fmi = data.createFileMetaInformation(TS);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (DicomOutputStream dos = new DicomOutputStream(baos, UID.ExplicitVRLittleEndian)) {
			dos.writeDataset(fmi, data);
		}
		return baos.toByteArray();
	}

	/** Raw dataset (no preamble / file meta information), consumed by the editor path. */
	private static byte[] serializeRaw(Attributes data) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (DicomOutputStream dos = new DicomOutputStream(baos, TS)) {
			dos.writeDataset(null, data);
		}
		return baos.toByteArray();
	}

	private static Params params(InputStream data) {
		return new Params(IUID, CUID, TS, 0, data, null);
	}

	private static StoreFromStreamSCU readyStreamSCU() {
		StoreFromStreamSCU scu = mock(StoreFromStreamSCU.class);
		when(scu.isReadyForDataTransfer()).thenReturn(true);
		when(scu.selectTransferSyntax(anyString(), anyString())).thenReturn(TS);
		when(scu.getRemoteDicomNode()).thenReturn(new DicomNode("DEST", "localhost", 104));
		when(scu.getNumberOfSuboperations()).thenReturn(0);
		when(scu.getState()).thenReturn(new DicomState(new DicomProgress()));
		return scu;
	}

	/** A lease over the given SCU, as the destination pool would hand out. */
	private static DicomForwardDestination.ScuLease lease(StoreFromStreamSCU scu) {
		return new DicomForwardDestination.ScuLease(scu, null, 0, true);
	}

	private static DicomForwardDestination dicomDestination(StoreFromStreamSCU scu, List<AttributeEditor> editors) {
		DicomForwardDestination dest = mock(DicomForwardDestination.class);
		when(dest.getStreamSCU()).thenReturn(scu);
		when(dest.getDicomEditors()).thenReturn(editors);
		when(dest.getOutputTransferSyntax(anyString())).thenReturn(TS);
		when(dest.getState()).thenReturn(new DicomState(new DicomProgress()));
		when(dest.getId()).thenReturn(2L);
		return dest;
	}

	private WebForwardDestination webDestination(DicomStowRS stow, List<AttributeEditor> editors) {
		return new WebForwardDestination(2L, fwdNode, stow, null, editors);
	}

	private NullForwardDestination nullDestination(List<AttributeEditor> editors) {
		return new NullForwardDestination(2L, fwdNode, editors);
	}

	private MonitoringEntry captureSingleEvent() {
		ArgumentCaptor<TransferMonitoringEvent> captor = ArgumentCaptor.forClass(TransferMonitoringEvent.class);
		verify(publisher).publishEvent(captor.capture());
		return (MonitoringEntry) captor.getValue().getSource();
	}

}