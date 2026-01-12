/*
 * Copyright (c) 2022 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.monitoring;

import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.QuerySortOrderBuilder;
import java.util.List;
import org.karnak.frontend.monitoring.component.TransferStatusFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.vaadin.artur.spring.dataprovider.FilterablePageableDataProvider;

@Component
public class TransferStatusDataProvider<T> extends FilterablePageableDataProvider<T, TransferStatusFilter> {

	// Services
	private final MonitoringLogic monitoringLogic;

	// Default sort order
	private final List<QuerySortOrder> defaultSortOrders;

	@Autowired
	public TransferStatusDataProvider(MonitoringLogic monitoringLogic) {
		this.monitoringLogic = monitoringLogic;

		// Default sort order
		QuerySortOrderBuilder builder = new QuerySortOrderBuilder();
		builder.thenDesc("transferDate");
		this.defaultSortOrders = builder.build();
	}

	@Override
	protected Page<T> fetchFromBackEnd(Query<T, TransferStatusFilter> query, Pageable pageable) {
		TransferStatusFilter filter = query.getFilter().orElse(new TransferStatusFilter());
		return (Page<T>) monitoringLogic.retrieveTransferStatus(filter, pageable);
	}

	@Override
	protected List<QuerySortOrder> getDefaultSortOrders() {
		return defaultSortOrders;
	}

	@Override
	protected int sizeInBackEnd(Query<T, TransferStatusFilter> query) {
		TransferStatusFilter filter = query.getFilter().orElse(new TransferStatusFilter());
		return monitoringLogic.countTransferStatus(filter);
	}

}
