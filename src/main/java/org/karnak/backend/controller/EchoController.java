/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.karnak.backend.constant.EndPoint;
import org.karnak.backend.model.echo.DestinationEcho;
import org.karnak.backend.model.echo.DestinationEchos;
import org.karnak.backend.service.EchoService;
import org.karnak.backend.util.SpringDocUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest controller managing echo
 */
@RestController
@RequestMapping(EndPoint.ECHO_PATH)
@Tag(name = "Echo", description = "API Endpoints for Echo")
public class EchoController {

	// Services
	private final EchoService echoService;

	/**
	 * Autowired constructor
	 * @param echoService Service managing echo
	 */
	@Autowired
	public EchoController(final EchoService echoService) {
		this.echoService = echoService;
	}

	@Operation(summary = "Retrieve status destinations",
			description = "Retrieve status and configured destinations from an AeTitle", tags = "Echo")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Status and configured destinations found from an AeTitle",
					content = @Content(schema = @Schema(implementation = DestinationEchos.class),
							examples = @ExampleObject(name = "Example values status destinations",
									value = SpringDocUtil.EXAMPLE_VALUES_STATUS_DESTINATIONS_ECHO))),
			@ApiResponse(responseCode = "204", description = "No Content", content = @Content) })
	/**
	 * Retrieve the status of the configured destinations from the source AeTitle in
	 * parameter
	 * @param srcAet Source AeTitle to get the destination from
	 * @return Configured destinations with theirs status
	 */
	@GetMapping(value = { EndPoint.DESTINATIONS_PATH },
			produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<DestinationEchos> retrieveStatusConfiguredDestinations(
			@RequestParam(value = EndPoint.SRC_AET_PARAM) String srcAet) {

		// Call service to retrieve the status of the configured destinations
		List<DestinationEcho> destinationEchos = echoService.retrieveStatusConfiguredDestinations(srcAet);

		// If empty no content else list of destinations with status
		return destinationEchos.isEmpty() ? ResponseEntity.noContent().build()
				: ResponseEntity.ok(new DestinationEchos(destinationEchos));
	}

}
