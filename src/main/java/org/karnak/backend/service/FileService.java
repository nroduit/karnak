/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

// TODO: files will not be store locally but in a flow: deactivated: will be done later
/// ** Service managing files */
// @Service
// public class FileService {
//
//
// // Service
// private final GatewaySetUpService gatewaySetUpService;
//
// @Autowired
// public FileService(final GatewaySetUpService gatewaySetUpService) {
// this.gatewaySetUpService = gatewaySetUpService;
// }
//
// /**
// * Retrieve the file to download
// *
// * @param aet AeTitle
// * @param fileName File name
// * @return File to download
// * @throws IOException IO Exception
// * @throws IllegalStateException Illegal State Exception
// */
// public byte[] retrieveFileToDownload(String aet, String fileName)
// throws IOException, IllegalStateException {
//
// // Stored Path to get file from
// final Path archiveDir = gatewaySetUpService.getStorePath();
//
// // If path is not valid throw an exception
// if (archiveDir == null || !Files.isDirectory(archiveDir) ||
// !Files.isReadable(archiveDir)) {
// throw new IllegalStateException("Cannot access to the archive directory");
// } else {
// if (aet != null && fileName != null) {
// Path path = Path.of(archiveDir.toString(), aet, fileName);
// if (path == null || !Files.isReadable(path)) {
// log.warn("Cannot get this file for downloading: {}", path);
// } else {
// // Return the file
// DataInputStream in = new DataInputStream(Files.newInputStream(path));
// return IOUtils.toByteArray(in);
// }
// }
// }
// return null;
// }
// }
