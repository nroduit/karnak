/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.notification;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import org.karnak.backend.constant.Notification;

/**
 * Model used for serie summary notification
 */
public class SerieSummaryNotification {

	private String serieUid;

	private String serieDescription;

	private LocalDateTime serieDate;

	private long nbTransferSent;

	private long nbTransferNotSent;

	private Set<String> unTransferedReasons;

	private Set<String> transferredModalities;

	private Set<String> transferredSopClassUid;

	public String getSerieUid() {
		return serieUid;
	}

	public void setSerieUid(String serieUid) {
		this.serieUid = serieUid;
	}

	public String getSerieDescription() {
		return serieDescription;
	}

	public void setSerieDescription(String serieDescription) {
		this.serieDescription = serieDescription;
	}

	public LocalDateTime getSerieDate() {
		return serieDate;
	}

	public void setSerieDate(LocalDateTime serieDate) {
		this.serieDate = serieDate;
	}

	public long getNbTransferSent() {
		return nbTransferSent;
	}

	public void setNbTransferSent(long nbTransferSent) {
		this.nbTransferSent = nbTransferSent;
	}

	public long getNbTransferNotSent() {
		return nbTransferNotSent;
	}

	public void setNbTransferNotSent(long nbTransferNotSent) {
		this.nbTransferNotSent = nbTransferNotSent;
	}

	public Set<String> getUnTransferedReasons() {
		return unTransferedReasons;
	}

	public void setUnTransferedReasons(Set<String> unTransferedReasons) {
		this.unTransferedReasons = unTransferedReasons;
	}

	public Set<String> getTransferredModalities() {
		return transferredModalities;
	}

	public void setTransferredModalities(Set<String> transferredModalities) {
		this.transferredModalities = transferredModalities;
	}

	public Set<String> getTransferredSopClassUid() {
		return transferredSopClassUid;
	}

	public void setTransferredSopClassUid(Set<String> transferredSopClassUid) {
		this.transferredSopClassUid = transferredSopClassUid;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SerieSummaryNotification that = (SerieSummaryNotification) o;
		return nbTransferSent == that.nbTransferSent && nbTransferNotSent == that.nbTransferNotSent
				&& Objects.equals(serieUid, that.serieUid) && Objects.equals(serieDescription, that.serieDescription)
				&& Objects.equals(serieDate, that.serieDate)
				&& Objects.equals(unTransferedReasons, that.unTransferedReasons)
				&& Objects.equals(transferredModalities, that.transferredModalities)
				&& Objects.equals(transferredSopClassUid, that.transferredSopClassUid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(serieUid, serieDescription, serieDate, nbTransferSent, nbTransferNotSent,
				unTransferedReasons, transferredModalities, transferredSopClassUid);
	}

	public String toStringUnTransferredReasons() {
		return String.join(Notification.COMMA_SEPARATOR, unTransferedReasons);
	}

	public String toStringTransferredModalities() {
		return String.join(Notification.COMMA_SEPARATOR, transferredModalities);
	}

	public String toStringTransferredSopClassUid() {
		return String.join(Notification.COMMA_SEPARATOR, transferredSopClassUid);
	}

}
