/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.forwardnode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.provider.ListDataProvider;
import java.util.ArrayList;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.service.ForwardNodeAPIService;
import org.karnak.backend.service.ForwardNodeService;
import org.karnak.backend.service.ProjectService;
import org.karnak.backend.service.SOPClassUIDService;
import org.karnak.frontend.forwardnode.edit.destination.DestinationLogic;
import org.karnak.frontend.forwardnode.edit.source.SourceLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Logic service use to make calls to backend and implement logic linked to the view
 */
@Service
@Slf4j
public class ForwardNodeLogic extends ListDataProvider<ForwardNodeEntity> {

	@Setter
	@Getter
	private ForwardNodeView forwardNodeView;

	// Services
	private final ForwardNodeAPIService forwardNodeAPIService;

	private final transient ForwardNodeService forwardNodeService;

	@Getter
	private final transient ProjectService projectService;

	@Getter
	private final SOPClassUIDService sopClassUIDService;

	@Getter
	private final SourceLogic sourceLogic;

	@Getter
	private final DestinationLogic destinationLogic;

	/**
	 * Text filter that can be changed separately.
	 */
	private String filterText;

	@Autowired
	public ForwardNodeLogic(final ForwardNodeAPIService forwardNodeAPIService,
			final ForwardNodeService forwardNodeService, final ProjectService projectService,
			final SOPClassUIDService sopClassUIDService, final SourceLogic sourceLogic,
			final DestinationLogic destinationLogic) {
		super(new ArrayList<>());
		this.forwardNodeAPIService = forwardNodeAPIService;
		this.forwardNodeService = forwardNodeService;
		this.projectService = projectService;
		this.sopClassUIDService = sopClassUIDService;
		this.sourceLogic = sourceLogic;
		this.destinationLogic = destinationLogic;
		this.filterText = "";
		this.forwardNodeView = null;

		initDataProvider();
	}

	@Override
	public void refreshAll() {
		getItems().clear();
		getItems().addAll(forwardNodeService.getAllForwardNodes());
		super.refreshAll();
	}

	@Override
	public Long getId(ForwardNodeEntity data) {
		Objects.requireNonNull(data, "Cannot provide an id for a null item.");
		return data.getId();
	}

	/**
	 * Initialize the data provider
	 */
	private void initDataProvider() {
		getItems().addAll(forwardNodeService.getAllForwardNodes());
	}

	/**
	 * Update the fragment without causing navigator to change view
	 */
	private void setFragmentParameter(String dataIdStr) {
		final String fragmentParameter;
		if (dataIdStr == null || dataIdStr.isEmpty()) {
			fragmentParameter = "";
		}
		else {
			fragmentParameter = dataIdStr;
		}
		UI.getCurrent().navigate(ForwardNodeView.class, fragmentParameter);
	}

	public Long enter(String dataIdStr) {
		// TODO: On enter, go to dataIdStr
		try {
			return Long.valueOf(dataIdStr);
		}
		catch (NumberFormatException e) {
			log.error("Cannot get valueOf {}", dataIdStr, e);
		}
		return null;
		/*
		 * if (dataIdStr != null && !dataIdStr.isEmpty()) { // Ensure this is selected
		 * even if coming directly here from login try { Long dataId =
		 * Long.valueOf(dataIdStr); ForwardNodeEntity data = findForwardNode(dataId);
		 * gatewayView.selectRow(data); } catch (NumberFormatException e) { } } else {
		 * gatewayView.showForm(false); }
		 */
	}

	public void editForwardNode(ForwardNodeEntity data) {
		if (data == null) {
			setFragmentParameter("");
		}
		else {
			setFragmentParameter(String.valueOf(data.getId()));
		}
	}

	public void cancelForwardNode() {
		setFragmentParameter("");
	}

	public void saveForwardNode(ForwardNodeEntity data) {
		/*
		 * boolean newData = data.isNewData(); gatewayView.clearSelection();
		 * gatewayView.updateForwardNode(data); setFragmentParameter("");
		 * gatewayView.showSaveNotification(data.getFwdAeTitle() + (newData ? " created" :
		 * " updated")); //editForwardNode(data); //if you dont't want to exit the
		 * selection after saving a forward node. editForwardNode(null); //if you want to
		 * exit the selection after saving a forward node.
		 */
	}

	// public void deleteForwardNode(ForwardNodeEntity data) {
	/*
	 * gatewayView.clearSelection(); gatewayView.removeForwardNode(data);
	 * setFragmentParameter(""); gatewayView.showSaveNotification(data.getFwdAeTitle() +
	 * " removed");
	 */
	// setFragmentParameter("");
	// }

	/**
	 * Retrieve forward node depending on its id
	 * @param idForwardNode id of the forward node to retrieve
	 * @return Forward node found
	 */
	public ForwardNodeEntity retrieveForwardNodeById(Long idForwardNode) {
		return forwardNodeAPIService.getForwardNodeById(idForwardNode);
	}

	/**
	 * Add a forward node
	 * @param forwardNodeEntity Forward node to add
	 */
	public void addForwardNode(ForwardNodeEntity forwardNodeEntity) {
		forwardNodeAPIService.addForwardNode(forwardNodeEntity);
		refreshItem(forwardNodeEntity);
		refreshAll();
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

		final String filterTextTrim = filterTextInput.trim();

		if (Objects.equals(this.filterText, filterTextTrim)) {
			return;
		}
		this.filterText = filterTextTrim;

		setFilter(data -> matchesFilter(data, filterTextTrim));
	}

	private boolean matchesFilter(ForwardNodeEntity data, String filterText) {
		return data != null && data.matchesFilter(filterText);
	}

	public void deleteForwardNode(ForwardNodeEntity forwardNodeEntity) {
		forwardNodeAPIService.deleteForwardNode(forwardNodeEntity);
	}

	public void updateForwardNode(ForwardNodeEntity forwardNodeEntity) {
		forwardNodeAPIService.updateForwardNode(forwardNodeEntity);
	}

}
