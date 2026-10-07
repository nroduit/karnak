/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.controller;

// import org.karnak.backend.constant.EndPoint;
// import org.karnak.backend.service.FileService;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.MediaType;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.bind.annotation.RestController;

// TODO: files will not be store locally but in a flow: deactivated: will be done later
/// ** Rest controller managing files */
// @RestController
// @RequestMapping(EndPoint.FILE_PATH)
// public class FileController {
//
//
// // Services
// private final FileService fileService;
//
// /**
// * Autowired constructor
// *
// * @param fileService Service managing files
// */
// @Autowired
// public FileController(final FileService fileService) {
// this.fileService = fileService;
// }
//
// /**
// * @param aet Aet to get files from
// * @param fileName File to download
// * @return if successful download the file otherwise display an error message
// */
// @GetMapping(
// value = {EndPoint.DOWNLOAD_SOPUID_PATH},
// produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
// public ResponseEntity<Object> downloadSopUidFromAetT(
// @RequestParam(value = EndPoint.AET_PARAM) String aet,
// @RequestParam(value = EndPoint.SOP_UID_PARAM) String fileName) {
// byte[] file;
//
// try {
// file = fileService.retrieveFileToDownload(aet, fileName);
// } catch (Exception e) {
// log.error("Unexpected exception when downloading", e);
// return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
// .body("Error when downloading the file: Internal server error => " + e.getMessage());
// }
//
// return file == null
// ? ResponseEntity.notFound().build()
// : ResponseEntity.status(HttpStatus.OK)
// .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
// .body(file);
// }
// }
