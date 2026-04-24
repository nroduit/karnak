/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
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

	/**
	 * @param apiBaseUrl the base URL of the de-identification image API
	 */
	public DeidentifyImageService(
			@Value("${karnak.deidentify-image.url:http://localhost:8000}") String apiBaseUrl) {
		this.apiBaseUrl = apiBaseUrl;
		this.restClient = RestClient.builder().baseUrl(apiBaseUrl).build();
		this.objectMapper = new ObjectMapper();
	}

	/**
	 * Sends the DICOM instance image and sensitive data to the external
	 * de-identification API, and extracts the mask definitions from the JSON response.
	 *
	 * <p>
	 * If the API detects no sensitive data burned into the image, the JSON response will
	 * have no {@code masks} field. In that case, this method returns an empty list.
	 * @param dcmAttributes the DICOM attributes of the instance
	 * @param sensitiveData a map of tag name → tag value for sensitive information
	 * @return a list of {@link MaskBody} extracted from the API response, or an empty list
	 * if no masks were found or an error occurred
	 */
	public List<MaskBody> callDeidentifyImageApi(Attributes dcmAttributes, Map<String, String> sensitiveData) {
		// Extract pixel data bytes from the DICOM instance
		byte[] imageBytes = extractPixelDataBytes(dcmAttributes);
		if (imageBytes == null || imageBytes.length == 0) {
			log.warn("Could not extract pixel data from DICOM instance — skipping API call");
			return Collections.emptyList();
		}

		// Serialize the sensitive data map to JSON
		String sensitiveDataJson;
		try {
			sensitiveDataJson = objectMapper.writeValueAsString(sensitiveData);
		}
		catch (JsonProcessingException e) {
			log.error("Failed to serialize sensitive data to JSON", e);
			return Collections.emptyList();
		}

		// Build the multipart request body
		MultiValueMap<String, HttpEntity<?>> multipartBody = generateMultipartBody(dcmAttributes,
				imageBytes, sensitiveDataJson);

		// Send the POST request and get the JSON response
		String jsonResponse;
		try {
			jsonResponse = restClient.post()
				.uri("/desidentify-image")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(multipartBody)
				.retrieve()
				.body(String.class);
		}
		catch (Exception e) {
			log.error("Error calling de-identification image API at {}/desidentify-image: {}",
					apiBaseUrl, e.getMessage(), e);
			return Collections.emptyList();
		}

		// Parse the JSON and extract the "masks" field
		return extractMasksFromJson(jsonResponse);
	}


	MultiValueMap<String, HttpEntity<?>> generateMultipartBody(Attributes dcmAttributes,
															   byte[] imageBytes, String sensitiveDataJson) {
		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();

		String filename = determineImageFilename(dcmAttributes);
		MediaType imageMediaType = determineImageMediaType(dcmAttributes);

		bodyBuilder.part("image", new ByteArrayResource(imageBytes) {
			@Override
			public String getFilename() {
				return filename;
			}
		}).contentType(imageMediaType);

		bodyBuilder.part("sensitive_data_list", new ByteArrayResource(sensitiveDataJson.getBytes()) {
			@Override
			public String getFilename() {
				return "sensitive_data_list.json";
			}
		}).contentType(MediaType.APPLICATION_JSON);

		if (filename.endsWith(".raw")) {
			bodyBuilder.part("rows", String.valueOf(dcmAttributes.getInt(Tag.Rows, 0)))
				.contentType(MediaType.TEXT_PLAIN);
			bodyBuilder.part("columns", String.valueOf(dcmAttributes.getInt(Tag.Columns, 0)))
				.contentType(MediaType.TEXT_PLAIN);
			bodyBuilder.part("bits_allocated", String.valueOf(dcmAttributes.getInt(Tag.BitsAllocated, 0)))
				.contentType(MediaType.TEXT_PLAIN);
			bodyBuilder.part("samples_per_pixel", String.valueOf(dcmAttributes.getInt(Tag.SamplesPerPixel, 0)))
				.contentType(MediaType.TEXT_PLAIN);
		}

		return bodyBuilder.build();
	}

	/**
	 * Parses the JSON string returned by the API into a {@link DeidentifyImageResponse}
	 * and extracts the {@code masks} field.
	 *
	 * @param jsonContent the raw JSON string returned by the API
	 * @return a list of {@link MaskBody}, or an empty list if none found
	 */
	List<MaskBody> extractMasksFromJson(String jsonContent) {
		if (jsonContent == null || jsonContent.isBlank()) {
			log.debug("Empty JSON response from de-identification image API — no masks to apply");
			return Collections.emptyList();
		}

		try {
			DeidentifyImageResponse response = objectMapper.readValue(jsonContent, DeidentifyImageResponse.class);

			if (response.getMasks() == null || response.getMasks().isEmpty()) {
				log.debug("No masks found in de-identification image API response (message: {})", response.getMessage());
				return Collections.emptyList();
			}

			log.debug("Masks extracted from de-identification image API response (message: {})",
					response.getMessage());
			return response.getMasks();
		}
		catch (Exception e) {
			log.error("Failed to parse JSON response from de-identification image API", e);
			return Collections.emptyList();
		}
	}

	/**
	 * Extracts the raw pixel data bytes from a DICOM Attributes object.
	 *
	 * @param dcmAttributes the DICOM attributes containing pixel data
	 * @return the pixel data as a byte array, or {@code null} if extraction fails
	 */
	byte[] extractPixelDataBytes(Attributes dcmAttributes) {
		Object pixelData = dcmAttributes.getValue(Tag.PixelData);

		if (pixelData instanceof BulkData bulkData) {
			// BulkData = uncompressed pixel data stored as raw bytes.
			try {
				return bulkData.toBytes(VR.OW, bulkData.bigEndian());
			}
			catch (IOException e) {
				log.error("Failed to read BulkData pixel bytes", e);
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
					catch (IOException e) {
						log.error("Failed to read Fragments frame BulkData bytes", e);
						return null;
					}
				}
			}
		}

		log.warn("Pixel data is not BulkData or Fragments — cannot extract image bytes");
		return null;
	}

	/**
	 * Determines a suitable filename for the image part of the multipart request.
	 *
	 * @param dcmAttributes the DICOM attributes
	 * @return a filename like "image.jpg", "image.jp2", or "image.raw"
	 */
	String determineImageFilename(Attributes dcmAttributes) {
		String tsuid = dcmAttributes.getString(Tag.TransferSyntaxUID);
		if (tsuid != null) {
			// JPEG transfer syntaxes start with 1.2.840.10008.1.2.4.50 to .57
			if (tsuid.startsWith("1.2.840.10008.1.2.4.5") || tsuid.equals("1.2.840.10008.1.2.4.70")) {
				return "image.jpg";
			}
			// JPEG 2000 transfer syntaxes: 1.2.840.10008.1.2.4.90, .91
			if (tsuid.startsWith("1.2.840.10008.1.2.4.9")) {
				return "image.jp2";
			}
		}
		// Default: raw uncompressed pixel data
		return "image.raw";
	}

	/**
	 * Determines the appropriate media type for the image data.
	 * @param dcmAttributes the DICOM attributes
	 * @return {@link MediaType#IMAGE_JPEG}, or {@link MediaType#APPLICATION_OCTET_STREAM}
	 * as default
	 */
	MediaType determineImageMediaType(Attributes dcmAttributes) {
		String tsuid = dcmAttributes.getString(Tag.TransferSyntaxUID);
		if (tsuid != null) {
			if (tsuid.startsWith("1.2.840.10008.1.2.4.5")) {
				return MediaType.IMAGE_JPEG;
			}
		}
		// No standard MediaType for JPEG2000 in Spring, use octet-stream
		return MediaType.APPLICATION_OCTET_STREAM;
	}

}
