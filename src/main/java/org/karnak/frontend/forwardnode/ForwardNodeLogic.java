/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
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
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.data.entity.ForwardNodeGroupEntity;
import org.karnak.backend.data.entity.NamedGroupEntity;
import org.karnak.backend.service.ForwardNodeAPIService;
import org.karnak.backend.service.ForwardNodeService;
import org.karnak.backend.service.ProjectService;
import org.karnak.backend.service.SOPClassUIDService;
import org.karnak.frontend.forwardnode.edit.destination.DestinationLogic;
import org.karnak.frontend.forwardnode.edit.source.SourceLogic;
import org.karnak.frontend.util.GroupTreeController;
import org.springframework.beans.factory.annotation.Autowired;
import org.weasis.core.util.annotations.Generated;

/**
 * Logic service use to make calls to backend and implement logic linked to the view
 */
@SpringComponent
@UIScope
@Slf4j
@Generated()
@NullUnmarked
public class ForwardNodeLogic extends ListDataProvider<ForwardNodeEntity>
		implements GroupTreeController<ForwardNodeEntity> {

	@Setter
	@Getter
	private ForwardNodeView forwardNodeView;

	// Services
	private final transient ForwardNodeAPIService forwardNodeAPIService;

	private final transient ForwardNodeService forwardNodeService;

	@Getter
	private final transient ProjectService projectService;

	@Getter
	private final transient SOPClassUIDService sopClassUIDService;

	@Getter
	private final transient SourceLogic sourceLogic;

	@Getter
	private final transient DestinationLogic destinationLogic;

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
		if (forwardNodeView != null) {
			forwardNodeView.getGridForwardNode().reload();
		}
	}

	// --- GroupTreeController ---------------------------------------------------------

	@Override
	public List<ForwardNodeEntity> listItems() {
		return forwardNodeService.getAllForwardNodes();
	}

	@Override
	public List<? extends NamedGroupEntity> listGroups() {
		return forwardNodeService.getAllGroups();
	}

	@Override
	public NamedGroupEntity groupOf(ForwardNodeEntity item) {
		return item.getGroup();
	}

	@Override
	public Long itemId(ForwardNodeEntity item) {
		return item.getId();
	}

	@Override
	public NamedGroupEntity createGroup(String name) {
		return forwardNodeService.saveGroup(name);
	}

	@Override
	public void renameGroup(NamedGroupEntity group, String name) {
		forwardNodeService.renameGroup((ForwardNodeGroupEntity) group, name);
	}

	@Override
	public void deleteGroup(NamedGroupEntity group) {
		forwardNodeService.deleteGroup((ForwardNodeGroupEntity) group);
	}

	@Override
	public void assign(ForwardNodeEntity item, NamedGroupEntity group) {
		forwardNodeService.assignToGroup(item, (ForwardNodeGroupEntity) group);
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
		try {
			return Long.valueOf(dataIdStr);
		}
		catch (NumberFormatException e) {
			log.error("Cannot get valueOf {}", dataIdStr, e);
		}
		return null;
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
		Objects.requireNonNull(filterTextInput, "Filter text cannot be null.");

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
