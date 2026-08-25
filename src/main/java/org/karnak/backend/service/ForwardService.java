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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
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
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.dicom.Defacer;
import org.karnak.backend.dicom.DicomForwardDestination;
import org.karnak.backend.dicom.DicomForwardDestination.ScuLease;
import org.karnak.backend.dicom.ForwardDestination;
import org.karnak.backend.dicom.ForwardDicomNode;
import org.karnak.backend.dicom.Params;
import org.karnak.backend.dicom.WebForwardDestination;
import org.karnak.backend.exception.AbortException;
import org.karnak.backend.model.event.ConformanceCollectEvent;
import org.karnak.backend.model.event.TransferMonitoringEvent;
import org.karnak.backend.model.image.TransformedPlanarImage;
import org.karnak.backend.model.monitoring.MonitoringEntry;
import org.karnak.backend.model.profilepipe.SensitiveTagDefinition;
import org.karnak.backend.model.validation.ImageIdentityCheckInput;
import org.karnak.backend.model.validation.InstanceConformanceData;
import org.karnak.backend.model.validation.MetadataSnapshot;
import org.karnak.backend.service.profilepipe.DeidentifyImageService;
import org.karnak.backend.service.profilepipe.Profile;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.weasis.core.util.FileUtil;
import org.weasis.core.util.LangUtil;
import org.weasis.core.util.StreamUtil;
import org.weasis.dicom.param.AttributeEditor;
import org.weasis.dicom.param.AttributeEditorContext;
import org.weasis.dicom.param.AttributeEditorContext.Abort;
import org.weasis.dicom.util.ServiceUtil;
import org.weasis.dicom.util.ServiceUtil.ProgressStatus;
import org.weasis.dicom.util.StoreFromStreamSCU;
import org.weasis.dicom.web.DicomStowRS;
import org.weasis.dicom.web.HttpException;
import org.weasis.opencv.data.PlanarImage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@NullUnmarked
public class ForwardService {

	private static final String ERROR_WHEN_FORWARDING = "Error when forwarding to the final destination";

	private final ApplicationEventPublisher applicationEventPublisher;

	private final DeidentifyImageService deidentifyImageService;

	@Value("${forward.parallel-fanout:true}")
	private boolean parallelFanout;

	@Value("${forward.fanout-max-threads:0}")
	private int fanoutMaxThreads;

	// Sequence depth the conformance snapshot is captured to when deep-sequence
	// validation is enabled on the destination; must match the validator's recursion
	// depth
	@Value("${conformance-report.max-sequence-depth:8}")
	private int conformanceMaxSequenceDepth;

	private ExecutorService fanoutExecutor;

	@Autowired
	public ForwardService(final ApplicationEventPublisher applicationEventPublisher,
			final DeidentifyImageService deidentifyImageService) {
		this.applicationEventPublisher = applicationEventPublisher;
		this.deidentifyImageService = deidentifyImageService;
	}

	@PostConstruct
	void initFanoutExecutor() {
		if (!parallelFanout) {
			return;
		}
		int threads = fanoutMaxThreads > 0 ? fanoutMaxThreads : Math.max(4, Runtime.getRuntime().availableProcessors());
		ThreadFactory threadFactory = new ThreadFactory() {
			private final AtomicInteger counter = new AtomicInteger(1);

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "forward-fanout-" + counter.getAndIncrement());
				t.setDaemon(true);
				return t;
			}
		};
		// Bounded queue + caller-runs: under overload the submitting thread runs the task
		// itself, degrading gracefully to sequential delivery rather than growing
		// threads/queue
		// without bound.
		ThreadPoolExecutor executor = new ThreadPoolExecutor(threads, threads, 60L, TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(threads * 4), threadFactory, new ThreadPoolExecutor.CallerRunsPolicy());
		executor.allowCoreThreadTimeOut(true);
		this.fanoutExecutor = executor;
	}

	@PreDestroy
	void shutdownFanoutExecutor() {
		if (fanoutExecutor != null) {
			fanoutExecutor.shutdown();
			try {
				if (!fanoutExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
					fanoutExecutor.shutdownNow();
				}
			}
			catch (InterruptedException e) {
				fanoutExecutor.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
	}

	public void storeMultipleDestination(ForwardDicomNode forwardNode, List<ForwardDestination> destinations, Params p)
			throws IOException {
		if (destinations == null || destinations.isEmpty()) {
			throw new IllegalStateException("Cannot find destinations from " + forwardNode.toString());
		}
		// Exclude DICOMDIR
		if ("1.2.840.10008.1.3.10".equals(p.cuid())) {
			log.warn("Cannot send DICOMDIR {}", p.iuid());
			return;
		}

		// Prepare and transfer files
		Attributes attributes = new Attributes();
		List<File> files = new ArrayList<>();
		try {
			int nbDestinations = destinations.size();
			// The first destination consumes the incoming stream once: for a single
			// destination it streams straight through; for several it also buffers the
			// dataset (bulk
			// data spooled to temporary files) into "attributes" so the remaining
			// destinations can be
			// served from it.
			prepareAndTransfer(forwardNode, p, 0, destinations.get(0), attributes, nbDestinations, files);

			if (nbDestinations > 1 && !attributes.isEmpty()) {
				fanOutToRemainingDestinations(forwardNode, p, destinations, attributes, nbDestinations, files);
			}
		}
		catch (IOException e) {
			log.error("Cannot connect to the final destination", e);
			throw e;
		}
		finally {
			// Force to clean the temporary bulk files (only after every parallel sending
			// has
			// finished)
			files.forEach(file -> FileUtil.delete(file.toPath()));
		}
	}

	/**
	 * Sends the buffered dataset to every destination after the first one. When parallel
	 * fan-out is enabled the sends (and per-destination editor application) run
	 * concurrently; otherwise they run sequentially as before. Each destination receives
	 * its own dataset copy so per-destination editors mutate independent objects and
	 * never read the shared buffer concurrently — the copies are made on the calling
	 * thread, only the editor application and the network send run in parallel.
	 */
	private void fanOutToRemainingDestinations(ForwardDicomNode forwardNode, Params p,
			List<ForwardDestination> destinations, Attributes attributes, int nbDestinations, List<File> files)
			throws IOException {
		if (!parallelFanout || fanoutExecutor == null) {
			for (int i = 1; i < nbDestinations; i++) {
				prepareAndTransfer(forwardNode, p, i, destinations.get(i), attributes, nbDestinations, files);
			}
			return;
		}

		List<Future<?>> futures = new ArrayList<>(nbDestinations - 1);
		for (int i = 1; i < nbDestinations; i++) {
			final int index = i;
			final ForwardDestination destination = destinations.get(i);
			final Attributes owned = new Attributes(attributes);
			futures.add(fanoutExecutor.submit(() -> {
				prepareAndTransfer(forwardNode, p, index, destination, owned, nbDestinations, files);
				return null;
			}));
		}
		awaitFanout(futures);
	}

	/**
	 * Waits for every parallel destination send to finish. Unlike the former sequential
	 * loop, all destinations are attempted even if one fails; the first failure is then
	 * re-thrown so the calling source still sees a forwarding error.
	 */
	private void awaitFanout(List<Future<?>> futures) throws IOException {
		IOException ioError = null;
		RuntimeException runtimeError = null;
		for (Future<?> future : futures) {
			try {
				future.get();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				futures.forEach(f -> f.cancel(true));
				throw new IOException("Interrupted while forwarding to multiple destinations", e);
			}
			catch (ExecutionException e) {
				Throwable cause = e.getCause();
				if (cause instanceof IOException io) {
					ioError = ioError != null ? ioError : io;
				}
				else if (cause instanceof RuntimeException re) {
					runtimeError = runtimeError != null ? runtimeError : re;
				}
				else {
					runtimeError = runtimeError != null ? runtimeError
							: new IllegalStateException(ERROR_WHEN_FORWARDING, cause);
				}
			}
		}
		if (ioError != null) {
			throw ioError;
		}
		if (runtimeError != null) {
			throw runtimeError;
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
		Attributes attToApply = nbDestinations > 1 ? attributes : null;
		if (destination.isVirtual()) {
			// Report-only destination: never forward, route to devnull. Still apply the
			// editors and publish the monitoring + conformance events.
			if (index == 0) {
				List<File> list = transferVirtual(fwdNode, destination, attToApply, p);
				if (list != null) {
					files.addAll(list);
				}
			}
			else if (!attributes.isEmpty()) {
				transferVirtualOther(fwdNode, destination, attributes, p);
			}
			return;
		}
		if (destination instanceof DicomForwardDestination dicomDestination) {
			// Lease one association from the destination's pool for the whole transfer,
			// so the same connection is used by prepareTransfer and the sending, then
			// return it.
			ScuLease lease = dicomDestination.acquire();
			try {
				prepareTransfer(lease, dicomDestination, p);
				if (index == 0) {
					List<File> list = transfer(fwdNode, dicomDestination, lease, attToApply, p);
					if (list != null) {
						files.addAll(list);
					}
				}
				else if (!attributes.isEmpty()) {
					transferOther(fwdNode, dicomDestination, lease, attributes, p);
				}
			}
			finally {
				dicomDestination.release(lease);
			}
		}
		else if (destination instanceof WebForwardDestination webDestination) {
			if (index == 0) {
				List<File> list = transfer(fwdNode, webDestination, attToApply, p);
				if (list != null) {
					files.addAll(list);
				}
			}
			else if (!attributes.isEmpty()) {
				transferOther(fwdNode, webDestination, attributes, p);
			}
		}
	}

	public static StoreFromStreamSCU prepareTransfer(ScuLease lease, DicomForwardDestination destination, Params p)
			throws IOException {
		String cuid = p.cuid();
		String tsuid = p.tsuid();
		String dstTsuid = destination.getOutputTransferSyntax(tsuid);
		StoreFromStreamSCU streamSCU = lease.scu();
		streamSCU.prepareTransfer(lease.service(), p.iuid(), cuid, dstTsuid);
		return streamSCU;
	}

	public List<File> transfer(ForwardDicomNode sourceNode, DicomForwardDestination destination, ScuLease lease,
			Attributes copy, Params p) throws IOException {
		StoreFromStreamSCU streamSCU = lease.scu();
		DicomInputStream in = null;
		List<File> files;
		Attributes attributesOriginal = new Attributes();
		Attributes attributesToSend = new Attributes();
		try {
			if (!streamSCU.isReadyForDataTransfer()) {
				throw new IllegalStateException("Association not ready for transfer.");
			}
			DataWriter dataWriter;
			String cuid = p.cuid();
			String iuid = p.iuid();
			String tsuid = p.tsuid();
			var syntax = new AdaptTransferSyntax(tsuid,
					streamSCU.selectTransferSyntax(cuid, destination.getOutputTransferSyntax(tsuid)));
			List<AttributeEditor> editors = destination.getDicomEditors();
			TransformedPlanarImage transformedPlanarImage = new TransformedPlanarImage();
			if (copy == null && editors.isEmpty() && syntax.getRequested().equals(tsuid)) {
				dataWriter = new InputStreamDataWriter(p.data());
				attributesToSend = new DicomInputStream(p.data()).readDataset();
				attributesOriginal.addAll(attributesToSend);
			}
			else {
				AttributeEditorContext context = new AttributeEditorContext(tsuid, sourceNode,
						streamSCU.getRemoteDicomNode());
				in = new DicomInputStream(p.data(), tsuid);
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

				abortIfRequested(context, p, true, "DICOM association abort: ");
				dataWriter = buildDataWriterFromTransformedImage(syntax, context, attributes, transformedPlanarImage,
						destination.isImageIdentityCheck());
			}

			launchCStore(p, streamSCU, dataWriter, cuid, iuid, syntax, transformedPlanarImage);

			progressNotify(destination, p.iuid(), p.cuid(), false, streamSCU);
			monitor(sourceNode, destination, attributesOriginal, attributesToSend, true, false, false, null,
					attributesOriginal.getString(Tag.Modality), p.cuid(), syntax.getSuitable(),
					buildImageIdentityCheckInput(destination, attributesOriginal, attributesToSend,
							transformedPlanarImage, p.tsuid()));
		}
		catch (AbortException e) {
			progressNotify(destination, p.iuid(), p.cuid(), true, streamSCU);
			monitor(sourceNode, destination, attributesOriginal, attributesToSend, false, false, errorReason(e),
					attributesOriginal.getString(Tag.Modality), p.cuid(), p.tsuid());
			if (e.getAbort() == Abort.CONNECTION_EXCEPTION) {
				throw e;
			}
		}
		catch (IOException e) {
			progressNotify(destination, p.iuid(), p.cuid(), true, streamSCU);
			monitor(sourceNode, destination, attributesOriginal, attributesToSend, false, true, errorReason(e),
					attributesOriginal.getString(Tag.Modality), p.cuid(), p.tsuid());
			throw e;
		}
		catch (Exception e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			progressNotify(destination, p.iuid(), p.cuid(), true, streamSCU);
			monitor(sourceNode, destination, attributesOriginal, attributesToSend, false, true, errorReason(e),
					attributesOriginal.getString(Tag.Modality), p.cuid(), p.tsuid());
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
			streamSCU.cstore(cuid, iuid, p.priority(), dataWriter, syntax.getSuitable());
		}
		finally {
			if (transformedPlanarImage != null && transformedPlanarImage.getPlanarImage() != null) {
				transformedPlanarImage.getPlanarImage().release();
			}
		}
	}

	private DataWriter buildDataWriterFromTransformedImage(AdaptTransferSyntax syntax, AttributeEditorContext context,
			Attributes attributes, TransformedPlanarImage transformedPlanarImage, boolean captureOutputImage)
			throws IOException {
		DataWriter dataWriter;
		BytesWithImageDescriptor desc = ImageAdapter.imageTranscode(attributes, syntax, context);
		boolean transformed = transformImage(attributes, context, transformedPlanarImage, captureOutputImage);
		dataWriter = ImageAdapter.buildDataWriter(attributes, syntax,
				transformed ? transformedPlanarImage.getEditablePlanarImage() : null, desc);
		return dataWriter;
	}

	/**
	 * Configure the masking/defacing transformation on the given
	 * {@link TransformedPlanarImage} in place. The realized {@link PlanarImage} is
	 * produced lazily by the editable when the data writer is consumed and stored back on
	 * the same instance, so the caller that owns {@code transformedPlanarImage} is
	 * responsible for releasing it. When {@code captureOutputImage} is {@code true}, the
	 * masked output pixels are also captured (as raw little-endian samples) at realization
	 * time for the conformance image-identity check.
	 * @return {@code true} if a transformation (mask or defacing) was configured,
	 * {@code false} otherwise
	 */
	private static boolean transformImage(Attributes attributes, AttributeEditorContext context,
			TransformedPlanarImage transformedPlanarImage, boolean captureOutputImage) {
		MaskArea m = context.getMaskArea();
		boolean defacing = LangUtil.emptyToFalse(context.getProperties().getProperty(Defacer.APPLY_DEFACING));

		// Retrieve additional masks (from the de-identification image API)
		// Each additional MaskArea can have a different color.
		List<MaskArea> additionalMasks = (List<MaskArea>) context.getProperties()
			.get(Profile.ADDITIONAL_MASK_AREAS_KEY);

		if (m != null || defacing || (additionalMasks != null && !additionalMasks.isEmpty())) {
			Editable<PlanarImage> editablePlanarImage = buildEditablePlanarImage(attributes, m, additionalMasks,
					defacing, transformedPlanarImage, captureOutputImage);
			transformedPlanarImage.setEditablePlanarImage(editablePlanarImage);
			transformedPlanarImage.setTransformApplied(true);
			return true;
		}
		return false;
	}

	private static Editable<PlanarImage> buildEditablePlanarImage(Attributes attributes, MaskArea m,
			List<MaskArea> additionalMasks, boolean defacing, TransformedPlanarImage transformedPlanarImage,
			boolean captureOutputImage) {
		return img -> {
			PlanarImage planarImage = buildPlanarImage(attributes, m, additionalMasks, defacing, img);
			transformedPlanarImage.setPlanarImage(planarImage);
			if (captureOutputImage) {
				// Capture the output pixels here, while the masked image is still alive
				transformedPlanarImage.setPixelDataBytes(rawBytesFromPlanarImage(planarImage));
			}
			return planarImage;
		};
	}

	private static PlanarImage buildPlanarImage(Attributes attributes, MaskArea m, List<MaskArea> additionalMasks,
			boolean defacing, PlanarImage img) {
		PlanarImage image = img;
		if (defacing) {
			image = Defacer.apply(attributes, image);
		}
		// Apply the primary mask (from static config or the first API mask). Each draw
		// returns a new image, so the previous intermediate must be released or its native
		// OpenCV Mat leaks (off-heap, invisible to the JVM). The source image (img) is owned
		// by the ImageAdapter pipeline and must never be released here.
		if (m != null) {
			PlanarImage masked = MaskArea.drawShape(image.toMat(), m);
			releaseIntermediate(image, img, masked);
			image = masked;
		}
		// Apply each additional mask (from the de-identification image API).
		if (additionalMasks != null) {
			for (MaskArea additional : additionalMasks) {
				PlanarImage masked = MaskArea.drawShape(image.toMat(), additional);
				releaseIntermediate(image, img, masked);
				image = masked;
			}
		}
		return image;
	}

	/**
	 * Releases an intermediate image produced while chaining transformations, unless it is
	 * the source image (owned by the {@link ImageAdapter} pipeline) or the freshly produced
	 * image that is kept for the next step / final output.
	 */
	private static void releaseIntermediate(PlanarImage current, PlanarImage source, PlanarImage next) {
		if (current != source && current != next && !current.isReleased()) {
			current.release();
		}
	}

	/**
	 * Serializes the realized (masked/defaced) output image to raw little-endian DICOM
	 * pixel bytes for the conformance image-identity check. Supports 8- and 16-bit samples
	 * (the depths of the modalities that carry burned-in identity); returns an empty array
	 * for anything else so the caller falls back to the sent dataset's own pixel data.
	 */
	private static byte[] rawBytesFromPlanarImage(PlanarImage image) {
		if (image == null) {
			return new byte[0];
		}
		Mat mat = image.toMat();
		if (mat == null || mat.empty()) {
			return new byte[0];
		}
		// A single Mat.get over the whole buffer requires a continuous layout.
		Mat continuous = mat.isContinuous() ? mat : mat.clone();
		int count = (int) continuous.total() * continuous.channels();
		int depth = CvType.depth(continuous.type());
		if (depth == CvType.CV_8U || depth == CvType.CV_8S) {
			byte[] data = new byte[count];
			continuous.get(0, 0, data);
			return data;
		}
		if (depth == CvType.CV_16U || depth == CvType.CV_16S) {
			short[] samples = new short[count];
			continuous.get(0, 0, samples);
			ByteBuffer buffer = ByteBuffer.allocate(count * 2).order(ByteOrder.LITTLE_ENDIAN);
			buffer.asShortBuffer().put(samples);
			return buffer.array();
		}
		return new byte[0];
	}

	private static List<File> cleanOrGetBulkDataFiles(DicomInputStream in, boolean clean) {
		StreamUtil.safeClose(in);
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

	public void transferOther(ForwardDicomNode fwdNode, DicomForwardDestination destination, ScuLease lease,
			Attributes copy, Params p) throws IOException {
		StoreFromStreamSCU streamSCU = lease.scu();
		Attributes attributesToSend = new Attributes();
		Attributes attributesOriginal = new Attributes();
		try {
			if (!streamSCU.isReadyForDataTransfer()) {
				throw new IllegalStateException("Association not ready for transfer.");
			}

			DataWriter dataWriter;
			String tsuid = p.tsuid();
			String iuid = p.iuid();
			String cuid = p.cuid();
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

				abortIfRequested(context, p, false, "DICOM association abort. ");
				dataWriter = buildDataWriterFromTransformedImage(syntax, context, attributes, transformedPlanarImage,
						destination.isImageIdentityCheck());
			}

			launchCStore(p, streamSCU, dataWriter, cuid, iuid, syntax, transformedPlanarImage);

			progressNotify(destination, p.iuid(), p.cuid(), false, streamSCU);
			monitor(fwdNode, destination, attributesOriginal, attributesToSend, true, false, false, null,
					attributesOriginal.getString(Tag.Modality), p.cuid(), syntax.getSuitable(),
					buildImageIdentityCheckInput(destination, attributesOriginal, attributesToSend,
							transformedPlanarImage, p.tsuid()));
		}
		catch (AbortException e) {
			progressNotify(destination, p.iuid(), p.cuid(), true, streamSCU);
			monitor(fwdNode, destination, attributesOriginal, attributesToSend, false, false, errorReason(e),
					attributesOriginal.getString(Tag.Modality), p.cuid(), p.tsuid());
			if (e.getAbort() == Abort.CONNECTION_EXCEPTION) {
				throw e;
			}
		}
		catch (IOException e) {
			progressNotify(destination, p.iuid(), p.cuid(), true, streamSCU);
			monitor(fwdNode, destination, attributesOriginal, attributesToSend, false, true, errorReason(e),
					attributesOriginal.getString(Tag.Modality), p.cuid(), p.tsuid());
			throw e;
		}
		catch (Exception e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			progressNotify(destination, p.iuid(), p.cuid(), true, streamSCU);
			monitor(fwdNode, destination, attributesOriginal, attributesToSend, false, true, errorReason(e),
					attributesOriginal.getString(Tag.Modality), p.cuid(), p.tsuid());
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
		TransformedPlanarImage transformedPlanarImage = new TransformedPlanarImage();
		try {
			List<AttributeEditor> editors = destination.getDicomEditors();
			DicomStowRS stow = destination.getStowrsSingleFile();
			var syntax = new AdaptTransferSyntax(p.tsuid(), destination.getOutputTransferSyntax(p.tsuid()));

			if (syntax.getRequested().equals(p.tsuid()) && copy == null && editors.isEmpty()) {
				Attributes fmi = Attributes.createFileMetaInformation(p.iuid(), p.cuid(), syntax.getRequested());
				try (InputStream stream = p.data()) {
					attributesToSend = new DicomInputStream(p.data()).readDataset();
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
				AttributeEditorContext context = new AttributeEditorContext(p.tsuid(), fwdNode, null);
				in = new DicomInputStream(p.data(), p.tsuid());
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

				abortIfRequested(context, p, true, "STOW-RS abort: ");

				BytesWithImageDescriptor desc = ImageAdapter.imageTranscode(attributes, syntax, context);
				if (desc == null) {
					stow.uploadDicom(attributes, syntax.getOriginal());
				}
				else {
					uploadPayLoadFromTransformedImage(stow, syntax, context, attributes, desc, transformedPlanarImage,
							destination.isImageIdentityCheck());
				}
			}
			progressNotify(destination, p.iuid(), p.cuid(), false, 0);
			monitor(fwdNode, destination, attributesOriginal, attributesToSend, true, false, false, null,
					attributesOriginal.getString(Tag.Modality), p.cuid(), syntax.getSuitable(),
					buildImageIdentityCheckInput(destination, attributesOriginal, attributesToSend,
							transformedPlanarImage, p.tsuid()));
		}
		catch (AbortException e) {
			progressNotify(destination, p.iuid(), p.cuid(), true, 0);
			monitor(fwdNode, destination, attributesOriginal, attributesToSend, false, false, errorReason(e),
					attributesOriginal.getString(Tag.Modality), p.cuid(), p.tsuid());
			if (e.getAbort() == Abort.CONNECTION_EXCEPTION) {
				throw e;
			}
		}
		catch (IOException e) {
			progressNotify(destination, p.iuid(), p.cuid(), true, 0);
			monitor(fwdNode, destination, attributesOriginal, attributesToSend, false, true, errorReason(e),
					attributesOriginal.getString(Tag.Modality), p.cuid(), p.tsuid());
			throw e;
		}
		catch (Exception e) {
			progressNotify(destination, p.iuid(), p.cuid(), true, 0);
			monitor(fwdNode, destination, attributesOriginal, attributesToSend, false, true, errorReason(e),
					attributesOriginal.getString(Tag.Modality), p.cuid(), p.tsuid());
			log.error(ERROR_WHEN_FORWARDING, e);
		}
		finally {
			files = cleanOrGetBulkDataFiles(in, copy == null);
		}
		return files;
	}

	private void uploadPayLoadFromTransformedImage(DicomStowRS stow, AdaptTransferSyntax syntax,
			AttributeEditorContext context, Attributes attributes, BytesWithImageDescriptor desc,
			TransformedPlanarImage transformedPlanarImage, boolean captureOutputImage) throws Exception {
		try {
			if (!transformImage(attributes, context, transformedPlanarImage, captureOutputImage)) {
				throw new IllegalStateException("Cannot transcode image for STOW-RS upload.");
			}
			stow.uploadPayload(DicomStowRS.createCompressedImagePayload(attributes, syntax, desc,
					transformedPlanarImage.getEditablePlanarImage()));
		}
		finally {
			if (transformedPlanarImage.getPlanarImage() != null) {
				transformedPlanarImage.getPlanarImage().release();
			}
		}
	}

	public void transferOther(ForwardDicomNode fwdNode, WebForwardDestination destination, Attributes copy, Params p)
			throws IOException {
		Attributes attributesToSend = new Attributes();
		Attributes attributesOriginal = new Attributes();
		TransformedPlanarImage transformedPlanarImage = new TransformedPlanarImage();
		try {
			List<AttributeEditor> editors = destination.getDicomEditors();
			DicomStowRS stow = destination.getStowrsSingleFile();
			var syntax = new AdaptTransferSyntax(p.tsuid(), destination.getOutputTransferSyntax(p.tsuid()));
			if (syntax.getRequested().equals(p.tsuid()) && editors.isEmpty()) {
				attributesToSend = copy;
				attributesOriginal.addAll(copy);
				stow.uploadDicom(copy, syntax.getRequested());
			}
			else {
				AttributeEditorContext context = new AttributeEditorContext(p.tsuid(), fwdNode, null);
				Attributes attributes = new Attributes(copy);
				attributesToSend = attributes;
				attributesOriginal.addAll(attributes);
				editors.forEach(e -> e.apply(attributes, context));

				abortIfRequested(context, p, false, "DICOM association abort. ");

				BytesWithImageDescriptor desc = ImageAdapter.imageTranscode(attributes, syntax, context);
				if (desc == null) {
					stow.uploadDicom(attributes, syntax.getOriginal());
				}
				else {
					uploadPayLoadFromTransformedImage(stow, syntax, context, attributes, desc, transformedPlanarImage,
							destination.isImageIdentityCheck());
				}
			}
			progressNotify(destination, p.iuid(), p.cuid(), false, 0);
			monitor(fwdNode, destination, attributesOriginal, attributesToSend, true, false, false, null,
					attributesOriginal.getString(Tag.Modality), p.cuid(), syntax.getSuitable(),
					buildImageIdentityCheckInput(destination, attributesOriginal, attributesToSend,
							transformedPlanarImage, p.tsuid()));
		}
		catch (HttpException httpException) {
			if (httpException.getStatusCode() != 409) {
				progressNotify(destination, p.iuid(), p.cuid(), true, 0);
				monitor(fwdNode, destination, attributesOriginal, attributesToSend, false, true,
						httpException.getMessage(), attributesOriginal.getString(Tag.Modality), p.cuid(), p.tsuid());
				throw new AbortException(Abort.FILE_EXCEPTION, "DICOMWeb forward", httpException);
			}
			else {
				progressNotify(destination, p.iuid(), p.cuid(), false, 0);
				// Already present in the destination: count as a retry, neither
				// sent nor errored.
				monitor(fwdNode, destination, attributesOriginal, attributesToSend, false, false, true, null,
						attributesOriginal.getString(Tag.Modality), p.cuid(), p.tsuid());
				log.debug("File already present in destination");
			}
		}
		catch (AbortException e) {
			progressNotify(destination, p.iuid(), p.cuid(), true, 0);
			monitor(fwdNode, destination, attributesOriginal, attributesToSend, false, false, errorReason(e),
					attributesOriginal.getString(Tag.Modality), p.cuid(), p.tsuid());
			if (e.getAbort() == Abort.CONNECTION_EXCEPTION) {
				throw e;
			}
		}
		catch (IOException e) {
			progressNotify(destination, p.iuid(), p.cuid(), true, 0);
			monitor(fwdNode, destination, attributesOriginal, attributesToSend, false, true, errorReason(e),
					attributesOriginal.getString(Tag.Modality), p.cuid(), p.tsuid());
			throw e;
		}
		catch (Exception e) {
			progressNotify(destination, p.iuid(), p.cuid(), true, 0);
			monitor(fwdNode, destination, attributesOriginal, attributesToSend, false, true, errorReason(e),
					attributesOriginal.getString(Tag.Modality), p.cuid(), p.tsuid());
			log.error(ERROR_WHEN_FORWARDING, e);
		}
	}

	/**
	 * Transfer for a virtual (report-only) destination reading from the incoming stream.
	 * The dataset is read and the destination editors are applied so the conformance
	 * report reflects what would have been sent, then it is discarded (devnull) instead
	 * of being forwarded. The monitoring and conformance events are still published.
	 */
	public List<File> transferVirtual(ForwardDicomNode fwdNode, ForwardDestination destination, Attributes copy,
			Params p) throws IOException {
		DicomInputStream in = null;
		List<File> files;
		Attributes attributesToSend = new Attributes();
		Attributes attributesOriginal = new Attributes();
		try {
			List<AttributeEditor> editors = destination.getDicomEditors();
			AttributeEditorContext context = new AttributeEditorContext(p.tsuid(), fwdNode, null);
			in = new DicomInputStream(p.data(), p.tsuid());
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
			abortIfRequested(context, p, true, "Virtual destination abort: ");
			// No network send: the dataset is routed to devnull.
			progressNotify(destination, p.iuid(), p.cuid(), false, 0);
			monitor(fwdNode, destination, attributesOriginal, attributesToSend, true, false, false, null,
					attributesOriginal.getString(Tag.Modality), p.cuid(), p.tsuid(),
					buildImageIdentityCheckInput(destination, attributesOriginal, attributesToSend, null, p.tsuid()));
		}
		catch (AbortException e) {
			progressNotify(destination, p.iuid(), p.cuid(), true, 0);
			monitor(fwdNode, destination, attributesOriginal, attributesToSend, false, false, errorReason(e),
					attributesOriginal.getString(Tag.Modality), p.cuid(), p.tsuid());
			if (e.getAbort() == Abort.CONNECTION_EXCEPTION) {
				throw e;
			}
		}
		catch (IOException e) {
			progressNotify(destination, p.iuid(), p.cuid(), true, 0);
			monitor(fwdNode, destination, attributesOriginal, attributesToSend, false, true, errorReason(e),
					attributesOriginal.getString(Tag.Modality), p.cuid(), p.tsuid());
			throw e;
		}
		catch (Exception e) {
			progressNotify(destination, p.iuid(), p.cuid(), true, 0);
			monitor(fwdNode, destination, attributesOriginal, attributesToSend, false, true, errorReason(e),
					attributesOriginal.getString(Tag.Modality), p.cuid(), p.tsuid());
			log.error(ERROR_WHEN_FORWARDING, e);
		}
		finally {
			files = cleanOrGetBulkDataFiles(in, copy == null);
		}
		return files;
	}

	/**
	 * Transfer for a virtual (report-only) destination served from the buffered dataset
	 * copy (fan-out to destinations after the first one). Behaves like
	 * {@link #transferVirtual} but does not consume the incoming stream.
	 */
	public void transferVirtualOther(ForwardDicomNode fwdNode, ForwardDestination destination, Attributes copy,
			Params p) throws IOException {
		Attributes attributesToSend = new Attributes();
		Attributes attributesOriginal = new Attributes();
		try {
			List<AttributeEditor> editors = destination.getDicomEditors();
			AttributeEditorContext context = new AttributeEditorContext(p.tsuid(), fwdNode, null);
			Attributes attributes = new Attributes(copy);
			attributesToSend = attributes;
			attributesOriginal.addAll(attributes);
			if (!editors.isEmpty()) {
				editors.forEach(e -> e.apply(attributes, context));
			}
			abortIfRequested(context, p, false, "Virtual destination abort. ");
			progressNotify(destination, p.iuid(), p.cuid(), false, 0);
			monitor(fwdNode, destination, attributesOriginal, attributesToSend, true, false, null,
					attributesOriginal.getString(Tag.Modality), p.cuid(), p.tsuid());
		}
		catch (AbortException e) {
			progressNotify(destination, p.iuid(), p.cuid(), true, 0);
			monitor(fwdNode, destination, attributesOriginal, attributesToSend, false, false, errorReason(e),
					attributesOriginal.getString(Tag.Modality), p.cuid(), p.tsuid());
			if (e.getAbort() == Abort.CONNECTION_EXCEPTION) {
				throw e;
			}
		}
		catch (Exception e) {
			progressNotify(destination, p.iuid(), p.cuid(), true, 0);
			monitor(fwdNode, destination, attributesOriginal, attributesToSend, false, true, errorReason(e),
					attributesOriginal.getString(Tag.Modality), p.cuid(), p.tsuid());
			log.error(ERROR_WHEN_FORWARDING, e);
		}
	}

	private static void progressNotify(ForwardDestination destination, String iuid, String cuid, boolean failed,
			StoreFromStreamSCU streamSCU) {
		streamSCU.removeIUIDProcessed(iuid);
		// Use the leased SCU's own state (each pooled association has its own progress).
		ServiceUtil.notifyProgression(streamSCU.getState(), iuid, cuid,
				failed ? Status.ProcessingFailure : Status.Success,
				failed ? ProgressStatus.FAILED : ProgressStatus.COMPLETED, streamSCU.getNumberOfSuboperations());
	}

	private static void progressNotify(ForwardDestination destination, String iuid, String cuid, boolean failed,
			int subOperations) {
		ServiceUtil.notifyProgression(destination.getState(), iuid, cuid,
				failed ? Status.ProcessingFailure : Status.Success,
				failed ? ProgressStatus.FAILED : ProgressStatus.COMPLETED, subOperations);
	}

	/**
	 * Translate an editor-requested abort into an {@link AbortException}. When
	 * {@code streaming} is {@code true} (the source object is still being read off the
	 * association) the pending payload is drained on a file-level abort and the
	 * association is aborted on a connection-level abort before the exception is raised.
	 * @param context the editor context carrying the requested {@link Abort}
	 * @param p the transfer parameters (source stream / association)
	 * @param streaming whether the incoming object is being streamed from the association
	 * @param connectionAbortPrefix message prefix for a connection-level abort
	 * @throws AbortException if an abort was requested
	 * @throws IOException if draining the pending payload fails
	 */
	private static void abortIfRequested(AttributeEditorContext context, Params p, boolean streaming,
			String connectionAbortPrefix) throws AbortException, IOException {
		Abort abort = context.getAbort();
		if (abort == Abort.FILE_EXCEPTION) {
			if (streaming && p.data() instanceof PDVInputStream pdv) {
				pdv.skipAll();
			}
			throw new AbortException(abort, context.getAbortMessage());
		}
		if (abort == Abort.CONNECTION_EXCEPTION) {
			if (streaming && p.as() != null) {
				p.as().abort();
			}
			throw new AbortException(abort, connectionAbortPrefix + context.getAbortMessage());
		}
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
	 * Records, for every destination of a forward node, an inbound object that was
	 * rejected before any transfer was attempted (e.g. an unauthorized source, or a
	 * DICOMDIR that is never forwarded). Counted as excluded — not a delivery error — but
	 * the reason is kept so the rejection is visible in monitoring instead of only in the
	 * logs. The dataset is usually not parsed at this point, so the study/series context
	 * may be empty.
	 * @param sourceNode Forward node the object was addressed to
	 * @param destinations Destinations that would have received the object
	 * @param attributes Whatever attributes are available (may be empty)
	 * @param sopClassUid SOP Class UID of the rejected object, when known
	 * @param reason Why the object was not forwarded
	 */
	public void monitorRejected(ForwardDicomNode sourceNode, List<ForwardDestination> destinations,
			Attributes attributes, String sopClassUid, String reason) {
		if (destinations == null) {
			return;
		}
		for (ForwardDestination destination : destinations) {
			monitor(sourceNode, destination, attributes, attributes, false, false, false, reason,
					attributes.getString(Tag.Modality), sopClassUid, null);
		}
	}

	/**
	 * Publish an event for monitoring purpose and, when the destination requests a
	 * conformance report, an event carrying a metadata-only snapshot of the dataset sent.
	 * @param sourceNode Forward node
	 * @param destination Destination
	 * @param attributesOriginal Original value
	 * @param attributesToSend De-identify value
	 * @param sent Flag to know if the transfer occurred
	 * @param reason Reason of not transferring the file
	 * @param modality Modality of the file transferred
	 * @param sopClassUid Sop Class Uid
	 * @param tsuidSent Transfer syntax the file was (to be) sent with
	 */
	private void monitor(ForwardDicomNode sourceNode, ForwardDestination destination, Attributes attributesOriginal,
			Attributes attributesToSend, boolean sent, boolean error, String reason, String modality,
			String sopClassUid, String tsuidSent) {
		monitor(sourceNode, destination, attributesOriginal, attributesToSend, sent, error, false, reason, modality,
				sopClassUid, tsuidSent);
	}

	/**
	 * Reason text for a failed transfer. Falls back to the exception's simple class name
	 * when it carries no message (e.g. a {@code NullPointerException} or a message-less
	 * {@code IOException}), so a failure is never monitored without a reason.
	 */
	private static String errorReason(Throwable e) {
		String message = e.getMessage();
		return (message == null || message.isBlank()) ? e.getClass().getSimpleName() : message;
	}

	/**
	 * @param duplicate The destination reported the object as already present (HTTP 409);
	 * the outcome is counted as a retry rather than a distinct instance.
	 */
	private void monitor(ForwardDicomNode sourceNode, ForwardDestination destination, Attributes attributesOriginal,
			Attributes attributesToSend, boolean sent, boolean error, boolean duplicate, String reason, String modality,
			String sopClassUid, String tsuidSent) {
		monitor(sourceNode, destination, attributesOriginal, attributesToSend, sent, error, duplicate, reason, modality,
				sopClassUid, tsuidSent, null);
	}

	private void monitor(ForwardDicomNode sourceNode, ForwardDestination destination, Attributes attributesOriginal,
			Attributes attributesToSend, boolean sent, boolean error, boolean duplicate, String reason, String modality,
			String sopClassUid, String tsuidSent, ImageIdentityCheckInput imageIdentityCheckInput) {
		applicationEventPublisher
			.publishEvent(new TransferMonitoringEvent(MonitoringEntry.of(sourceNode.getId(), destination.getId(),
					attributesOriginal, attributesToSend, sent, error, duplicate, reason, modality, sopClassUid)));
		if (destination.isBuildConformanceReport() && attributesToSend.containsValue(Tag.SOPClassUID)) {
			// The conformance snapshot is secondary telemetry, published after the
			// transfer outcome above. Building it can throw (e.g. an incompatible
			// SpecificCharacterSet while copying attributes); that failure must be
			// contained here. If it propagated, the caller's transfer() error handler
			// would catch it and record a second, spurious error outcome for an instance
			// already reported as sent.
			try {
				// The snapshot must be built synchronously: bulk data references become
				// invalid once the temporary files are cleaned. When deep-sequence
				// validation is on, capture as deep as the validator recurses; otherwise
				// the default depth
				boolean deep = destination.isDeepSequenceValidation();
				int snapshotDepth = deep ? conformanceMaxSequenceDepth : MetadataSnapshot.DEFAULT_MAX_SEQUENCE_DEPTH;
				MetadataSnapshot snapshot = MetadataSnapshot.of(attributesToSend, snapshotDepth);
				applicationEventPublisher.publishEvent(
						new ConformanceCollectEvent(InstanceConformanceData.of(sourceNode.getId(), destination.getId(),
								sourceNode.getAet(), tsuidSent, sent, reason, destination.isCheckValueConformity(),
								deep, destination.isDeidentified(), snapshot, imageIdentityCheckInput)));
			}
			catch (RuntimeException e) {
				log.error("Cannot build conformance snapshot for instance {}: {}",
						attributesToSend.getString(Tag.SOPInstanceUID), e.getMessage());
			}
		}
	}

	/**
	 * Captures, on the forwarding thread, the encoded pixel data the receiver gets and the
	 * original identifying values so the conformance report pipeline can later query the
	 * de-identification image API for burned-in identity. When the destination masked or
	 * defaced the image, the check runs on the realized <em>output</em> pixels captured at
	 * transformation time (raw little-endian, hence {@link UID#ExplicitVRLittleEndian});
	 * When no image transformation was applied (metadata-only de-identification,
	 * pass-through, or a virtual destination), the sent pixels equal {@code attributesToSend}'s
	 * own pixel data, which is read here. The pixel bytes must be read on this thread because
	 * their bulk-data temp files are cleaned right after the transfer.
	 * Returns {@code null} when the check is disabled or
	 * the instance carries no readable pixel data.
	 */
	private ImageIdentityCheckInput buildImageIdentityCheckInput(ForwardDestination destination,
			Attributes attributesOriginal, Attributes attributesToSend, TransformedPlanarImage transformedPlanarImage,
			String originalTsuid) {
		if (!destination.isImageIdentityCheck()) {
			return null;
		}
		byte[] imageBytes;
		String tsuid;
		boolean transformApplied = transformedPlanarImage != null && transformedPlanarImage.isTransformApplied();
		if (transformApplied) {
			// The image was masked/defaced: the check must run on the transformed output
			// pixels the receiver gets. The sent dataset (attributesToSend) still holds the
			// original, unmasked PixelData bulk reference (the mask is drawn lazily into the
			// encoded stream, never written back).
			// If the output pixels could not be captured, skip the check.
			byte[] outputPixels = transformedPlanarImage.getPixelDataBytes();
			if (outputPixels == null || outputPixels.length == 0) {
				return null;
			}
			imageBytes = outputPixels;
			tsuid = UID.ExplicitVRLittleEndian;
		}
		else {
			// No image transformation: the sent pixels equal the sent dataset's own pixels.
			imageBytes = deidentifyImageService.extractPixelDataBytes(attributesToSend);
			if (imageBytes.length == 0) {
				return null;
			}
			tsuid = originalTsuid;
		}
		return new ImageIdentityCheckInput(imageBytes, SensitiveTagDefinition.extractSensitiveData(attributesOriginal),
				tsuid);
	}

}
