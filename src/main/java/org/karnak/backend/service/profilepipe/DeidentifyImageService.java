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

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.BulkData;
import org.dcm4che3.data.Fragments;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.karnak.backend.model.profilebody.MaskBody;
import org.karnak.backend.model.profilepipe.DeidentifyImageResponse;

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
	public DeidentifyImageService(@Value("${karnak.deidentify-image.url:http://localhost:8000}") String apiBaseUrl) {
		this.apiBaseUrl = apiBaseUrl;
		this.objectMapper = new ObjectMapper();

		HttpClient jdkClient = HttpClient.newBuilder()
			.version(HttpClient.Version.HTTP_1_1)
			.build();
		this.restClient = RestClient.builder().baseUrl(apiBaseUrl).requestFactory(new JdkClientHttpRequestFactory(jdkClient)).build();
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
	 * @throws DeidentifyImageException if there is a problem when calling the deidentification API
	 * @return a list of {@link MaskBody} extracted from the API response, or an empty
	 * list if no masks were found or an error occurred
	 */
	public List<MaskBody> callDeidentifyImageApi(Attributes dcmAttributes, Map<String, String> sensitiveData,
			String tsuid) {
		// Extract pixel data bytes from the DICOM instance
		byte[] imageBytes = this.extractPixelDataBytes(dcmAttributes);
		if (imageBytes == null || imageBytes.length == 0) {
			log.warn("Could not extract pixel data from DICOM instance — skipping API call");
			return Collections.emptyList();
		}

		// Serialize the sensitive data map to JSON
		String sensitiveDataJson;
		try {
			sensitiveDataJson = this.objectMapper.writeValueAsString(sensitiveData);
		}
		catch (JsonProcessingException ex) {
			log.error("Failed to serialize sensitive data to JSON", ex);
			return Collections.emptyList();
		}

		// Build the multipart request body
		MultiValueMap<String, HttpEntity<?>> multipartBody = this.generateMultipartBody(dcmAttributes, imageBytes,
				sensitiveDataJson, tsuid);

		// Send the POST request and get the JSON response
		String jsonResponse;
		log.info("multipartBody={}", multipartBody);
		try {
			jsonResponse = this.restClient.post()
				.uri("/deidentify-image")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(multipartBody)
				.accept(MediaType.parseMediaType("application/json; version=1"))
				.retrieve()
				.body(String.class);
		}
		catch (HttpClientErrorException ex) {
			// Errors 4xx
			throw new DeidentifyImageException(String.format(
					"Client error %s from de-identification image API — check the request format: %s",
					ex.getStatusCode(), ex.getMessage()), ex);
		}
		catch (HttpServerErrorException ex) {
			// Errors 5xx
			throw new DeidentifyImageException(String.format(
					"Server error %s from de-identification image API — service may be temporarily unavailable",
					ex.getStatusCode()), ex);
		}
		catch (ResourceAccessException ex) {
			throw new DeidentifyImageException(String.format(
					"Cannot reach de-identification image API at %s — service is unavailable: %s", this.apiBaseUrl,
					ex.getMessage()), ex);
		}
		catch (Exception ex) {
			throw new DeidentifyImageException("Unexpected error calling de-identification image API: " + ex.getMessage(),
					ex);
		}

		// Parse the JSON response
		// Check the SOP UID
		// Extract the masks field
		return this.extractMasksFromJson(jsonResponse, dcmAttributes.getString(Tag.SOPInstanceUID));
	}

	MultiValueMap<String, HttpEntity<?>> generateMultipartBody(Attributes dcmAttributes, byte[] imageBytes,
			String sensitiveDataJson, String tsuid) {
		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();

		TransferSyntaxMapping mapping = this.resolveMapping(tsuid);
		bodyBuilder.part("image", new ByteArrayResource(imageBytes) {
			@Override
			public String getFilename() {
				return mapping.filename();
			}
		}).contentType(mapping.mediaType());

		this.addTextPart(bodyBuilder, "sensitive_data_list", sensitiveDataJson);
		this.addTextPart(bodyBuilder, "sop_instance_uid", dcmAttributes.getString(Tag.SOPInstanceUID));
		this.addTextPart(bodyBuilder, "transfer_syntax_uid", tsuid);

		this.addTextPart(bodyBuilder, "rows", dcmAttributes.getInt(Tag.Rows, 0));
		this.addTextPart(bodyBuilder, "columns", dcmAttributes.getInt(Tag.Columns, 0));
		this.addTextPart(bodyBuilder, "bits_allocated", dcmAttributes.getInt(Tag.BitsAllocated, 0));
		this.addTextPart(bodyBuilder, "samples_per_pixel", dcmAttributes.getInt(Tag.SamplesPerPixel, 0));
		this.addTextPart(bodyBuilder, "photometric_interpretation", dcmAttributes.getString(Tag.PhotometricInterpretation));

		if (mapping.filename().endsWith(".raw")) {
			this.addRawPixelDataParts(bodyBuilder, dcmAttributes);
		}

		return bodyBuilder.build();
	}

	private void addRawPixelDataParts(MultipartBodyBuilder bodyBuilder, Attributes attrs) {
		this.addOptionalDoublePart(bodyBuilder, attrs, "rescale_slope", Tag.RescaleSlope, 1.0);
		this.addOptionalDoublePart(bodyBuilder, attrs, "rescale_intercept", Tag.RescaleIntercept, 0.0);
		this.addOptionalDoublePart(bodyBuilder, attrs, "window_center", Tag.WindowCenter, 0.0);
		this.addOptionalDoublePart(bodyBuilder, attrs, "window_width", Tag.WindowWidth, 0.0);

		String paletteLutJson = this.buildPaletteColorLutJson(attrs);
		if (paletteLutJson != null) {
			this.addTextPart(bodyBuilder, "palette_color_lut", paletteLutJson);
		}
	}

	private void addOptionalDoublePart(MultipartBodyBuilder bodyBuilder, Attributes attrs, String name, int tag,
			double defaultValue) {
		if (attrs.containsValue(tag)) {
			this.addTextPart(bodyBuilder, name, attrs.getDouble(tag, defaultValue));
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
			DeidentifyImageResponse response = this.objectMapper.readValue(jsonContent, DeidentifyImageResponse.class);

			if (response.sopInstanceUid() == null) {
				log.error("The SOP Instance UID in the API response is null");
				return Collections.emptyList();
			}

			if (!response.sopInstanceUid().equals(expectedSopInstanceUID)) {
				log.error("The SOP Instance UID in the API response ({}) does not match the expected UID ({}) — skipping masks",
					response.sopInstanceUid(), expectedSopInstanceUID);
				return Collections.emptyList();
			}

			if (response.masks() == null || response.masks().isEmpty()) {
				log.debug("No masks found in de-identification image API response (message: {})",
						response.message());
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
	 * @return the pixel data as a byte array, or {@code null} if extraction fails
	 */
	byte[] extractPixelDataBytes(Attributes dcmAttributes) {
		if (dcmAttributes == null) {
			log.error("The passed DCMAttributes is null !");
			return null;
		}

		Object pixelData = dcmAttributes.getValue(Tag.PixelData);

		if (pixelData instanceof BulkData bulkData) {
			// BulkData = uncompressed pixel data stored as raw bytes.
			try {
				return bulkData.toBytes(VR.OW, bulkData.bigEndian());
			}
			catch (IOException ex) {
				log.error("Failed to read BulkData pixel bytes", ex);
				return null;
			}
		}
		else if (pixelData instanceof Fragments fragments) {
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
						return null;
					}
				}
			}
		}

		log.warn("Pixel data is not BulkData or Fragments — cannot extract image bytes");
		return null;
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
	String buildPaletteColorLutJson(Attributes dcmAttributes) {
		String photometric = dcmAttributes.getString(Tag.PhotometricInterpretation);
		if (!"PALETTE COLOR".equals(photometric)) {
			return null;
		}

		int[] redDesc = dcmAttributes.getInts(Tag.RedPaletteColorLookupTableDescriptor);
		int[] greenDesc = dcmAttributes.getInts(Tag.GreenPaletteColorLookupTableDescriptor);
		int[] blueDesc = dcmAttributes.getInts(Tag.BluePaletteColorLookupTableDescriptor);
		if (redDesc == null || greenDesc == null || blueDesc == null) {
			log.warn("PALETTE COLOR photometric but missing LUT descriptors");
			return null;
		}

		int[] redLut = this.extractLutData(dcmAttributes, Tag.RedPaletteColorLookupTableData, redDesc);
		int[] greenLut = this.extractLutData(dcmAttributes, Tag.GreenPaletteColorLookupTableData, greenDesc);
		int[] blueLut = this.extractLutData(dcmAttributes, Tag.BluePaletteColorLookupTableData, blueDesc);
		if (redLut == null || greenLut == null || blueLut == null) {
			log.warn("PALETTE COLOR photometric but missing LUT data");
			return null;
		}

		Map<String, int[]> lutMap = new HashMap<>();
		lutMap.put("red", redLut);
		lutMap.put("green", greenLut);
		lutMap.put("blue", blueLut);

		try {
			return this.objectMapper.writeValueAsString(lutMap);
		}
		catch (JsonProcessingException ex) {
			log.error("Failed to serialize Palette Color LUT to JSON", ex);
			return null;
		}
	}

	/**
	 * Extracts LUT data for a single color channel. The descriptor array has 3 values:
	 * [numberOfEntries, firstStoredPixelValue, bitsPerEntry]. If bitsPerEntry is 8,
	 * values are read as bytes; if 16, as unsigned shorts (ints).
	 */
	private int[] extractLutData(Attributes dcmAttributes, int lutDataTag, int[] descriptor) {
		int bitsPerEntry = descriptor[2];
		if (bitsPerEntry == 8) {
			byte[] data = dcmAttributes.getSafeBytes(lutDataTag);
			if (data == null) {
				return null;
			}
			int[] result = new int[data.length];
			for (int i = 0; i < data.length; i++) {
				result[i] = data[i] & 0xFF;
			}
			return result;
		}
		return dcmAttributes.getInts(lutDataTag);
	}

	private record TransferSyntaxMapping(String filename, MediaType mediaType) {
	}

}
