/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.profilepipe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.BulkData;
import org.dcm4che3.data.Fragments;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.util.TagUtils;
import org.jspecify.annotations.Nullable;
import org.karnak.backend.model.profilebody.MaskBody;
import org.karnak.backend.model.profilepipe.DeidentifyImageResponse;
import org.karnak.backend.model.profilepipe.ReportingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Service responsible for calling the external de-identification image API.
 */
@Slf4j
@Service
public class DeidentifyImageService {

	private final String apiBaseUrl;

	private final RestClient restClient;

	private final ObjectMapper objectMapper;

	private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

	private static final int[] EMPTY_INT_ARRAY = new int[0];

	private static final String PALETTE_COLOR = "PALETTE COLOR";

	private static final String MONOCHROME1 = "MONOCHROME1";

	private static final String MONOCHROME2 = "MONOCHROME2";

	/**
	 * Number of entries of a Palette Color LUT whose descriptor declares {@code 0}, which
	 * means 2^16 entries (see PS3.3 C.7.9.2).
	 */
	private static final int MAX_LUT_ENTRIES = 65536;

	private static final TransferSyntaxMapping JPEG = new TransferSyntaxMapping("image.jpg", MediaType.IMAGE_JPEG);

	private static final TransferSyntaxMapping JP2 = new TransferSyntaxMapping("image.jp2",
			MediaType.parseMediaType("image/jp2"));

	private static final TransferSyntaxMapping JLS = new TransferSyntaxMapping("image.jls",
			MediaType.parseMediaType("image/jls"));

	private static final TransferSyntaxMapping JPX = new TransferSyntaxMapping("image.jpx",
			MediaType.parseMediaType("image/jpx"));

	private static final TransferSyntaxMapping JXL = new TransferSyntaxMapping("image.jxl",
			MediaType.parseMediaType("image/jxl"));

	private static final TransferSyntaxMapping JPHC = new TransferSyntaxMapping("image.jphc",
			MediaType.parseMediaType("image/jphc"));

	private static final TransferSyntaxMapping RAW = new TransferSyntaxMapping("image.raw",
			MediaType.APPLICATION_OCTET_STREAM);

	private static final Map<String, TransferSyntaxMapping> TS_MAPPINGS = Map.ofEntries(
			// JPEG
			Map.entry("1.2.840.10008.1.2.4.50", JPEG), Map.entry("1.2.840.10008.1.2.4.51", JPEG),
			Map.entry("1.2.840.10008.1.2.4.53", JPEG), Map.entry("1.2.840.10008.1.2.4.55", JPEG),
			Map.entry("1.2.840.10008.1.2.4.57", JPEG), Map.entry("1.2.840.10008.1.2.4.70", JPEG),
			// JPEG-LS
			Map.entry("1.2.840.10008.1.2.4.80", JLS), Map.entry("1.2.840.10008.1.2.4.81", JLS),
			// JPEG 2000
			Map.entry("1.2.840.10008.1.2.4.90", JP2), Map.entry("1.2.840.10008.1.2.4.91", JP2),
			// JPEG 2000 Part 2
			Map.entry("1.2.840.10008.1.2.4.92", JPX), Map.entry("1.2.840.10008.1.2.4.93", JPX),
			// JPEG XL
			Map.entry("1.2.840.10008.1.2.4.110", JXL), Map.entry("1.2.840.10008.1.2.4.111", JXL),
			Map.entry("1.2.840.10008.1.2.4.112", JXL),
			// High-Throughput JPEG 2000
			Map.entry("1.2.840.10008.1.2.4.201", JPHC), Map.entry("1.2.840.10008.1.2.4.202", JPHC),
			Map.entry("1.2.840.10008.1.2.4.203", JPHC));

	/**
	 * @param apiBaseUrl the base URL of the de-identification image API
	 */
	public DeidentifyImageService(@Value("${ocr.url:http://localhost:8000}") String apiBaseUrl) {
		this.apiBaseUrl = apiBaseUrl;
		this.objectMapper = new ObjectMapper();

		HttpClient jdkClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
		this.restClient = RestClient.builder()
			.baseUrl(apiBaseUrl)
			.requestFactory(new JdkClientHttpRequestFactory(jdkClient))
			.build();
	}

	/**
	 * Sends the DICOM instance image and sensitive data to the external de-identification
	 * API, and extracts the mask definitions from the JSON response.
	 *
	 * <p>
	 * If the API detects no sensitive data burned into the image, the JSON response will
	 * have no {@code masks} field. In that case, this method returns an empty list.
	 * @param dcmAttributes the DICOM attributes of the instance
	 * @param sensitiveData a map of tag name → tag value for sensitive information
	 * @throws DeidentifyImageException if there is a problem when calling the
	 * deidentification API
	 * @return a list of {@link MaskBody} extracted from the API response, or an empty
	 * list if no masks were found or an error occurred
	 */
	public List<MaskBody> callDeidentifyImageApi(Attributes dcmAttributes, Map<String, String> sensitiveData,
			String tsuid) {
		// Extract pixel data bytes from the DICOM instance
		byte[] imageBytes = extractPixelDataBytes(dcmAttributes);
		if (imageBytes.length == 0) {
			log.warn("Could not extract pixel data from DICOM instance — skipping API call");
			return Collections.emptyList();
		}

		// Serialize the sensitive data map to JSON
		String sensitiveDataJson;
		try {
			sensitiveDataJson = objectMapper.writeValueAsString(sensitiveData);
		}
		catch (JsonProcessingException ex) {
			log.error("Failed to serialize sensitive data to JSON", ex);
			return Collections.emptyList();
		}

		// Build the multipart request body
		MultiValueMap<String, HttpEntity<?>> multipartBody = generateMultipartBody(dcmAttributes, imageBytes,
				sensitiveDataJson, tsuid);

		// Send the POST request and get the JSON response
		String jsonResponse = postMultipart("/deidentify-image", "de-identification image API", multipartBody);

		if (jsonResponse == null) {
			log.warn("Empty response body from de-identification image API — no masks to apply");
			return Collections.emptyList();
		}

		// Parse the JSON response
		// Check the SOP UID
		// Extract the masks field
		return extractMasksFromJson(jsonResponse, dcmAttributes.getString(Tag.SOPInstanceUID));
	}

	/**
	 * Sends the received image and its original identifying values to the external
	 * {@code /reporting} endpoint and returns the names of the DICOM tags whose value was
	 * found burned into the pixel data.
	 * @param metadata the (metadata-only) DICOM attributes describing the image geometry
	 * @param imageBytes the encoded pixel data
	 * @param sensitiveData a map of tag name to tag value for the identifying information
	 * to look for
	 * @param tsuid the transfer syntax the {@code imageBytes} are encoded with
	 * @param sopInstanceUid the SOP Instance UID echoed back for concurrency safety
	 * @throws DeidentifyImageException if there is a problem when calling the reporting
	 * API
	 * @return the detected identifying tag names, or an empty list when none were found
	 */
	public List<String> callReportingApi(Attributes metadata, byte[] imageBytes, Map<String, String> sensitiveData,
			String tsuid, String sopInstanceUid) {
		if (imageBytes == null || imageBytes.length == 0) {
			log.warn("No pixel data to inspect - skipping reporting API call");
			return Collections.emptyList();
		}

		String sensitiveDataJson;
		try {
			sensitiveDataJson = objectMapper.writeValueAsString(sensitiveData);
		}
		catch (JsonProcessingException ex) {
			throw new DeidentifyImageException("Failed to serialize sensitive data to JSON for the reporting API", ex);
		}

		MultiValueMap<String, HttpEntity<?>> multipartBody = generateMultipartBody(metadata, imageBytes,
				sensitiveDataJson, tsuid);

		String jsonResponse = postMultipart("/reporting", "reporting API", multipartBody);

		return extractDetectedTagsFromJson(jsonResponse, sopInstanceUid);
	}

	/**
	 * Sends the multipart body to {@code endpoint} and returns the raw JSON response
	 * body. Transport failures and HTTP error responses are translated into
	 * {@link DeidentifyImageException}, with {@code apiLabel} identifying the target API
	 * in the message.
	 * @return the response body, or {@code null} when the API returned an empty body
	 */
	private @Nullable String postMultipart(String endpoint, String apiLabel,
			MultiValueMap<String, HttpEntity<?>> multipartBody) {
		try {
			return restClient.post()
				.uri(endpoint)
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(multipartBody)
				.accept(MediaType.parseMediaType("application/json; version=1"))
				.retrieve()
				.body(String.class);
		}
		catch (HttpClientErrorException ex) {
			// Errors 4xx
			throw new DeidentifyImageException(String.format("Client error %s from %s - check the request format: %s",
					ex.getStatusCode(), apiLabel, ex.getMessage()), ex);
		}
		catch (HttpServerErrorException ex) {
			// Errors 5xx
			throw new DeidentifyImageException(
					String.format("Server error %s from %s - service may be temporarily unavailable",
							ex.getStatusCode(), apiLabel),
					ex);
		}
		catch (ResourceAccessException ex) {
			throw new DeidentifyImageException(String.format("Cannot reach %s at %s - service is unavailable: %s",
					apiLabel, apiBaseUrl, ex.getMessage()), ex);
		}
		catch (Exception ex) {
			throw new DeidentifyImageException("Unexpected error calling " + apiLabel + ": " + ex.getMessage(), ex);
		}
	}

	/**
	 * Parses the JSON string returned by the {@code /reporting} endpoint and extracts the
	 * {@code detected_tags} field.
	 * @param jsonContent the raw JSON string returned by the API
	 * @return the detected identifying tag names, or an empty list if none found
	 */
	List<String> extractDetectedTagsFromJson(String jsonContent, String expectedSopInstanceUID) {
		if (jsonContent == null || jsonContent.isBlank()) {
			log.debug("Empty JSON response from reporting API - no identifying data reported");
			return Collections.emptyList();
		}

		ReportingResponse response;
		try {
			response = objectMapper.readValue(jsonContent, ReportingResponse.class);
		}
		catch (Exception ex) {
			throw new DeidentifyImageException("Failed to parse JSON response from the reporting API", ex);
		}

		if (response.sopInstanceUid() != null && !response.sopInstanceUid().equals(expectedSopInstanceUID)) {
			log.error(
					"The SOP Instance UID in the reporting API response ({}) does not match the expected UID ({}) - ignoring detected tags",
					response.sopInstanceUid(), expectedSopInstanceUID);
			return Collections.emptyList();
		}

		return response.detectedTags() == null ? Collections.emptyList() : response.detectedTags();
	}

	MultiValueMap<String, HttpEntity<?>> generateMultipartBody(Attributes dcmAttributes, byte[] imageBytes,
			String sensitiveDataJson, String tsuid) {
		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();

		TransferSyntaxMapping mapping = resolveMapping(tsuid);
		boolean rawPixelData = mapping.filename().endsWith(".raw");

		// The Palette Color LUT is resolved first because the photometric interpretation
		// and the samples per pixel sent to the API depend on whether a usable LUT could
		// be built.
		String paletteLutJson = rawPixelData ? buildPaletteColorLutJson(dcmAttributes) : null;
		boolean paletteFallback = rawPixelData && isPaletteColor(dcmAttributes) && paletteLutJson == null;
		int rows = dcmAttributes.getInt(Tag.Rows, 0);
		int columns = dcmAttributes.getInt(Tag.Columns, 0);
		int bitsAllocated = dcmAttributes.getInt(Tag.BitsAllocated, 0);
		int samplesPerPixel = resolveSamplesPerPixel(dcmAttributes, paletteFallback);

		byte[] payload = rawPixelData
				? firstRawFrame(imageBytes, rows, columns, bitsAllocated, samplesPerPixel,
						dcmAttributes.getString(Tag.SOPInstanceUID))
				: imageBytes;

		bodyBuilder.part("image", new ByteArrayResource(payload) {
			@Override
			public String getFilename() {
				return mapping.filename();
			}
		}).contentType(mapping.mediaType());

		addTextPart(bodyBuilder, "sensitive_data_list", sensitiveDataJson);
		// dcmAttributes is never null here: callDeidentifyImageApi returns as soon as
		// extractPixelDataBytes gives back the empty array it answers a null instance
		// with, and its own caller has already read the sensitive tags off the
		// instance.
		addTextPart(bodyBuilder, "sop_instance_uid", dcmAttributes.getString(Tag.SOPInstanceUID)); // NOSONAR
		addTextPart(bodyBuilder, "transfer_syntax_uid", tsuid);

		addTextPart(bodyBuilder, "rows", rows);
		addTextPart(bodyBuilder, "columns", columns);
		addTextPart(bodyBuilder, "bits_allocated", bitsAllocated);
		addTextPart(bodyBuilder, "samples_per_pixel", samplesPerPixel);
		String photometricInterpretation = resolvePhotometricInterpretation(dcmAttributes, paletteFallback);
		addTextPart(bodyBuilder, "photometric_interpretation", photometricInterpretation);

		if (rawPixelData) {
			addRawPixelDataParts(bodyBuilder, dcmAttributes, paletteLutJson, photometricInterpretation);
		}

		return bodyBuilder.build();
	}

	/**
	 * Keeps only the first frame of an uncompressed pixel data buffer and checks it
	 * against the declared geometry. Compressed streams are sent frame by frame already
	 * (see {@code extractPixelDataBytesFromFragments}), while raw pixel data holds every
	 * frame back to back: sending the whole buffer makes the API fail to decode the
	 * image. A buffer smaller than a single frame is a genuine inconsistency: the request
	 * is rejected here, with the sizes at hand, rather than as an opaque HTTP 400.
	 * @throws DeidentifyImageException when the buffer is too small for the declared
	 * geometry
	 */
	byte[] firstRawFrame(byte[] imageBytes, int rows, int columns, int bitsAllocated, int samplesPerPixel,
			@Nullable String sopInstanceUid) {
		int frameLength = rows * columns * samplesPerPixel * ((bitsAllocated + 7) / 8);
		if (frameLength <= 0) {
			log.warn("Incomplete image geometry for SOP Instance UID {} (rows={}, columns={}, bits allocated={}, "
					+ "samples per pixel={}) - sending the pixel data as is", sopInstanceUid, rows, columns,
					bitsAllocated, samplesPerPixel);
			return imageBytes;
		}
		if (imageBytes.length < frameLength) {
			throw new DeidentifyImageException(String.format(
					"Pixel data of SOP Instance UID %s holds %d bytes but a single %dx%d frame of %d bit(s) and %d "
							+ "sample(s) per pixel needs %d bytes",
					sopInstanceUid, imageBytes.length, rows, columns, bitsAllocated, samplesPerPixel, frameLength));
		}
		if (imageBytes.length == frameLength) {
			return imageBytes;
		}
		// Multi-frame instances (and buffers padded to an even length) are truncated to
		// their first frame, which is the one the API is asked to inspect.
		log.debug("Pixel data of SOP Instance UID {} holds {} bytes, keeping the first {} bytes frame",
				sopInstanceUid, imageBytes.length, frameLength);
		return Arrays.copyOf(imageBytes, frameLength);
	}

	/**
	 * Returns the photometric interpretation to send to the API.
	 *
	 * <p>
	 * A {@code PALETTE COLOR} image whose LUT is missing or unusable is declared as
	 * {@code MONOCHROME2}: the stored pixel values are single channel indexes which remain
	 * fully usable for burned-in text detection. Without this normalization the API would
	 * receive a {@code PALETTE COLOR} image without its LUT and would reject the request
	 * with an HTTP 400.
	 */
	private @Nullable String resolvePhotometricInterpretation(Attributes dcmAttributes, boolean paletteFallback) {
		if (paletteFallback) {
			log.warn("PALETTE COLOR without usable LUT for SOP Instance UID {} - falling back to {}",
					dcmAttributes.getString(Tag.SOPInstanceUID), MONOCHROME2);
			return MONOCHROME2;
		}
		return dcmAttributes.getString(Tag.PhotometricInterpretation);
	}

	/**
	 * Returns the samples per pixel to send to the API, forcing {@code 1} when a
	 * {@code PALETTE COLOR} image is downgraded to {@code MONOCHROME2}: an inconsistent
	 * value would make the API compute an erroneous buffer size for the raw pixel data.
	 */
	private int resolveSamplesPerPixel(Attributes dcmAttributes, boolean paletteFallback) {
		int samplesPerPixel = dcmAttributes.getInt(Tag.SamplesPerPixel, 0);
		if (paletteFallback && samplesPerPixel != 1) {
			log.warn("Inconsistent Samples per Pixel ({}) for a PALETTE COLOR image - forcing 1", samplesPerPixel);
			return 1;
		}
		return samplesPerPixel;
	}

	private void addRawPixelDataParts(MultipartBodyBuilder bodyBuilder, Attributes attrs,
			@Nullable String paletteLutJson, @Nullable String photometricInterpretation) {
		// The API applies the photometric polarity of raw pixel data through this flag
		// only: it never reads back the photometric interpretation on that path.
		addTextPart(bodyBuilder, "is_monochrome1", MONOCHROME1.equals(photometricInterpretation));
		addOptionalDoublePart(bodyBuilder, attrs, "rescale_slope", Tag.RescaleSlope, 1.0);
		addOptionalDoublePart(bodyBuilder, attrs, "rescale_intercept", Tag.RescaleIntercept, 0.0);
		addOptionalDoublePart(bodyBuilder, attrs, "window_center", Tag.WindowCenter, 0.0);
		addOptionalDoublePart(bodyBuilder, attrs, "window_width", Tag.WindowWidth, 0.0);

		if (paletteLutJson != null) {
			addTextPart(bodyBuilder, "palette_color_lut", paletteLutJson);
		}
	}

	private void addOptionalDoublePart(MultipartBodyBuilder bodyBuilder, Attributes attrs, String name, int tag,
			double defaultValue) {
		if (attrs.containsValue(tag)) {
			addTextPart(bodyBuilder, name, attrs.getDouble(tag, defaultValue));
		}
	}

	private void addTextPart(MultipartBodyBuilder bodyBuilder, String name, Object value) {
		bodyBuilder.part(name, String.valueOf(value)).contentType(MediaType.TEXT_PLAIN);
	}

	/**
	 * Parses the JSON string returned by the API into a {@link DeidentifyImageResponse}
	 * and extracts the {@code masks} field.
	 * @param jsonContent the raw JSON string returned by the API
	 * @return a list of {@link MaskBody}, or an empty list if none found
	 */
	List<MaskBody> extractMasksFromJson(String jsonContent, String expectedSopInstanceUID) {
		if (jsonContent == null || jsonContent.isBlank()) {
			log.debug("Empty JSON response from de-identification image API — no masks to apply");
			return Collections.emptyList();
		}

		try {
			DeidentifyImageResponse response = objectMapper.readValue(jsonContent, DeidentifyImageResponse.class);

			if (response.sopInstanceUid() == null) {
				log.error("The SOP Instance UID in the API response is null");
				return Collections.emptyList();
			}

			if (!response.sopInstanceUid().equals(expectedSopInstanceUID)) {
				log.error(
						"The SOP Instance UID in the API response ({}) does not match the expected UID ({}) — skipping masks",
						response.sopInstanceUid(), expectedSopInstanceUID);
				return Collections.emptyList();
			}

			if (response.masks() == null || response.masks().isEmpty()) {
				log.debug("No masks found in de-identification image API response (message: {})", response.message());
				return Collections.emptyList();
			}

			log.debug("Masks extracted from de-identification image API response (message: {})", response.message());
			return response.masks();
		}
		catch (Exception ex) {
			log.error("Failed to parse JSON response from de-identification image API", ex);
			return Collections.emptyList();
		}
	}

	/**
	 * Extracts the raw pixel data bytes from a DICOM Attributes object.
	 * @param dcmAttributes the DICOM attributes containing pixel data
	 * @return the pixel data as a byte array, or an empty array if extraction fails
	 */
	public byte[] extractPixelDataBytes(Attributes dcmAttributes) {
		if (dcmAttributes == null) {
			log.error("The passed DCMAttributes is null !");
			return EMPTY_BYTE_ARRAY;
		}

		Object pixelData = dcmAttributes.getValue(Tag.PixelData);

		if (pixelData instanceof byte[] rawBytes) {
			// Uncompressed pixel data read fully in memory.
			return rawBytes;
		}
		else if (pixelData instanceof BulkData bulkData) {
			// BulkData = uncompressed pixel data stored as raw bytes.
			try {
				return bulkData.toBytes(VR.OW, bulkData.bigEndian());
			}
			catch (IOException ex) {
				log.error("Failed to read BulkData pixel bytes", ex);
				return EMPTY_BYTE_ARRAY;
			}
		}
		else if (pixelData instanceof Fragments fragments) {
			return extractPixelDataBytesFromFragments(fragments);
		}

		log.warn("Pixel data is not BulkData or Fragments — cannot extract image bytes");
		return EMPTY_BYTE_ARRAY;
	}

	private byte[] extractPixelDataBytesFromFragments(Fragments fragments) {
		// Fragments = compressed pixel data (JPEG, JPEG2000, etc.).
		// Index 0 is the offset table (usually empty bytes), index 1+ are the
		// actual compressed image frames. We extract the first actual frame.
		if (fragments.size() > 1) {
			Object frame = fragments.get(1);
			if (frame instanceof byte[] bytes) {
				return bytes;
			}
			else if (frame instanceof BulkData frameBulkData) {
				try {
					return frameBulkData.toBytes(fragments.vr(), frameBulkData.bigEndian());
				}
				catch (IOException ex) {
					log.error("Failed to read Fragments frame BulkData bytes", ex);
					return EMPTY_BYTE_ARRAY;
				}
			}
		}

		return EMPTY_BYTE_ARRAY;
	}

	private TransferSyntaxMapping resolveMapping(String tsuid) {
		if (tsuid != null) {
			TransferSyntaxMapping retrievedMapping = TS_MAPPINGS.get(tsuid);
			if (retrievedMapping != null) {
				return retrievedMapping;
			}
		}
		return RAW;
	}

	/**
	 * Builds a JSON string for the Palette Color LUT if present in DICOM attributes.
	 * Returns a JSON object with "red", "green", "blue" arrays, or null if no palette LUT
	 * data is found.
	 */
	@Nullable String buildPaletteColorLutJson(Attributes dcmAttributes) {
		if (!isPaletteColor(dcmAttributes)) {
			return null;
		}

		int[] redDesc = dcmAttributes.getInts(Tag.RedPaletteColorLookupTableDescriptor);
		int[] greenDesc = dcmAttributes.getInts(Tag.GreenPaletteColorLookupTableDescriptor);
		int[] blueDesc = dcmAttributes.getInts(Tag.BluePaletteColorLookupTableDescriptor);
		if (isMalformedDescriptor(redDesc) || isMalformedDescriptor(greenDesc) || isMalformedDescriptor(blueDesc)) {
			log.warn("PALETTE COLOR photometric but missing or malformed LUT descriptors");
			return null;
		}

		int[] redLut = extractLutData(dcmAttributes, Tag.RedPaletteColorLookupTableData, redDesc);
		int[] greenLut = extractLutData(dcmAttributes, Tag.GreenPaletteColorLookupTableData, greenDesc);
		int[] blueLut = extractLutData(dcmAttributes, Tag.BluePaletteColorLookupTableData, blueDesc);
		if (redLut.length == 0 || greenLut.length == 0 || blueLut.length == 0) {
			log.warn("PALETTE COLOR photometric but missing or unusable LUT data");
			return null;
		}

		Map<String, int[]> lutMap = new HashMap<>();
		lutMap.put("red", redLut);
		lutMap.put("green", greenLut);
		lutMap.put("blue", blueLut);

		try {
			return objectMapper.writeValueAsString(lutMap);
		}
		catch (JsonProcessingException ex) {
			log.error("Failed to serialize Palette Color LUT to JSON", ex);
			return null;
		}
	}

	/**
	 * Returns {@code true} when the instance declares a {@code PALETTE COLOR} photometric
	 * interpretation.
	 */
	private boolean isPaletteColor(Attributes dcmAttributes) {
		return PALETTE_COLOR.equals(dcmAttributes.getString(Tag.PhotometricInterpretation));
	}

	/**
	 * A Palette Color LUT descriptor must hold exactly 3 values: [numberOfEntries,
	 * firstStoredPixelValue, bitsPerEntry].
	 */
	private boolean isMalformedDescriptor(int @Nullable [] descriptor) {
		return descriptor == null || descriptor.length != 3;
	}

	/**
	 * Extracts LUT data for a single color channel. The descriptor array has 3 values:
	 * [numberOfEntries, firstStoredPixelValue, bitsPerEntry]. If bitsPerEntry is 8,
	 * values are read as bytes; if 16, as unsigned shorts (ints).
	 * @return the LUT entries, or an empty array when the data is absent or inconsistent
	 * with its descriptor
	 */
	private int[] extractLutData(Attributes dcmAttributes, int lutDataTag, int[] descriptor) {
		return normalizeLutData(readLutData(dcmAttributes, lutDataTag, descriptor), descriptor, lutDataTag);
	}

	private int[] readLutData(Attributes dcmAttributes, int lutDataTag, int[] descriptor) {
		int bitsPerEntry = descriptor[2];
		if (bitsPerEntry == 8) {
			byte[] data = dcmAttributes.getSafeBytes(lutDataTag);
			if (data == null) {
				return EMPTY_INT_ARRAY;
			}
			int[] result = new int[data.length];
			for (int i = 0; i < data.length; i++) {
				result[i] = data[i] & 0xFF;
			}
			return result;
		}
		int[] lutDatas = dcmAttributes.getInts(lutDataTag);
		return lutDatas == null ? EMPTY_INT_ARRAY : lutDatas;
	}

	/**
	 * Checks the LUT data against the number of entries declared by its descriptor.
	 * Truncated data is rejected (an empty array is returned) so that the caller falls
	 * back to a grayscale image instead of sending a palette the API cannot apply. Extra
	 * entries are dropped: an 8-bit LUT segment is padded to an even length when it holds
	 * an odd number of entries.
	 */
	private int[] normalizeLutData(int[] lutData, int[] descriptor, int lutDataTag) {
		if (lutData.length == 0) {
			return EMPTY_INT_ARRAY;
		}

		int expectedEntries = descriptor[0] == 0 ? MAX_LUT_ENTRIES : descriptor[0];
		if (lutData.length < expectedEntries) {
			log.warn("Palette Color LUT data {} is truncated: {} entries read but {} declared by its descriptor",
					TagUtils.toString(lutDataTag), lutData.length, expectedEntries);
			return EMPTY_INT_ARRAY;
		}
		if (lutData.length > expectedEntries) {
			log.debug("Palette Color LUT data {} holds {} entries, truncating to the {} declared by its descriptor",
					TagUtils.toString(lutDataTag), lutData.length, expectedEntries);
			return Arrays.copyOf(lutData, expectedEntries);
		}
		return lutData;
	}

	private record TransferSyntaxMapping(String filename, MediaType mediaType) {
	}

}
