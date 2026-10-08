/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.dicom.result;

/**
 * Summary of the TLS handshake performed against an {@code https} DICOMweb endpoint: the
 * negotiated protocol and the server leaf certificate's identity, expiry and trust
 * status. Surfaces the classic silent failure where the port is open but an expired or
 * untrusted certificate breaks every transfer.
 *
 * @param protocol negotiated TLS protocol (e.g. {@code TLSv1.3})
 * @param subject leaf certificate subject distinguished name
 * @param issuer leaf certificate issuer distinguished name
 * @param notAfter expiry date of the leaf certificate (ISO-8601)
 * @param daysUntilExpiry whole days until expiry (negative once expired)
 * @param expired whether the certificate is outside its validity window now
 * @param trusted whether the chain validates against the default trust store
 */
public record TlsCertificateInfo(String protocol, String subject, String issuer, String notAfter, long daysUntilExpiry,
		boolean expired, boolean trusted) {

	public String getSummary() {
		String validity = this.expired ? "EXPIRED" : (this.daysUntilExpiry + " day(s) left");
		String trust = this.trusted ? "trusted" : "not trusted (self-signed or unknown CA)";
		return this.protocol + " — certificate expires " + this.notAfter + " (" + validity + "), " + trust;
	}

}