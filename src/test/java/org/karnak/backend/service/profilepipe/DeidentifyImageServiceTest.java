/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.profilepipe;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.BulkData;
import org.dcm4che3.data.Fragments;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.karnak.backend.model.profilebody.MaskBody;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;

public class DeidentifyImageServiceTest {

	@TempDir
	Path tempDir;

	// Service
	DeidentifyImageService deidentifyImageService;

	@BeforeEach
	public void setup() {
		this.deidentifyImageService = new DeidentifyImageService("http://localhost:8000");
	}

	// Extract mask from JSON
	@Test
	public void extractMasksFromJson_null_params_should_return_empty_list() {
		List<MaskBody> maskBodies = this.deidentifyImageService.extractMasksFromJson(null, null);
		assertThat(maskBodies).isEmpty();
	}

	@Test
	public void extractMasksFromJson_blank_json_should_return_empty_list() {
		List<MaskBody> maskBodies = this.deidentifyImageService.extractMasksFromJson("", null);
		assertThat(maskBodies).isEmpty();
	}

	@Test
	public void extractMasksFromJson_valid_json_null_response_uid_should_return_empty_list() {
		String responseJson = """
				{
					"message": "Sensitive data detected",
					"masks": [
					{
						"stationName": "*",
						"color": "2e2d2d",
						"rectangles": [
							"229 16 69 19",
							"568 15 44 21"
						]
					},
					{
						"stationName": "*",
						"color": "202020",
						"rectangles": [
							"231 44 271 20"
						]
					}
					]
				}
				""";
		String expectedUid = "2.25.251867431509614238946512793485716204981";

		List<MaskBody> maskBodies = this.deidentifyImageService.extractMasksFromJson(responseJson, expectedUid);

		assertThat(maskBodies).isEmpty();
	}

	@Test
	public void extractMasksFromJson_valid_json_response_uid_not_equals_expected_should_return_empty_list() {
		String responseJson = """
				{
					"message": "Sensitive data detected",
					"masks": [
					{
						"stationName": "*",
						"color": "2e2d2d",
						"rectangles": [
							"229 16 69 19",
							"568 15 44 21"
						]
					},
					{
						"stationName": "*",
						"color": "202020",
						"rectangles": [
							"231 44 271 20"
						]
					}
					],
				  "sop_instance_uid": "2.25.251867431509614238946512793485716204981"
				}
				""";
		String expectedUid = "2.25.251867431509614238946512793485716204980";

		List<MaskBody> maskBodies = this.deidentifyImageService.extractMasksFromJson(responseJson, expectedUid);

		assertThat(maskBodies).isEmpty();
	}

	@Test
	public void extractMasksFromJson_valid_json_response_no_mask_should_return_empty_list() {
		String responseJson = """
				{
					"message": "Test",
					"sop_instance_uid": "2.25.251867431509614238946512793485716204981"
				}
				""";
		String expectedUid = "2.25.251867431509614238946512793485716204981";

		List<MaskBody> maskBodies = this.deidentifyImageService.extractMasksFromJson(responseJson, expectedUid);

		assertThat(maskBodies).isEmpty();
	}

	@Test
	public void extractMasksFromJson_valid_json_response_empty_mask_should_return_empty_list() {
		String responseJson = """
				{
					"message": "Test",
					"masks" : [],
					"sop_instance_uid": "2.25.251867431509614238946512793485716204981"
				}
				""";
		String expectedUid = "2.25.251867431509614238946512793485716204981";

		List<MaskBody> maskBodies = this.deidentifyImageService.extractMasksFromJson(responseJson, expectedUid);

		assertThat(maskBodies).isEmpty();
	}

	@Test
	public void extractMasksFromJson_should_return_correct_masks() {
		String responseJson = """
				{
					"message": "Sensitive data detected",
					"masks": [
					{
						"stationName": "*",
						"color": "2e2d2d",
						"rectangles": [
							"229 16 69 19",
							"568 15 44 21"
						]
					},
					{
						"stationName": "*",
						"color": "202020",
						"rectangles": [
							"231 44 271 20"
						]
					}
					],
				  "sop_instance_uid": "2.25.251867431509614238946512793485716204981"
				}
				""";
		String expectedUid = "2.25.251867431509614238946512793485716204981";

		List<MaskBody> maskBodies = this.deidentifyImageService.extractMasksFromJson(responseJson, expectedUid);

		assertThat(maskBodies).hasSize(2);
		assertThat(maskBodies.getFirst().getStationName()).isEqualTo("*");
		assertThat(maskBodies.getFirst().getColor()).isEqualTo("2e2d2d");
		assertThat(maskBodies.getFirst().getRectangles()).containsExactly("229 16 69 19", "568 15 44 21");
		assertThat(maskBodies.get(1).getStationName()).isEqualTo("*");
		assertThat(maskBodies.get(1).getColor()).isEqualTo("202020");
		assertThat(maskBodies.get(1).getRectangles()).containsExactly("231 44 271 20");
	}

	@Test
	public void extractMasksFromJson_malformed_json_should_return_empty_list() {
		String malformedJson = "not json";

		List<MaskBody> maskBodies = this.deidentifyImageService.extractMasksFromJson(malformedJson, null);

		assertThat(maskBodies).isEmpty();
	}

	// Extract Pixel Data Bytes
	@Test
	public void extractPixelDataBytes_dcmAttribute_null_should_return_null() {
		byte[] extractedPixels = this.deidentifyImageService.extractPixelDataBytes(null);
		assertThat(extractedPixels).isNull();
	}

	@Test
	public void extractPixelDataBytes_pixeldata_absent_should_return_null() {
		Attributes attributes = new Attributes();

		byte[] extractedPixels = this.deidentifyImageService.extractPixelDataBytes(attributes);

		assertThat(extractedPixels).isNull();
	}

	@Test
	public void extractPixelDataBytes_bulkdata_return_correct() throws IOException {
		Attributes attributes = new Attributes();
		Path temp = this.tempDir.resolve("test.bin");
		byte[] pixelDataBytes = new byte[] { 1, 2, 3, 4 };
		Files.write(temp, pixelDataBytes);
		BulkData bulkData = new BulkData(temp.toUri().toString(), 0, pixelDataBytes.length, false);
		attributes.setValue(Tag.PixelData, VR.OW, bulkData);

		byte[] extractedPixels = this.deidentifyImageService.extractPixelDataBytes(attributes);

		assertThat(extractedPixels).isEqualTo(pixelDataBytes);
	}

	@Test
	public void extractPixelDataBytes_fragments_with_only_offset_table_return_null() {
		Attributes attributes = new Attributes();
		Fragments fragments = attributes.newFragments(Tag.PixelData, VR.OB, 1);
		fragments.add(null);

		byte[] extractedPixels = this.deidentifyImageService.extractPixelDataBytes(attributes);

		assertThat(extractedPixels).isNull();
	}

	@Test
	public void extractPixelDataBytes_fragments_return_correct() {
		Attributes attributes = new Attributes();
		byte[] pixelDataBytes = new byte[] { 1, 2, 3, 4 };
		Fragments fragments = attributes.newFragments(Tag.PixelData, VR.OB, 2);
		fragments.add(null);
		fragments.add(pixelDataBytes);

		byte[] extractedPixels = this.deidentifyImageService.extractPixelDataBytes(attributes);

		assertThat(extractedPixels).isEqualTo(pixelDataBytes);
	}

	@Test
	public void extractPixelDataBytes_fragments_unexpected_type_return_null() {
		Attributes attributes = new Attributes();
		String pixelData = "test";
		Fragments fragments = attributes.newFragments(Tag.PixelData, VR.OB, 2);
		fragments.add(null);
		fragments.add(pixelData);

		byte[] extractedPixels = this.deidentifyImageService.extractPixelDataBytes(attributes);

		assertThat(extractedPixels).isNull();
	}

	// Build Pattern Color LUT Json
	@Test
	public void buildPaletteColorLutJson_monochrome_should_return_null() {
		Attributes attributes = new Attributes();
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "MONOCHROME2");

		String builtPalette = this.deidentifyImageService.buildPaletteColorLutJson(attributes);

		assertThat(builtPalette).isNull();
	}

	@Test
	public void buildPaletteColorLutJson_palette_without_descriptor_tags_should_return_null() {
		Attributes attributes = new Attributes();
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "PALETTE COLOR");

		String builtPalette = this.deidentifyImageService.buildPaletteColorLutJson(attributes);

		assertThat(builtPalette).isNull();
	}

	@Test
	public void buildPaletteColorLutJson_palette_without_lut_data_tags_should_return_null() {
		Attributes attributes = new Attributes();
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "PALETTE COLOR");
		attributes.setInt(Tag.RedPaletteColorLookupTableDescriptor, VR.US, 1, 2, 8);
		attributes.setInt(Tag.GreenPaletteColorLookupTableDescriptor, VR.US, 1, 2, 8);
		attributes.setInt(Tag.BluePaletteColorLookupTableDescriptor, VR.US, 1, 2, 8);

		String builtPalette = this.deidentifyImageService.buildPaletteColorLutJson(attributes);

		assertThat(builtPalette).isNull();
	}

	@Test
	public void buildPaletteColorLutJson_palette_with_8bit() throws JsonProcessingException {
		byte[] redBytes = new byte[] { 10, 24, (byte) 200 };
		byte[] greenBytes = new byte[] { 20, 1, 0 };
		byte[] blueBytes = new byte[] { 100, 50, 12 };

		Attributes attributes = new Attributes();
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "PALETTE COLOR");
		attributes.setInt(Tag.RedPaletteColorLookupTableDescriptor, VR.US, 1, 2, 8);
		attributes.setInt(Tag.GreenPaletteColorLookupTableDescriptor, VR.US, 1, 2, 8);
		attributes.setInt(Tag.BluePaletteColorLookupTableDescriptor, VR.US, 1, 2, 8);
		attributes.setBytes(Tag.RedPaletteColorLookupTableData, VR.OB, redBytes);
		attributes.setBytes(Tag.GreenPaletteColorLookupTableData, VR.OB, greenBytes);
		attributes.setBytes(Tag.BluePaletteColorLookupTableData, VR.OB, blueBytes);

		String builtPalette = this.deidentifyImageService.buildPaletteColorLutJson(attributes);

		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, int[]> parsedJson = objectMapper.readValue(builtPalette, new TypeReference<>() {
		});

		assertThat(parsedJson.get("red")).containsExactly(10, 24, 200);
		assertThat(parsedJson.get("green")).containsExactly(20, 1, 0);
		assertThat(parsedJson.get("blue")).containsExactly(100, 50, 12);
	}

	@Test
	public void buildPaletteColorLutJson_palette_with_16bit() throws JsonProcessingException {
		int[] redInts = new int[] { 10, 24, 50 };
		int[] greenInts = new int[] { 20, 1, 0 };
		int[] blueInts = new int[] { 100, 50, 12 };

		Attributes attributes = new Attributes();
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "PALETTE COLOR");
		attributes.setInt(Tag.RedPaletteColorLookupTableDescriptor, VR.US, 1, 2, 16);
		attributes.setInt(Tag.GreenPaletteColorLookupTableDescriptor, VR.US, 1, 2, 16);
		attributes.setInt(Tag.BluePaletteColorLookupTableDescriptor, VR.US, 1, 2, 16);
		attributes.setInt(Tag.RedPaletteColorLookupTableData, VR.US, redInts);
		attributes.setInt(Tag.GreenPaletteColorLookupTableData, VR.US, greenInts);
		attributes.setInt(Tag.BluePaletteColorLookupTableData, VR.US, blueInts);

		String builtPalette = this.deidentifyImageService.buildPaletteColorLutJson(attributes);

		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, int[]> parsedJson = objectMapper.readValue(builtPalette, new TypeReference<>() {
		});

		assertThat(parsedJson.get("red")).containsExactly(10, 24, 50);
		assertThat(parsedJson.get("green")).containsExactly(20, 1, 0);
		assertThat(parsedJson.get("blue")).containsExactly(100, 50, 12);
	}

	// Generate Multipart Body
	@Test
	public void generateMultipartBody_with_jpeg() {
		Attributes attributes = new Attributes();
		byte[] imageByte = new byte[] { 10, 20, 30 };

		MultiValueMap<String, HttpEntity<?>> generatedBody = this.deidentifyImageService
			.generateMultipartBody(attributes, imageByte, "", "1.2.840.10008.1.2.4.50");

		HttpEntity<?> imagePart = generatedBody.getFirst("image");
		assertThat(imagePart).isNotNull();
		assertThat(imagePart.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);
		ByteArrayResource imageResource = (ByteArrayResource) imagePart.getBody();
		assertThat(imageResource).isNotNull();
		assertThat(imageResource.getFilename()).isEqualTo("image.jpg");
	}

	@Test
	public void generateMultipartBody_with_unknown_tsid_should_be_raw() {
		Attributes attributes = new Attributes();
		byte[] imageByte = new byte[] { 10, 20, 30 };

		MultiValueMap<String, HttpEntity<?>> generatedBody = this.deidentifyImageService
			.generateMultipartBody(attributes, imageByte, "", "unknown");

		HttpEntity<?> imagePart = generatedBody.getFirst("image");
		assertThat(imagePart).isNotNull();
		assertThat(imagePart.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
		ByteArrayResource imageResource = (ByteArrayResource) imagePart.getBody();
		assertThat(imageResource).isNotNull();
		assertThat(imageResource.getFilename()).isEqualTo("image.raw");
	}

	@Test
	public void generateMultipartBody_with_raw() {
		Attributes attributes = new Attributes();
		attributes.setDouble(Tag.RescaleSlope, VR.DS, 1.0);
		attributes.setDouble(Tag.RescaleIntercept, VR.DS, 2.0);
		attributes.setDouble(Tag.WindowCenter, VR.DS, 3.0);
		attributes.setDouble(Tag.WindowWidth, VR.DS, 4.0);
		byte[] imageByte = new byte[] { 10, 20, 30 };

		MultiValueMap<String, HttpEntity<?>> generatedBody = this.deidentifyImageService
			.generateMultipartBody(attributes, imageByte, "", "1.2.840.10008.1.2.1");

		HttpEntity<?> imagePart = generatedBody.getFirst("image");
		assertThat(imagePart).isNotNull();
		assertThat(imagePart.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);

		ByteArrayResource imageResource = (ByteArrayResource) imagePart.getBody();
		assertThat(imageResource).isNotNull();
		assertThat(imageResource.getFilename()).isEqualTo("image.raw");

		HttpEntity<?> rescaleSlopePart = generatedBody.getFirst("rescale_slope");
		assertThat(rescaleSlopePart).isNotNull();
		String rescaleSlopeBody = (String) rescaleSlopePart.getBody();
		assertThat(rescaleSlopeBody).isNotNull();
		double retrievedRescaleSlope = Double.parseDouble(rescaleSlopeBody);
		assertThat(retrievedRescaleSlope).isEqualTo(1.0);

		HttpEntity<?> rescaleInterceptPart = generatedBody.getFirst("rescale_intercept");
		assertThat(rescaleInterceptPart).isNotNull();
		String rescaleInterceptBody = (String) rescaleInterceptPart.getBody();
		assertThat(rescaleInterceptBody).isNotNull();
		double retrievedRescaleIntercept = Double.parseDouble(rescaleInterceptBody);
		assertThat(retrievedRescaleIntercept).isEqualTo(2.0);

		HttpEntity<?> windowCenterPart = generatedBody.getFirst("window_center");
		assertThat(windowCenterPart).isNotNull();
		String windowCenterBody = (String) windowCenterPart.getBody();
		assertThat(windowCenterBody).isNotNull();
		double retrievedWindowCenter = Double.parseDouble(windowCenterBody);
		assertThat(retrievedWindowCenter).isEqualTo(3.0);

		HttpEntity<?> windowWidthPart = generatedBody.getFirst("window_width");
		assertThat(windowWidthPart).isNotNull();
		String windowWidthBody = (String) windowWidthPart.getBody();
		assertThat(windowWidthBody).isNotNull();
		double retrievedWindowWidth = Double.parseDouble(windowWidthBody);
		assertThat(retrievedWindowWidth).isEqualTo(4.0);
	}

	@Test
	public void generateMultipartBody_with_raw_without_optional_tags_should_not_contain_optional_parts() {
		Attributes attributes = new Attributes(); // no rescale/window tags
		byte[] imageByte = new byte[] { 10, 20, 30 };

		MultiValueMap<String, HttpEntity<?>> generatedBody = this.deidentifyImageService
			.generateMultipartBody(attributes, imageByte, "", "1.2.840.10008.1.2.1");

		assertThat(generatedBody.getFirst("rescale_slope")).isNull();
		assertThat(generatedBody.getFirst("rescale_intercept")).isNull();
		assertThat(generatedBody.getFirst("window_center")).isNull();
		assertThat(generatedBody.getFirst("window_width")).isNull();
	}

	@Test
	public void generateMultipartBody_with_photometric_lut_data() throws JsonProcessingException {
		int[] redInts = new int[] { 10, 24, 200 };
		int[] greenInts = new int[] { 20, 1, 0 };
		int[] blueInts = new int[] { 100, 50, 12 };
		Attributes attributes = new Attributes();
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "PALETTE COLOR");
		attributes.setInt(Tag.RedPaletteColorLookupTableDescriptor, VR.US, 1, 2, 16);
		attributes.setInt(Tag.GreenPaletteColorLookupTableDescriptor, VR.US, 1, 2, 16);
		attributes.setInt(Tag.BluePaletteColorLookupTableDescriptor, VR.US, 1, 2, 16);
		attributes.setInt(Tag.RedPaletteColorLookupTableData, VR.US, redInts);
		attributes.setInt(Tag.GreenPaletteColorLookupTableData, VR.US, greenInts);
		attributes.setInt(Tag.BluePaletteColorLookupTableData, VR.US, blueInts);
		byte[] imageByte = new byte[] { 10, 20, 30 };

		MultiValueMap<String, HttpEntity<?>> generatedBody = this.deidentifyImageService
			.generateMultipartBody(attributes, imageByte, "", "1.2.840.10008.1.2.1");

		HttpEntity<?> palettePart = generatedBody.getFirst("palette_color_lut");
		assertThat(palettePart).isNotNull();
		String retrievedPaletteLut = (String) palettePart.getBody();
		assertThat(retrievedPaletteLut).isNotNull();
		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, int[]> parsedJson = objectMapper.readValue(retrievedPaletteLut, new TypeReference<>() {
		});

		assertThat(parsedJson.get("red")).containsExactly(10, 24, 200);
		assertThat(parsedJson.get("green")).containsExactly(20, 1, 0);
		assertThat(parsedJson.get("blue")).containsExactly(100, 50, 12);
	}

	@Test
	public void generateMultipartBody_correct_sop_instance_uid() {
		String sopInstanceUid = "1.2.3.4.5.6";
		Attributes attributes = new Attributes();
		attributes.setString(Tag.SOPInstanceUID, VR.UI, sopInstanceUid);

		byte[] imageByte = new byte[] { 10, 20, 30 };

		MultiValueMap<String, HttpEntity<?>> generatedBody = this.deidentifyImageService
			.generateMultipartBody(attributes, imageByte, "", "1.2.840.10008.1.2.1");

		HttpEntity<?> sopInstanceUidPart = generatedBody.getFirst("sop_instance_uid");
		assertThat(sopInstanceUidPart).isNotNull();
		String retrievedSopInstanceUid = (String) sopInstanceUidPart.getBody();
		assertThat(retrievedSopInstanceUid).isEqualTo(sopInstanceUid);
	}

}
