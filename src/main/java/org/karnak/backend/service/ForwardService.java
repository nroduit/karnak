/*
 * Copyright (c) 2009-2019 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.img.op.MaskArea;
import org.dcm4che3.img.stream.BytesWithImageDescriptor;
import org.dcm4che3.img.stream.ImageAdapter;
import org.dcm4che3.img.stream.ImageAdapter.AdaptTransferSyntax;
import org.dcm4che3.img.util.Editable;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomInputStream.IncludeBulkData;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.DataWriter;
import org.dcm4che3.net.DataWriterAdapter;
import org.dcm4che3.net.InputStreamDataWriter;
import org.dcm4che3.net.PDVInputStream;
import org.dcm4che3.net.Status;
import org.karnak.backend.data.entity.TransferStatusEntity;
import org.karnak.backend.dicom.Defacer;
import org.karnak.backend.dicom.DicomForwardDestination;
import org.karnak.backend.dicom.ForwardDestination;
import org.karnak.backend.dicom.ForwardDicomNode;
import org.karnak.backend.dicom.Params;
import org.karnak.backend.dicom.WebForwardDestination;
import org.karnak.backend.exception.AbortException;
import org.karnak.backend.model.event.TransferMonitoringEvent;
import org.karnak.backend.model.image.TransformedPlanarImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.weasis.core.util.FileUtil;
import org.weasis.core.util.LangUtil;
import org.weasis.dicom.param.AttributeEditor;
import org.weasis.dicom.param.AttributeEditorContext;
import org.weasis.dicom.param.AttributeEditorContext.Abort;
import org.weasis.dicom.util.ServiceUtil;
import org.weasis.dicom.util.ServiceUtil.ProgressStatus;
import org.weasis.dicom.util.StoreFromStreamSCU;
import org.weasis.dicom.web.DicomStowRS;
import org.weasis.dicom.web.HttpException;
import org.weasis.opencv.data.PlanarImage;

@Service
@Slf4j
public class ForwardService {

	private static final String ERROR_WHEN_FORWARDING = "Error when forwarding to the final destination";

	private final ApplicationEventPublisher applicationEventPublisher;

	@Autowired
	public ForwardService(final ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	public void storeMultipleDestination(ForwardDicomNode forwardNode, List<ForwardDestination> destinations, Params p)
			throws IOException {
		if (destinations == null || destinations.isEmpty()) {
			throw new IllegalStateException("Cannot find destinations from " + forwardNode.toString());
		}
		// Exclude DICOMDIR
		if ("1.2.840.10008.1.3.10".equals(p.getCuid())) {
			log.warn("Cannot send DICOMDIR {}", p.getIuid());
			return;
		}

		// Prepare and transfer files
		Attributes attributes = new Attributes();
		List<File> files = new ArrayList<>();
		try {
			int nbDestinations = destinations.size();
			for (int i = 0; i < nbDestinations; i++) {
				prepareAndTransfer(forwardNode, p, i, destinations.get(i), attributes, nbDestinations, files);
			}
		}
		catch (IOException e) {
			log.error("Cannot connect to the final destination", e);
			throw e;
		}
		finally {
			if (!files.isEmpty()) {
				// Force to clean if tmp bulk files
				for (File file : files) {
					FileUtil.delete(file);
				}
			}
		}
	}

	/**
	 * Prepare and transfer files
	 * @param fwdNode Forward node
	 * @param p Params
	 * @param index Current index
	 * @param destination Destination
	 * @param attributes Attributes
	 * @param nbDestinations Number of destinations to handle
	 * @param files Temp files to delete
	 */
	private void prepareAndTransfer(ForwardDicomNode fwdNode, Params p, int index, ForwardDestination destination,
			Attributes attributes, int nbDestinations, List<File> files) throws IOException {
		if (destination instanceof DicomForwardDestination) {
			// Prepare transfer only for dicom destination
			prepareTransfer((DicomForwardDestination) destination, p);
		}
		if (index == 0) {
			// Case first iteration: handle first destination of the forward node
			Attributes attToApply = nbDestinations > 1 ? attributes : null;
			if (destination instanceof DicomForwardDestination) {
				List<File> list = transfer(fwdNode, (DicomForwardDestination) destination, attToApply, p);
				if (list != null) {
					files.addAll(list);
				}
			}
			else if (destination instanceof WebForwardDestination) {
				List<File> list = transfer(fwdNode, (WebForwardDestination) destination, attToApply, p);
				if (list != null) {
					files.addAll(list);
				}
			}
		}
		else {
			// Case other iterations: handle other destinations of the forward node
			if (!attributes.isEmpty()) {
				if (destination instanceof DicomForwardDestination) {
					transferOther(fwdNode, (DicomForwardDestination) destination, attributes, p);
				}
				else if (destination instanceof WebForwardDestination) {
					transferOther(fwdNode, (WebForwardDestination) destination, attributes, p);
				}
			}
		}
	}

	public static StoreFromStreamSCU prepareTransfer(DicomForwardDestination destination, Params p) throws IOException {
		String cuid = p.getCuid();
		String tsuid = p.getTsuid();
		String dstTsuid = destination.getOutputTransferSyntax(tsuid);
		StoreFromStreamSCU streamSCU = destination.getStreamSCU();
		streamSCU.prepareTransfer(destination.getStreamSCUService(), p.getIuid(), cuid, dstTsuid);
		return streamSCU;
	}

	public List<File> transfer(ForwardDicomNode sourceNode, DicomForwardDestination destination, Attributes copy,
			Params p) throws IOException {
		StoreFromStreamSCU streamSCU = destination.getStreamSCU();
		DicomInputStream in = null;
		List<File> files;
		Attributes attributesOriginal = new Attributes();
		Attributes attributesToSend = new Attributes();
		try {
			if (!streamSCU.isReadyForDataTransfer()) {
				throw new IllegalStateException("Association not ready for transfer.");
			}
			DataWriter dataWriter;
			String cuid = p.getCuid();
			String iuid = p.getIuid();
			String tsuid = p.getTsuid();
			var syntax = new AdaptTransferSyntax(tsuid,
					streamSCU.selectTransferSyntax(cuid, destination.getOutputTransferSyntax(tsuid)));
			List<AttributeEditor> editors = destination.getDicomEditors();
			TransformedPlanarImage transformedPlanarImage = new TransformedPlanarImage();
			if (copy == null && editors.isEmpty() && syntax.getRequested().equals(tsuid)) {
				dataWriter = new InputStreamDataWriter(p.getData());
				attributesToSend = new DicomInputStream(p.getData()).readDataset();
				attributesOriginal.addAll(attributesToSend);
			}
			else {
				AttributeEditorContext context = new AttributeEditorContext(tsuid, sourceNode,
						streamSCU.getRemoteDicomNode());
				in = new DicomInputStream(p.getData(), tsuid);
				in.setIncludeBulkData(IncludeBulkData.URI);
				Attributes attributes = in.readDataset();
				attributesOriginal.addAll(attributes);
				attributesToSend = attributes;
				if (copy != null) {
					copy.addAll(attributes);
				}

				if (!editors.isEmpty()) {
					editors.forEach(e -> e.apply(attributes, context));
					iuid = attributes.getString(Tag.SOPInstanceUID);
					cuid = attributes.getString(Tag.SOPClassUID);
				}

				if (context.getAbort() == Abort.FILE_EXCEPTION) {
					if (p.getData() instanceof PDVInputStream) {
						((PDVInputStream) p.getData()).skipAll();
					}
					throw new AbortException(context.getAbort(), context.getAbortMessage());
				}
				else if (context.getAbort() == Abort.CONNECTION_EXCEPTION) {
					if (p.getAs() != null) {
						p.getAs().abort();
					}
					throw new AbortException(context.getAbort(),
							"DICOM association abort: " + context.getAbortMessage());
				}
				dataWriter = buildDataWriterFromTransformedImage(syntax, context, attributes, transformedPlanarImage);
			}

			launchCStore(p, streamSCU, dataWriter, cuid, iuid, syntax, transformedPlanarImage);

			progressNotify(destination, p.getIuid(), p.getCuid(), false, streamSCU);
			monitor(sourceNode.getId(), destination.getId(), attributesOriginal, attributesToSend, true, null,
					attributesOriginal.getString(Tag.Modality), p.getCuid());
		}
		catch (AbortException e) {
			progressNotify(destination, p.getIuid(), p.getCuid(), true, streamSCU);
			monitor(sourceNode.getId(), destination.getId(), attributesOriginal, attributesToSend, false,
					e.getMessage(), attributesOriginal.getString(Tag.Modality), p.getCuid());
			if (e.getAbort() == Abort.CONNECTION_EXCEPTION) {
				throw e;
			}
		}
		catch (IOException e) {
			progressNotify(destination, p.getIuid(), p.getCuid(), true, streamSCU);
			monitor(sourceNode.getId(), destination.getId(), attributesOriginal, attributesToSend, false,
					e.getMessage(), attributesOriginal.getString(Tag.Modality), p.getCuid());
			throw e;
		}
		catch (Exception e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			progressNotify(destination, p.getIuid(), p.getCuid(), true, streamSCU);
			monitor(sourceNode.getId(), destination.getId(), attributesOriginal, attributesToSend, false,
					e.getMessage(), attributesOriginal.getString(Tag.Modality), p.getCuid());
			log.error(ERROR_WHEN_FORWARDING, e);
		}
		finally {
			streamSCU.triggerCloseExecutor();
			files = cleanOrGetBulkDataFiles(in, copy == null);
		}
		return files;
	}

	private void launchCStore(Params p, StoreFromStreamSCU streamSCU, DataWriter dataWriter, String cuid, String iuid,
			AdaptTransferSyntax syntax, TransformedPlanarImage transformedPlanarImage)
			throws IOException, InterruptedException {
		try {
			streamSCU.cstore(cuid, iuid, p.getPriority(), dataWriter, syntax.getSuitable());
		}
		finally {
			if (transformedPlanarImage != null && transformedPlanarImage.getPlanarImage() != null) {
				transformedPlanarImage.getPlanarImage().release();
			}
		}
	}

	private DataWriter buildDataWriterFromTransformedImage(AdaptTransferSyntax syntax, AttributeEditorContext context,
			Attributes attributes, TransformedPlanarImage transformedPlanarImage) throws IOException {
		DataWriter dataWriter;
		BytesWithImageDescriptor desc = ImageAdapter.imageTranscode(attributes, syntax, context);
		transformedPlanarImage = transformImage(attributes, context, transformedPlanarImage);
		dataWriter = ImageAdapter.buildDataWriter(attributes, syntax,
				transformedPlanarImage != null ? transformedPlanarImage.getEditablePlanarImage() : null, desc);
		return dataWriter;
	}

	private static TransformedPlanarImage transformImage(Attributes attributes, AttributeEditorContext context,
			TransformedPlanarImage transformedPlanarImage) {
		MaskArea m = context.getMaskArea();
		boolean defacing = LangUtil.getEmptytoFalse(context.getProperties().getProperty(Defacer.APPLY_DEFACING));
		if (m != null || defacing) {
			Editable<PlanarImage> editablePlanarImage = buildEditablePlanarImage(attributes, m, defacing,
					transformedPlanarImage);
			transformedPlanarImage.setEditablePlanarImage(editablePlanarImage);
			return transformedPlanarImage;
		}
		return null;
	}

	private static Editable<PlanarImage> buildEditablePlanarImage(Attributes attributes, MaskArea m, boolean defacing,
			TransformedPlanarImage transformedPlanarImage) {
		return img -> {
			PlanarImage planarImage = buildPlanarImage(attributes, m, defacing, img);
			transformedPlanarImage.setPlanarImage(planarImage);
			return planarImage;
		};
	}

	private static PlanarImage buildPlanarImage(Attributes attributes, MaskArea m, boolean defacing, PlanarImage img) {
		PlanarImage image = img;
		if (defacing) {
			image = Defacer.apply(attributes, image);
		}
		if (m != null) {
			image = MaskArea.drawShape(image.toMat(), m);
		}
		return image;
	}

	private static List<File> cleanOrGetBulkDataFiles(DicomInputStream in, boolean clean) {
		FileUtil.safeClose(in);
		if (clean) {
			// Force to clean if tmp bulk files
			ServiceUtil.safeClose(in);
		}
		else if (in != null) {
			// Return tmp bulk files
			return in.getBulkDataFiles();
		}
		return null;
	}

	public void transferOther(ForwardDicomNode fwdNode, DicomForwardDestination destination, Attributes copy, Params p)
			throws IOException {
		StoreFromStreamSCU streamSCU = destination.getStreamSCU();
		Attributes attributesToSend = new Attributes();
		Attributes attributesOriginal = new Attributes();
		try {
			if (!streamSCU.isReadyForDataTransfer()) {
				throw new IllegalStateException("Association not ready for transfer.");
			}

			DataWriter dataWriter;
			String tsuid = p.getTsuid();
			String iuid = p.getIuid();
			String cuid = p.getCuid();
			var syntax = new AdaptTransferSyntax(tsuid,
					streamSCU.selectTransferSyntax(cuid, destination.getOutputTransferSyntax(tsuid)));
			List<AttributeEditor> editors = destination.getDicomEditors();
			TransformedPlanarImage transformedPlanarImage = new TransformedPlanarImage();
			if (editors.isEmpty() && syntax.getRequested().equals(tsuid)) {
				dataWriter = new DataWriterAdapter(copy);
				attributesOriginal.addAll(copy);
				attributesToSend = copy;
			}
			else {
				AttributeEditorContext context = new AttributeEditorContext(tsuid, fwdNode,
						streamSCU.getRemoteDicomNode());
				Attributes attributes = new Attributes(copy);
				attributesOriginal.addAll(attributes);
				attributesToSend = attributes;
				if (!editors.isEmpty()) {
					editors.forEach(e -> e.apply(attributes, context));
					iuid = attributes.getString(Tag.SOPInstanceUID);
					cuid = attributes.getString(Tag.SOPClassUID);
				}

				if (context.getAbort() == Abort.FILE_EXCEPTION) {
					throw new AbortException(context.getAbort(), context.getAbortMessage());
				}
				else if (context.getAbort() == Abort.CONNECTION_EXCEPTION) {
					throw new AbortException(context.getAbort(),
							"DICOM association abort. " + context.getAbortMessage());
				}
				dataWriter = buildDataWriterFromTransformedImage(syntax, context, attributes, transformedPlanarImage);
			}

			launchCStore(p, streamSCU, dataWriter, cuid, iuid, syntax, transformedPlanarImage);

			progressNotify(destination, p.getIuid(), p.getCuid(), false, streamSCU);
			monitor(fwdNode.getId(), destination.getId(), attributesOriginal, attributesToSend, true, null,
					attributesOriginal.getString(Tag.Modality), p.getCuid());
		}
		catch (AbortException e) {
			progressNotify(destination, p.getIuid(), p.getCuid(), true, streamSCU);
			monitor(fwdNode.getId(), destination.getId(), attributesOriginal, attributesToSend, false, e.getMessage(),
					attributesOriginal.getString(Tag.Modality), p.getCuid());
			if (e.getAbort() == Abort.CONNECTION_EXCEPTION) {
				throw e;
			}
		}
		catch (IOException e) {
			progressNotify(destination, p.getIuid(), p.getCuid(), true, streamSCU);
			monitor(fwdNode.getId(), destination.getId(), attributesOriginal, attributesToSend, false, e.getMessage(),
					attributesOriginal.getString(Tag.Modality), p.getCuid());
			throw e;
		}
		catch (Exception e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			progressNotify(destination, p.getIuid(), p.getCuid(), true, streamSCU);
			monitor(fwdNode.getId(), destination.getId(), attributesOriginal, attributesToSend, false, e.getMessage(),
					attributesOriginal.getString(Tag.Modality), p.getCuid());
			log.error(ERROR_WHEN_FORWARDING, e);
		}
		finally {
			streamSCU.triggerCloseExecutor();
		}
	}

	public List<File> transfer(ForwardDicomNode fwdNode, WebForwardDestination destination, Attributes copy, Params p)
			throws IOException {
		DicomInputStream in = null;
		List<File> files;
		Attributes attributesToSend = new Attributes();
		Attributes attributesOriginal = new Attributes();
		try {
			List<AttributeEditor> editors = destination.getDicomEditors();
			DicomStowRS stow = destination.getStowrsSingleFile();
			var syntax = new AdaptTransferSyntax(p.getTsuid(), destination.getOutputTransferSyntax(p.getTsuid()));

			if (syntax.getRequested().equals(p.getTsuid()) && copy == null && editors.isEmpty()) {
				Attributes fmi = Attributes.createFileMetaInformation(p.getIuid(), p.getCuid(), syntax.getRequested());
				try (InputStream stream = p.getData()) {
					attributesToSend = new DicomInputStream(p.getData()).readDataset();
					attributesOriginal.addAll(attributesToSend);
					stow.uploadDicom(stream, fmi);
				}
				catch (HttpException httpException) {
					if (httpException.getStatusCode() != 409) {
						throw new AbortException(Abort.FILE_EXCEPTION, httpException.getMessage());
					}
					else {
						log.debug("File already present in destination");
					}
				}
			}
			else {
				AttributeEditorContext context = new AttributeEditorContext(p.getTsuid(), fwdNode, null);
				in = new DicomInputStream(p.getData(), p.getTsuid());
				in.setIncludeBulkData(IncludeBulkData.URI);
				Attributes attributes = in.readDataset();
				attributesToSend = attributes;
				attributesOriginal.addAll(attributes);
				if (copy != null) {
					copy.addAll(attributes);
				}
				if (!editors.isEmpty()) {
					editors.forEach(e -> e.apply(attributes, context));
				}

				if (context.getAbort() == Abort.FILE_EXCEPTION) {
					if (p.getData() instanceof PDVInputStream) {
						((PDVInputStream) p.getData()).skipAll();
					}
					throw new AbortException(context.getAbort(), context.getAbortMessage());
				}
				else if (context.getAbort() == Abort.CONNECTION_EXCEPTION) {
					if (p.getAs() != null) {
						p.getAs().abort();
					}
					throw new AbortException(context.getAbort(), "STOW-RS abort: " + context.getAbortMessage());
				}

				BytesWithImageDescriptor desc = ImageAdapter.imageTranscode(attributes, syntax, context);
				if (desc == null) {
					stow.uploadDicom(attributes, syntax.getOriginal());
				}
				else {
					uploadPayLoadFromTransformedImage(stow, syntax, context, attributes, desc);
				}
			}
			progressNotify(destination, p.getIuid(), p.getCuid(), false, 0);
			monitor(fwdNode.getId(), destination.getId(), attributesOriginal, attributesToSend, true, null,
					attributesOriginal.getString(Tag.Modality), p.getCuid());
		}
		catch (AbortException e) {
			progressNotify(destination, p.getIuid(), p.getCuid(), true, 0);
			monitor(fwdNode.getId(), destination.getId(), attributesOriginal, attributesToSend, false, e.getMessage(),
					attributesOriginal.getString(Tag.Modality), p.getCuid());
			if (e.getAbort() == Abort.CONNECTION_EXCEPTION) {
				throw e;
			}
		}
		catch (IOException e) {
			progressNotify(destination, p.getIuid(), p.getCuid(), true, 0);
			monitor(fwdNode.getId(), destination.getId(), attributesOriginal, attributesToSend, false, e.getMessage(),
					attributesOriginal.getString(Tag.Modality), p.getCuid());
			throw e;
		}
		catch (Exception e) {
			progressNotify(destination, p.getIuid(), p.getCuid(), true, 0);
			monitor(fwdNode.getId(), destination.getId(), attributesOriginal, attributesToSend, false, e.getMessage(),
					attributesOriginal.getString(Tag.Modality), p.getCuid());
			log.error(ERROR_WHEN_FORWARDING, e);
		}
		finally {
			files = cleanOrGetBulkDataFiles(in, copy == null);
		}
		return files;
	}

	private void uploadPayLoadFromTransformedImage(DicomStowRS stow, AdaptTransferSyntax syntax,
			AttributeEditorContext context, Attributes attributes, BytesWithImageDescriptor desc) throws Exception {
		TransformedPlanarImage transformedPlanarImage = new TransformedPlanarImage();
		try {
			transformedPlanarImage = transformImage(attributes, context, transformedPlanarImage);
			stow.uploadPayload(ImageAdapter.preparePlayload(attributes, syntax, desc,
					transformedPlanarImage != null ? transformedPlanarImage.getEditablePlanarImage() : null));
		}
		finally {
			if (transformedPlanarImage != null && transformedPlanarImage.getPlanarImage() != null) {
				transformedPlanarImage.getPlanarImage().release();
			}
		}
	}

	public void transferOther(ForwardDicomNode fwdNode, WebForwardDestination destination, Attributes copy, Params p)
			throws IOException {
		Attributes attributesToSend = new Attributes();
		Attributes attributesOriginal = new Attributes();
		try {
			List<AttributeEditor> editors = destination.getDicomEditors();
			DicomStowRS stow = destination.getStowrsSingleFile();
			var syntax = new AdaptTransferSyntax(p.getTsuid(), destination.getOutputTransferSyntax(p.getTsuid()));
			if (syntax.getRequested().equals(p.getTsuid()) && editors.isEmpty()) {
				attributesToSend = copy;
				attributesOriginal.addAll(copy);
				stow.uploadDicom(copy, syntax.getRequested());
			}
			else {
				AttributeEditorContext context = new AttributeEditorContext(p.getTsuid(), fwdNode, null);
				Attributes attributes = new Attributes(copy);
				attributesToSend = attributes;
				attributesOriginal.addAll(attributes);
				editors.forEach(e -> e.apply(attributes, context));

				if (context.getAbort() == Abort.FILE_EXCEPTION) {
					throw new AbortException(context.getAbort(), context.getAbortMessage());
				}
				else if (context.getAbort() == Abort.CONNECTION_EXCEPTION) {
					throw new AbortException(context.getAbort(),
							"DICOM associtation abort. " + context.getAbortMessage());
				}

				BytesWithImageDescriptor desc = ImageAdapter.imageTranscode(attributes, syntax, context);
				if (desc == null) {
					stow.uploadDicom(attributes, syntax.getOriginal());
				}
				else {
					uploadPayLoadFromTransformedImage(stow, syntax, context, attributes, desc);
				}
				progressNotify(destination, p.getIuid(), p.getCuid(), false, 0);
				monitor(fwdNode.getId(), destination.getId(), attributesOriginal, attributesToSend, true, null,
						attributesOriginal.getString(Tag.Modality), p.getCuid());
			}
		}
		catch (HttpException httpException) {
			if (httpException.getStatusCode() != 409) {
				progressNotify(destination, p.getIuid(), p.getCuid(), true, 0);
				monitor(fwdNode.getId(), destination.getId(), attributesOriginal, attributesToSend, false,
						httpException.getMessage(), attributesOriginal.getString(Tag.Modality), p.getCuid());
				throw new AbortException(Abort.FILE_EXCEPTION, "DICOMWeb forward", httpException);
			}
			else {
				progressNotify(destination, p.getIuid(), p.getCuid(), false, 0);
				monitor(fwdNode.getId(), destination.getId(), attributesOriginal, attributesToSend, true, null,
						attributesOriginal.getString(Tag.Modality), p.getCuid());
				log.debug("File already present in destination");
			}
		}
		catch (AbortException e) {
			progressNotify(destination, p.getIuid(), p.getCuid(), true, 0);
			monitor(fwdNode.getId(), destination.getId(), attributesOriginal, attributesToSend, false, e.getMessage(),
					attributesOriginal.getString(Tag.Modality), p.getCuid());
			if (e.getAbort() == Abort.CONNECTION_EXCEPTION) {
				throw e;
			}
		}
		catch (IOException e) {
			progressNotify(destination, p.getIuid(), p.getCuid(), true, 0);
			monitor(fwdNode.getId(), destination.getId(), attributesOriginal, attributesToSend, false, e.getMessage(),
					attributesOriginal.getString(Tag.Modality), p.getCuid());
			throw e;
		}
		catch (Exception e) {
			progressNotify(destination, p.getIuid(), p.getCuid(), true, 0);
			monitor(fwdNode.getId(), destination.getId(), attributesOriginal, attributesToSend, false, e.getMessage(),
					attributesOriginal.getString(Tag.Modality), p.getCuid());
			log.error(ERROR_WHEN_FORWARDING, e);
		}
	}

	private static void progressNotify(ForwardDestination destination, String iuid, String cuid, boolean failed,
			StoreFromStreamSCU streamSCU) {
		streamSCU.removeIUIDProcessed(iuid);
		ServiceUtil.notifyProgession(destination.getState(), iuid, cuid,
				failed ? Status.ProcessingFailure : Status.Success,
				failed ? ProgressStatus.FAILED : ProgressStatus.COMPLETED, streamSCU.getNumberOfSuboperations());
	}

	private static void progressNotify(ForwardDestination destination, String iuid, String cuid, boolean failed,
			int subOperations) {
		ServiceUtil.notifyProgession(destination.getState(), iuid, cuid,
				failed ? Status.ProcessingFailure : Status.Success,
				failed ? ProgressStatus.FAILED : ProgressStatus.COMPLETED, subOperations);
	}

	public static String selectTransferSyntax(Association as, String cuid, String filets) {
		Set<String> tss = as.getTransferSyntaxesFor(cuid);
		if (tss.contains(filets)) {
			return filets;
		}

		if (tss.contains(UID.ExplicitVRLittleEndian)) {
			return UID.ExplicitVRLittleEndian;
		}

		return UID.ImplicitVRLittleEndian;
	}

	/**
	 * Publish an event for monitoring purpose
	 * @param forwardNodeId ForwardNode Id
	 * @param destinationId Destination Id
	 * @param attributesOriginal Original value
	 * @param attributesToSend De-identify value
	 * @param sent Flag to know if the transfer occurred
	 * @param reason Reason of not transferring the file
	 * @param modality Modality of the file transferred
	 * @param sopClassUid Sop Class Uid
	 */
	private void monitor(Long forwardNodeId, Long destinationId, Attributes attributesOriginal,
			Attributes attributesToSend, boolean sent, String reason, String modality, String sopClassUid) {
		applicationEventPublisher
			.publishEvent(new TransferMonitoringEvent(TransferStatusEntity.buildTransferStatusEntity(forwardNodeId,
					destinationId, attributesOriginal, attributesToSend, sent, reason, modality, sopClassUid)));
	}

}
