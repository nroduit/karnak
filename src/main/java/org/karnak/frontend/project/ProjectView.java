/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.project;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.data.entity.SecretEntity;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.frontend.MainLayout;
import org.karnak.frontend.project.component.EditProject;
import org.karnak.frontend.project.component.GridProject;
import org.karnak.frontend.project.component.NewProject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;

/**
 * Project View
 */
@Route(value = ProjectView.ROUTE, layout = MainLayout.class)
@PageTitle("KARNAK - Projects")
@Secured({ "ROLE_admin" })
@SuppressWarnings("serial")
public class ProjectView extends HorizontalLayout implements HasUrlParameter<String> {

	public static final String VIEW_NAME = "Projects";

	public static final String ROUTE = "projects";

	// Project Logic
	private final ProjectLogic projectLogic;

	// UI components
	private final NewProject newProject;

	private final GridProject gridProject;

	private final EditProject editProject;

	private Binder<ProjectEntity> newResearchBinder;

	/**
	 * Autowired constructor.
	 * @param projectLogic Project Logic used to call backend services and implement logic
	 * linked to the project view
	 */
	@Autowired
	public ProjectView(final ProjectLogic projectLogic) {
		// Bind the autowired service
		this.projectLogic = projectLogic;

		// Set the view in the service
		this.projectLogic.setProjectView(this);

		// Build components
		this.editProject = new EditProject();
		this.newProject = new NewProject();
		this.gridProject = new GridProject();

		// Init components
		initComponents();

		// Create layout
		buildLayout();

		// Events
		addEventButtonNewProject();
		addEventGridSelection();
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		ProjectEntity currentProjectEntity = null;
		projectLogic.refreshAll();
		if (parameter != null) {
			Long idProject = projectLogic.enter(parameter);
			if (idProject != null) {
				currentProjectEntity = projectLogic.retrieveProject(idProject);
			}
		}
		editProject.setProject(currentProjectEntity);
		gridProject.selectRow(currentProjectEntity);
	}

	/**
	 * Create and add the layout of the view
	 */
	private void buildLayout() {
		VerticalLayout layoutNewProject = new VerticalLayout(this.newProject, this.gridProject);
		setWidthFull();
		layoutNewProject.setWidth("40%");
		this.editProject.setWidth("60%");
		add(layoutNewProject, this.editProject);
	}

	/**
	 * Init components
	 */
	private void initComponents() {
		initEditProject(editProject, gridProject);
		initNewProjectForm(newProject);
		gridProject.setItems(projectLogic);
		newResearchBinder = newProject.getBinder();
	}

	/**
	 * Init the component NewProjectForm
	 * @param newProject Component to initialize
	 */
	private void initNewProjectForm(NewProject newProject) {
		projectLogic.initNewProjectProfileDropDown(newProject);
	}

	/**
	 * Init the component edit project
	 * @param editProject Component to initialize
	 * @param gridProject Link to the grid for element selection
	 */
	private void initEditProject(EditProject editProject, GridProject gridProject) {
		projectLogic.addEditEventButtonUpdate(editProject, gridProject);
		projectLogic.addEditEventButtonRemove(editProject);
		projectLogic.initEditProfileDropDown(editProject);
	}

	/**
	 * Add event on button New Project
	 */
	private void addEventButtonNewProject() {
		newProject.getButtonAdd().addClickListener(event -> {
			ProjectEntity newProjectEntity = new ProjectEntity();
			if (newResearchBinder.writeBeanIfValid(newProjectEntity)) {
				projectLogic.createProject(newProjectEntity, new SecretEntity(HMAC.generateRandomKey()));
				newProject.clear();
				gridProject.select(newProjectEntity);
				navigateProject(newProjectEntity);
			}
		});
	}

	/**
	 * Add event when selecting a project in the grid
	 */
	private void addEventGridSelection() {
		gridProject.asSingleSelect().addValueChangeListener(event -> navigateProject(event.getValue()));
	}

	/**
	 * Navigation to the project in parameter
	 * @param projectEntity Project to navigate to
	 */
	public void navigateProject(ProjectEntity projectEntity) {
		if (projectEntity == null) {
			UI.getCurrent().navigate(ProjectView.class, "");
		}
		else {
			String projectID = String.valueOf(projectEntity.getId());
			UI.getCurrent().navigate(ProjectView.class, projectID);
		}
	}

}
