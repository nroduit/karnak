/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.dicom;

import com.sun.net.httpserver.HttpServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.weasis.dicom.param.AttributeEditor;
import org.weasis.dicom.web.HttpException;

@DisplayNameGeneration(ReplaceUnderscores.class)
class WebForwardDestinationTest {

	private static final String URL = "http://localhost:8080/dicomweb/studies";

	private static WebForwardDestination destination() {
		return new WebForwardDestination(new ForwardDicomNode("FWD-AET"), URL);
	}

	@Test
	void exposes_the_request_url() {
		assertEquals(URL, destination().getRequestURL());
	}

	@Test
	void to_string_is_the_request_url() {
		assertEquals(URL, destination().toString());
	}

	@Test
	void exposes_the_calling_forward_node() {
		ForwardDicomNode fwdNode = new ForwardDicomNode("FWD-AET");

		WebForwardDestination destination = new WebForwardDestination(fwdNode, URL);

		assertSame(fwdNode, destination.getForwardDicomNode());
	}

	@Test
	void provides_a_state_and_an_upload_manager() {
		WebForwardDestination destination = destination();

		assertNotNull(destination.getState());
		assertNotNull(destination.getStowrsSingleFile());
	}

	@Test
	void has_no_id_when_built_with_the_short_constructor() {
		assertNull(destination().getId());
	}

	@Test
	void keeps_the_attribute_editors_it_was_built_with() {
		AttributeEditor editor = (dcm, context) -> {
		};

		WebForwardDestination destination = new WebForwardDestination(new ForwardDicomNode("FWD-AET"), URL,
				List.of(editor));

		assertEquals(1, destination.getDicomEditors().size());
		assertSame(editor, destination.getDicomEditors().getFirst());
	}

	@Test
	void output_transfer_syntax_defaults_to_empty_and_normalises_null() {
		WebForwardDestination destination = destination();
		assertEquals("", destination.getOutputTransferSyntax());

		destination.setOutputTransferSyntax(UID.ExplicitVRLittleEndian);
		assertEquals(UID.ExplicitVRLittleEndian, destination.getOutputTransferSyntax());

		destination.setOutputTransferSyntax(null);
		assertEquals("", destination.getOutputTransferSyntax());
	}

	@Test
	void transcode_only_uncompressed_keeps_a_compressed_syntax_unchanged() {
		WebForwardDestination destination = destination();

		// Default transcodeOnlyUncompressed=true: a compressed, non-RLE syntax is
		// forwarded as-is.
		assertEquals(UID.JPEGBaseline8Bit, destination.getOutputTransferSyntax(UID.JPEGBaseline8Bit));
	}

	@Test
	void implicit_little_endian_is_promoted_to_explicit_little_endian() {
		WebForwardDestination destination = destination();

		assertEquals(UID.ExplicitVRLittleEndian, destination.getOutputTransferSyntax(UID.ImplicitVRLittleEndian));
	}

	@Test
	void transcode_only_uncompressed_can_be_disabled() {
		WebForwardDestination destination = destination();

		destination.setTranscodeOnlyUncompressed(false);

		assertFalse(destination.isTranscodeOnlyUncompressed());
	}

	@Test
	void a_redirected_upload_fails_instead_of_reporting_success() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		AtomicInteger loginPageHits = new AtomicInteger();
		server.createContext("/dicomweb/studies", exchange -> {
			exchange.getRequestBody().readAllBytes();
			exchange.getResponseHeaders().add("Location", "/login");
			exchange.sendResponseHeaders(302, -1);
			exchange.close();
		});
		server.createContext("/login", exchange -> {
			loginPageHits.incrementAndGet();
			byte[] body = "<html>login</html>".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();
		String url = "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort()
				+ "/dicomweb/studies";
		WebForwardDestination destination = new WebForwardDestination(new ForwardDicomNode("FWD-AET"), url);
		try {
			Attributes dataset = new Attributes();
			dataset.setString(Tag.SOPClassUID, VR.UI, UID.SecondaryCaptureImageStorage);
			dataset.setString(Tag.SOPInstanceUID, VR.UI, "1.2.3.4.5");

			HttpException e = assertThrows(HttpException.class,
					() -> destination.getStowrsSingleFile().uploadDicom(dataset, UID.ExplicitVRLittleEndian));

			assertEquals(302, e.getStatusCode());
			assertEquals(0, loginPageHits.get());
		}
		finally {
			destination.stop();
			server.stop(0);
		}
	}

}
