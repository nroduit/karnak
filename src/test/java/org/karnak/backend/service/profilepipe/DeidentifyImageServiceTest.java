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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.BulkData;
import org.dcm4che3.data.Fragments;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.karnak.backend.model.profilebody.MaskBody;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;

class DeidentifyImageServiceTest {

	@TempDir
	Path tempDir;

	// Service
	DeidentifyImageService deidentifyImageService;

	@BeforeEach
	void setup() {
		this.deidentifyImageService = new DeidentifyImageService("http://localhost:8000");
	}

	// Extract mask from JSON
	@Test
	void extractMasksFromJson_null_params_should_return_empty_list() {
		List<MaskBody> maskBodies = this.deidentifyImageService.extractMasksFromJson(null, null);
		assertThat(maskBodies).isEmpty();
	}

	@Test
	void extractMasksFromJson_blank_json_should_return_empty_list() {
		List<MaskBody> maskBodies = this.deidentifyImageService.extractMasksFromJson("", null);
		assertThat(maskBodies).isEmpty();
	}

	static Stream<Arguments> emptyMasksCases() {
		return Stream.of(Arguments.of("""
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
				""", "2.25.251867431509614238946512793485716204981", "UID absent"), Arguments.of("""
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
				""", "2.25.251867431509614238946512793485716204980", "UID different"), Arguments.of("""
				{
					"message": "Test",
					"sop_instance_uid": "2.25.251867431509614238946512793485716204981"
				}
				""", "2.25.251867431509614238946512793485716204981", "No mask"), Arguments.of("""
				{
					"message": "Test",
					"masks" : [],
					"sop_instance_uid": "2.25.251867431509614238946512793485716204981"
				}
				""", "2.25.251867431509614238946512793485716204981", "Empty mask"));
	}

	@ParameterizedTest(name = "{index} - {2}")
	@MethodSource("emptyMasksCases")
	void extractMasksFromJson_valid_json_should_return_empty_list(String responseJson, String expectedUid,
			String description) {
		List<MaskBody> maskBodies = this.deidentifyImageService.extractMasksFromJson(responseJson, expectedUid);

		assertThat(maskBodies).isEmpty();
	}

	@Test
	void extractMasksFromJson_should_return_correct_masks() {
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
	void extractMasksFromJson_malformed_json_should_return_empty_list() {
		String malformedJson = "not json";

		List<MaskBody> maskBodies = this.deidentifyImageService.extractMasksFromJson(malformedJson, null);

		assertThat(maskBodies).isEmpty();
	}

	// Extract Pixel Data Bytes
	@Test
	void extractPixelDataBytes_dcmAttribute_null_should_return_empty_array() {
		byte[] extractedPixels = this.deidentifyImageService.extractPixelDataBytes(null);
		assertThat(extractedPixels).isEmpty();
	}

	@Test
	void extractPixelDataBytes_pixeldata_absent_should_return_empty_array() {
		Attributes attributes = new Attributes();

		byte[] extractedPixels = this.deidentifyImageService.extractPixelDataBytes(attributes);

		assertThat(extractedPixels).isEmpty();
	}

	@Test
	void extractPixelDataBytes_bulkdata_return_correct() throws IOException {
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
	void extractPixelDataBytes_fragments_with_only_offset_table_return_empty_array() {
		Attributes attributes = new Attributes();
		Fragments fragments = attributes.newFragments(Tag.PixelData, VR.OB, 1);
		fragments.add(null);

		byte[] extractedPixels = this.deidentifyImageService.extractPixelDataBytes(attributes);

		assertThat(extractedPixels).isEmpty();
	}

	@Test
	void extractPixelDataBytes_fragments_return_correct() {
		Attributes attributes = new Attributes();
		byte[] pixelDataBytes = new byte[] { 1, 2, 3, 4 };
		Fragments fragments = attributes.newFragments(Tag.PixelData, VR.OB, 2);
		fragments.add(null);
		fragments.add(pixelDataBytes);

		byte[] extractedPixels = this.deidentifyImageService.extractPixelDataBytes(attributes);

		assertThat(extractedPixels).isEqualTo(pixelDataBytes);
	}

	@Test
	void extractPixelDataBytes_fragments_unexpected_type_return_empty_array() {
		Attributes attributes = new Attributes();
		String pixelData = "test";
		Fragments fragments = attributes.newFragments(Tag.PixelData, VR.OB, 2);
		fragments.add(null);
		fragments.add(pixelData);

		byte[] extractedPixels = this.deidentifyImageService.extractPixelDataBytes(attributes);

		assertThat(extractedPixels).isEmpty();
	}

	// Build Pattern Color LUT Json
	@Test
	void buildPaletteColorLutJson_monochrome_should_return_null() {
		Attributes attributes = new Attributes();
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "MONOCHROME2");

		String builtPalette = this.deidentifyImageService.buildPaletteColorLutJson(attributes);

		assertThat(builtPalette).isNull();
	}

	@Test
	void buildPaletteColorLutJson_palette_without_descriptor_tags_should_return_null() {
		Attributes attributes = new Attributes();
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "PALETTE COLOR");

		String builtPalette = this.deidentifyImageService.buildPaletteColorLutJson(attributes);

		assertThat(builtPalette).isNull();
	}

	@Test
	void buildPaletteColorLutJson_palette_without_lut_data_tags_should_return_null() {
		Attributes attributes = new Attributes();
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "PALETTE COLOR");
		attributes.setInt(Tag.RedPaletteColorLookupTableDescriptor, VR.US, 3, 2, 8);
		attributes.setInt(Tag.GreenPaletteColorLookupTableDescriptor, VR.US, 3, 2, 8);
		attributes.setInt(Tag.BluePaletteColorLookupTableDescriptor, VR.US, 3, 2, 8);

		String builtPalette = this.deidentifyImageService.buildPaletteColorLutJson(attributes);

		assertThat(builtPalette).isNull();
	}

	@Test
	void buildPaletteColorLutJson_palette_with_malformed_descriptor_should_return_null() {
		Attributes attributes = new Attributes();
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "PALETTE COLOR");
		// A descriptor must hold exactly 3 values
		attributes.setInt(Tag.RedPaletteColorLookupTableDescriptor, VR.US, 3, 2);
		attributes.setInt(Tag.GreenPaletteColorLookupTableDescriptor, VR.US, 3, 2, 8);
		attributes.setInt(Tag.BluePaletteColorLookupTableDescriptor, VR.US, 3, 2, 8);
		attributes.setBytes(Tag.RedPaletteColorLookupTableData, VR.OB, new byte[] { 10, 24, 50 });
		attributes.setBytes(Tag.GreenPaletteColorLookupTableData, VR.OB, new byte[] { 20, 1, 0 });
		attributes.setBytes(Tag.BluePaletteColorLookupTableData, VR.OB, new byte[] { 100, 50, 12 });

		String builtPalette = this.deidentifyImageService.buildPaletteColorLutJson(attributes);

		assertThat(builtPalette).isNull();
	}

	@Test
	void buildPaletteColorLutJson_palette_with_truncated_lut_data_should_return_null() {
		Attributes attributes = new Attributes();
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "PALETTE COLOR");
		// Descriptors declare 4 entries but only 3 are stored
		attributes.setInt(Tag.RedPaletteColorLookupTableDescriptor, VR.US, 4, 2, 16);
		attributes.setInt(Tag.GreenPaletteColorLookupTableDescriptor, VR.US, 4, 2, 16);
		attributes.setInt(Tag.BluePaletteColorLookupTableDescriptor, VR.US, 4, 2, 16);
		attributes.setInt(Tag.RedPaletteColorLookupTableData, VR.US, 10, 24, 50);
		attributes.setInt(Tag.GreenPaletteColorLookupTableData, VR.US, 20, 1, 0);
		attributes.setInt(Tag.BluePaletteColorLookupTableData, VR.US, 100, 50, 12);

		String builtPalette = this.deidentifyImageService.buildPaletteColorLutJson(attributes);

		assertThat(builtPalette).isNull();
	}

	@Test
	void buildPaletteColorLutJson_palette_with_padded_8bit_lut_should_drop_padding() throws JsonProcessingException {
		// An 8-bit LUT segment holding an odd number of entries is padded to an even
		// length: the extra byte must be dropped, not rejected.
		byte[] redBytes = new byte[] { 10, 24, (byte) 200, 0 };
		byte[] greenBytes = new byte[] { 20, 1, 5, 0 };
		byte[] blueBytes = new byte[] { 100, 50, 12, 0 };

		Attributes attributes = new Attributes();
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "PALETTE COLOR");
		attributes.setInt(Tag.RedPaletteColorLookupTableDescriptor, VR.US, 3, 2, 8);
		attributes.setInt(Tag.GreenPaletteColorLookupTableDescriptor, VR.US, 3, 2, 8);
		attributes.setInt(Tag.BluePaletteColorLookupTableDescriptor, VR.US, 3, 2, 8);
		attributes.setBytes(Tag.RedPaletteColorLookupTableData, VR.OB, redBytes);
		attributes.setBytes(Tag.GreenPaletteColorLookupTableData, VR.OB, greenBytes);
		attributes.setBytes(Tag.BluePaletteColorLookupTableData, VR.OB, blueBytes);

		String builtPalette = this.deidentifyImageService.buildPaletteColorLutJson(attributes);

		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, int[]> parsedJson = objectMapper.readValue(builtPalette, new TypeReference<>() {
		});

		assertThat(parsedJson.get("red")).containsExactly(10, 24, 200);
		assertThat(parsedJson.get("green")).containsExactly(20, 1, 5);
		assertThat(parsedJson.get("blue")).containsExactly(100, 50, 12);
	}

	@Test
	void buildPaletteColorLutJson_palette_with_8bit() throws JsonProcessingException {
		byte[] redBytes = new byte[] { 10, 24, (byte) 200 };
		byte[] greenBytes = new byte[] { 20, 1, 0 };
		byte[] blueBytes = new byte[] { 100, 50, 12 };

		Attributes attributes = new Attributes();
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "PALETTE COLOR");
		attributes.setInt(Tag.RedPaletteColorLookupTableDescriptor, VR.US, 3, 2, 8);
		attributes.setInt(Tag.GreenPaletteColorLookupTableDescriptor, VR.US, 3, 2, 8);
		attributes.setInt(Tag.BluePaletteColorLookupTableDescriptor, VR.US, 3, 2, 8);
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
	void buildPaletteColorLutJson_palette_with_16bit() throws JsonProcessingException {
		int[] redInts = new int[] { 10, 24, 50 };
		int[] greenInts = new int[] { 20, 1, 0 };
		int[] blueInts = new int[] { 100, 50, 12 };

		Attributes attributes = new Attributes();
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "PALETTE COLOR");
		attributes.setInt(Tag.RedPaletteColorLookupTableDescriptor, VR.US, 3, 2, 16);
		attributes.setInt(Tag.GreenPaletteColorLookupTableDescriptor, VR.US, 3, 2, 16);
		attributes.setInt(Tag.BluePaletteColorLookupTableDescriptor, VR.US, 3, 2, 16);
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
	void generateMultipartBody_with_jpeg() {
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
	void generateMultipartBody_with_unknown_tsid_should_be_raw() {
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
	void generateMultipartBody_with_raw() {
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
	void generateMultipartBody_with_raw_without_optional_tags_should_not_contain_optional_parts() {
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
	void generateMultipartBody_with_photometric_lut_data() throws JsonProcessingException {
		int[] redInts = new int[] { 10, 24, 200 };
		int[] greenInts = new int[] { 20, 1, 0 };
		int[] blueInts = new int[] { 100, 50, 12 };
		Attributes attributes = new Attributes();
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "PALETTE COLOR");
		attributes.setInt(Tag.SamplesPerPixel, VR.US, 1);
		attributes.setInt(Tag.RedPaletteColorLookupTableDescriptor, VR.US, 3, 2, 16);
		attributes.setInt(Tag.GreenPaletteColorLookupTableDescriptor, VR.US, 3, 2, 16);
		attributes.setInt(Tag.BluePaletteColorLookupTableDescriptor, VR.US, 3, 2, 16);
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

		// The photometric interpretation is left untouched when the LUT is usable
		assertThat(partBody(generatedBody, "photometric_interpretation")).isEqualTo("PALETTE COLOR");
		assertThat(partBody(generatedBody, "samples_per_pixel")).isEqualTo("1");
	}

	@Test
	void generateMultipartBody_with_raw_palette_color_without_lut_should_fall_back_to_monochrome2() {
		Attributes attributes = new Attributes();
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "PALETTE COLOR");
		attributes.setInt(Tag.SamplesPerPixel, VR.US, 1);
		byte[] imageByte = new byte[] { 10, 20, 30 };

		MultiValueMap<String, HttpEntity<?>> generatedBody = this.deidentifyImageService
			.generateMultipartBody(attributes, imageByte, "", "1.2.840.10008.1.2.1");

		assertThat(partBody(generatedBody, "photometric_interpretation")).isEqualTo("MONOCHROME2");
		assertThat(partBody(generatedBody, "samples_per_pixel")).isEqualTo("1");
		assertThat(generatedBody.getFirst("palette_color_lut")).isNull();
	}

	@Test
	void generateMultipartBody_with_raw_palette_color_with_truncated_lut_should_fall_back_to_monochrome2() {
		Attributes attributes = new Attributes();
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "PALETTE COLOR");
		attributes.setInt(Tag.SamplesPerPixel, VR.US, 1);
		attributes.setInt(Tag.RedPaletteColorLookupTableDescriptor, VR.US, 4, 2, 16);
		attributes.setInt(Tag.GreenPaletteColorLookupTableDescriptor, VR.US, 4, 2, 16);
		attributes.setInt(Tag.BluePaletteColorLookupTableDescriptor, VR.US, 4, 2, 16);
		attributes.setInt(Tag.RedPaletteColorLookupTableData, VR.US, 10, 24, 200);
		attributes.setInt(Tag.GreenPaletteColorLookupTableData, VR.US, 20, 1, 0);
		attributes.setInt(Tag.BluePaletteColorLookupTableData, VR.US, 100, 50, 12);
		byte[] imageByte = new byte[] { 10, 20, 30 };

		MultiValueMap<String, HttpEntity<?>> generatedBody = this.deidentifyImageService
			.generateMultipartBody(attributes, imageByte, "", "1.2.840.10008.1.2.1");

		assertThat(partBody(generatedBody, "photometric_interpretation")).isEqualTo("MONOCHROME2");
		assertThat(generatedBody.getFirst("palette_color_lut")).isNull();
	}

	@Test
	void generateMultipartBody_with_raw_palette_color_without_lut_should_force_samples_per_pixel_to_one() {
		Attributes attributes = new Attributes();
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "PALETTE COLOR");
		// Inconsistent value: a PALETTE COLOR image stores a single channel of indexes
		attributes.setInt(Tag.SamplesPerPixel, VR.US, 3);
		byte[] imageByte = new byte[] { 10, 20, 30 };

		MultiValueMap<String, HttpEntity<?>> generatedBody = this.deidentifyImageService
			.generateMultipartBody(attributes, imageByte, "", "1.2.840.10008.1.2.1");

		assertThat(partBody(generatedBody, "photometric_interpretation")).isEqualTo("MONOCHROME2");
		assertThat(partBody(generatedBody, "samples_per_pixel")).isEqualTo("1");
	}

	@Test
	void generateMultipartBody_with_compressed_palette_color_without_lut_should_keep_photometric() {
		Attributes attributes = new Attributes();
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "PALETTE COLOR");
		attributes.setInt(Tag.SamplesPerPixel, VR.US, 1);
		byte[] imageByte = new byte[] { 10, 20, 30 };

		MultiValueMap<String, HttpEntity<?>> generatedBody = this.deidentifyImageService
			.generateMultipartBody(attributes, imageByte, "", "1.2.840.10008.1.2.4.50");

		// The codestream carries its own color information: no normalization needed
		assertThat(partBody(generatedBody, "photometric_interpretation")).isEqualTo("PALETTE COLOR");
		assertThat(generatedBody.getFirst("palette_color_lut")).isNull();
	}

	@Test
	void generateMultipartBody_with_raw_monochrome_should_keep_photometric() {
		Attributes attributes = new Attributes();
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "MONOCHROME1");
		attributes.setInt(Tag.SamplesPerPixel, VR.US, 1);
		byte[] imageByte = new byte[] { 10, 20, 30 };

		MultiValueMap<String, HttpEntity<?>> generatedBody = this.deidentifyImageService
			.generateMultipartBody(attributes, imageByte, "", "1.2.840.10008.1.2.1");

		assertThat(partBody(generatedBody, "photometric_interpretation")).isEqualTo("MONOCHROME1");
		assertThat(partBody(generatedBody, "samples_per_pixel")).isEqualTo("1");
		assertThat(partBody(generatedBody, "is_monochrome1")).isEqualTo("true");
	}

	@Test
	void generateMultipartBody_with_raw_monochrome2_should_not_flag_monochrome1() {
		Attributes attributes = new Attributes();
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "MONOCHROME2");
		attributes.setInt(Tag.SamplesPerPixel, VR.US, 1);
		byte[] imageByte = new byte[] { 10, 20, 30 };

		MultiValueMap<String, HttpEntity<?>> generatedBody = this.deidentifyImageService
			.generateMultipartBody(attributes, imageByte, "", "1.2.840.10008.1.2.1");

		assertThat(partBody(generatedBody, "is_monochrome1")).isEqualTo("false");
	}

	@Test
	void generateMultipartBody_with_compressed_monochrome1_should_not_add_the_flag() {
		Attributes attributes = new Attributes();
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "MONOCHROME1");
		attributes.setInt(Tag.SamplesPerPixel, VR.US, 1);
		byte[] imageByte = new byte[] { 10, 20, 30 };

		MultiValueMap<String, HttpEntity<?>> generatedBody = this.deidentifyImageService
			.generateMultipartBody(attributes, imageByte, "", "1.2.840.10008.1.2.4.50");

		// The flag only drives the raw decoding path of the API
		assertThat(generatedBody.getFirst("is_monochrome1")).isNull();
	}

	private static String partBody(MultiValueMap<String, HttpEntity<?>> body, String partName) {
		HttpEntity<?> part = body.getFirst(partName);
		assertThat(part).isNotNull();
		return (String) part.getBody();
	}

	private static byte[] imagePartBytes(MultiValueMap<String, HttpEntity<?>> body) {
		HttpEntity<?> imagePart = body.getFirst("image");
		assertThat(imagePart).isNotNull();
		ByteArrayResource imageResource = (ByteArrayResource) imagePart.getBody();
		assertThat(imageResource).isNotNull();
		return imageResource.getByteArray();
	}

	// First raw frame
	@Test
	void firstRawFrame_matching_geometry_should_return_the_buffer() {
		byte[] imageBytes = new byte[] { 1, 2, 3, 4 };

		byte[] frame = this.deidentifyImageService.firstRawFrame(imageBytes, 2, 2, 8, 1, "1.2.3");

		assertThat(frame).isSameAs(imageBytes);
	}

	@Test
	void firstRawFrame_unknown_geometry_should_return_the_buffer() {
		byte[] imageBytes = new byte[] { 1, 2, 3, 4 };

		byte[] frame = this.deidentifyImageService.firstRawFrame(imageBytes, 0, 0, 0, 0, "1.2.3");

		assertThat(frame).isSameAs(imageBytes);
	}

	@Test
	void firstRawFrame_multi_frame_should_keep_the_first_frame() {
		byte[] imageBytes = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };

		byte[] frame = this.deidentifyImageService.firstRawFrame(imageBytes, 2, 2, 8, 1, "1.2.3");

		assertThat(frame).containsExactly(1, 2, 3, 4);
	}

	@Test
	void firstRawFrame_16bit_should_use_two_bytes_per_sample() {
		byte[] imageBytes = new byte[8];

		byte[] frame = this.deidentifyImageService.firstRawFrame(imageBytes, 2, 2, 16, 1, "1.2.3");

		assertThat(frame).hasSize(8);
	}

	@Test
	void firstRawFrame_buffer_smaller_than_a_frame_should_throw() {
		byte[] imageBytes = new byte[] { 1, 2, 3 };

		assertThatThrownBy(() -> this.deidentifyImageService.firstRawFrame(imageBytes, 2, 2, 8, 1, "1.2.3"))
			.isInstanceOf(DeidentifyImageException.class)
			.hasMessageContaining("1.2.3")
			.hasMessageContaining("4 bytes");
	}

	@Test
	void generateMultipartBody_with_raw_multi_frame_should_send_the_first_frame_only() {
		Attributes attributes = new Attributes();
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "MONOCHROME2");
		attributes.setInt(Tag.Rows, VR.US, 2);
		attributes.setInt(Tag.Columns, VR.US, 2);
		attributes.setInt(Tag.BitsAllocated, VR.US, 8);
		attributes.setInt(Tag.SamplesPerPixel, VR.US, 1);
		attributes.setInt(Tag.NumberOfFrames, VR.IS, 2);
		byte[] imageByte = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };

		MultiValueMap<String, HttpEntity<?>> generatedBody = this.deidentifyImageService
			.generateMultipartBody(attributes, imageByte, "", "1.2.840.10008.1.2.1");

		assertThat(imagePartBytes(generatedBody)).containsExactly(1, 2, 3, 4);
	}

	@Test
	void generateMultipartBody_with_compressed_multi_frame_should_send_the_buffer_as_is() {
		Attributes attributes = new Attributes();
		attributes.setInt(Tag.Rows, VR.US, 2);
		attributes.setInt(Tag.Columns, VR.US, 2);
		attributes.setInt(Tag.BitsAllocated, VR.US, 8);
		attributes.setInt(Tag.SamplesPerPixel, VR.US, 1);
		byte[] imageByte = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };

		MultiValueMap<String, HttpEntity<?>> generatedBody = this.deidentifyImageService
			.generateMultipartBody(attributes, imageByte, "", "1.2.840.10008.1.2.4.50");

		assertThat(imagePartBytes(generatedBody)).containsExactly(1, 2, 3, 4, 5, 6, 7, 8);
	}

	@Test
	void generateMultipartBody_with_palette_color_fallback_should_size_the_frame_on_one_sample() {
		Attributes attributes = new Attributes();
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "PALETTE COLOR");
		attributes.setInt(Tag.Rows, VR.US, 2);
		attributes.setInt(Tag.Columns, VR.US, 2);
		attributes.setInt(Tag.BitsAllocated, VR.US, 8);
		// Inconsistent value: the frame must be sized with the forced single sample
		attributes.setInt(Tag.SamplesPerPixel, VR.US, 3);
		byte[] imageByte = new byte[] { 1, 2, 3, 4 };

		MultiValueMap<String, HttpEntity<?>> generatedBody = this.deidentifyImageService
			.generateMultipartBody(attributes, imageByte, "", "1.2.840.10008.1.2.1");

		assertThat(partBody(generatedBody, "photometric_interpretation")).isEqualTo("MONOCHROME2");
		assertThat(partBody(generatedBody, "samples_per_pixel")).isEqualTo("1");
		assertThat(imagePartBytes(generatedBody)).containsExactly(1, 2, 3, 4);
	}

	@Test
	void generateMultipartBody_correct_sop_instance_uid() {
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
