/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profilebody;

import java.util.List;
import java.util.Map;

public class ProfileElementBody {

	private String name;

	private String codename;

	private String condition;

	private String action;

	private String option;

	private String args;

	private List<String> tagEntities;

	private List<String> excludedTags;

	private Map<String, String> arguments;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCodename() {
		return codename;
	}

	public void setCodename(String codename) {
		this.codename = codename;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getOption() {
		return option;
	}

	public void setOption(String option) {
		this.option = option;
	}

	public String getArgs() {
		return args;
	}

	public void setArgs(String args) {
		this.args = args;
	}

	public Map<String, String> getArguments() {
		return arguments;
	}

	public void setArguments(Map<String, String> arguments) {
		this.arguments = arguments;
	}

	public List<String> getTags() {
		return tagEntities;
	}

	public void setTags(List<String> tagEntities) {
		this.tagEntities = tagEntities;
	}

	public List<String> getExcludedTags() {
		return excludedTags;
	}

	public void setExcludedTags(List<String> excludedTags) {
		this.excludedTags = excludedTags;
	}

}
