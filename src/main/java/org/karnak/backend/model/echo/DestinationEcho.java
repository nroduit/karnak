/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.echo;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;

/**
 * Model for destination in echo controller
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DestinationEcho {

	// AeTitle of the destination dicom
	private String aet;

	// Url of the destination stow
	private String url;

	// Status
	private int status;

	/**
	 * Constructor without parameter
	 */
	public DestinationEcho() {
	}

	/**
	 * Constructor with parameters
	 * @param aet AeTitle
	 * @param url Url
	 * @param status Status
	 */
	public DestinationEcho(String aet, String url, int status) {
		this.aet = aet;
		this.url = url;
		this.status = status;
	}

	public String getAet() {
		return aet;
	}

	public void setAet(String aet) {
		this.aet = aet;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DestinationEcho that = (DestinationEcho) o;
		return Objects.equals(aet, that.aet) && Objects.equals(url, that.url) && Objects.equals(status, that.status);
	}

	@Override
	public int hashCode() {
		return Objects.hash(aet, url, status);
	}

}
