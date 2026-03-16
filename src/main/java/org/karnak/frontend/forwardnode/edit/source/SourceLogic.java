/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.forwardnode.edit.source;

import com.vaadin.flow.data.provider.ListDataProvider;
import java.util.HashSet;
import java.util.Objects;
import org.karnak.backend.data.entity.DicomSourceNodeEntity;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.model.event.NodeEvent;
import org.karnak.backend.service.SourceNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Logic service use to make calls to backend and implement logic linked to the view
 */
@Service
public class SourceLogic extends ListDataProvider<DicomSourceNodeEntity> {

	// View
	private SourceView sourceView;

	// Services
	private final transient SourceNodeService sourceNodeService;

	/**
	 * Text filter that can be changed separately.
	 */
	private String filterText;

	private ForwardNodeEntity forwardNodeEntity; // Current forward node

	/**
	 * Autowired constructor
	 * @param sourceNodeService SourceNode Service
	 */
	@Autowired
	public SourceLogic(final SourceNodeService sourceNodeService) {
		super(new HashSet<>());
		this.sourceNodeService = sourceNodeService;
		this.sourceView = null;
		this.forwardNodeEntity = null;
		this.filterText = "";
	}

	@Override
	public Object getId(DicomSourceNodeEntity data) {
		Objects.requireNonNull(data, "Cannot provide an id for a null item.");
		return data.hashCode();
	}

	@Override
	public void refreshAll() {
		getItems().clear();
		if (forwardNodeEntity != null) {
			getItems().addAll(forwardNodeEntity.getSourceNodes());
		}
		super.refreshAll();
	}

	/**
	 * Sets the filter to use for this data provider and refreshes data.
	 *
	 * <p>
	 * Filter is compared for allowed properties.
	 * @param filterTextInput the text to filter by, never null.
	 */
	public void setFilter(String filterTextInput) {
		Objects.requireNonNull(filterText, "Filter text cannot be null.");

		final String filterTextInputTrim = filterTextInput.trim();

		if (Objects.equals(this.filterText, filterTextInputTrim)) {
			return;
		}
		this.filterText = filterTextInputTrim;

		setFilter(data -> matchesFilter(data, filterTextInputTrim));
	}

	private boolean matchesFilter(DicomSourceNodeEntity data, String filterText) {
		return data != null && data.matchesFilter(filterText);
	}

	public SourceView getSourceNodesView() {
		return sourceView;
	}

	public void setSourceNodesView(SourceView sourceView) {
		this.sourceView = sourceView;
	}

	public void loadForwardNode(ForwardNodeEntity forwardNodeEntity) {
		this.forwardNodeEntity = forwardNodeEntity;
		getItems().clear();
		getItems().addAll(sourceNodeService.retrieveSourceNodes(this.forwardNodeEntity));
	}

	/**
	 * Save the source
	 * @param dicomSourceNodeEntity source to save
	 */
	public void saveSourceNode(DicomSourceNodeEntity dicomSourceNodeEntity) {
		sourceNodeService.save(forwardNodeEntity, dicomSourceNodeEntity);
		refreshAll();
	}

	public void publishEvent(NodeEvent nodeEvent) {
		sourceNodeService.getApplicationEventPublisher().publishEvent(nodeEvent);
	}

	/**
	 * Delete the source in parameter
	 * @param dicomSourceNodeEntity source to delete
	 */
	public void deleteSourceNode(DicomSourceNodeEntity dicomSourceNodeEntity) {
		sourceNodeService.delete(dicomSourceNodeEntity);
		refreshAll();
	}

}
