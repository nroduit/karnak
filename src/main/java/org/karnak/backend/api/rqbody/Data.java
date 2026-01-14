/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.api.rqbody;

public class Data {

	private String[] idtypes;

	private Fields fields;

	private Ids ids;

	private SearchIds[] searchIds;

	private String[] resultFields;

	private String[] resultIds;

	public Data(String[] idtypes, Fields fields, Ids ids) {
		this.idtypes = idtypes;
		this.fields = fields;
		this.ids = ids;
	}

	public Data(SearchIds[] searchIds, String[] resultFields, String[] resultIds) {
		this.searchIds = searchIds;
		this.resultFields = resultFields;
		this.resultIds = resultIds;
	}

	public Data(SearchIds[] searchIds, String[] resultFields) {
		this.searchIds = searchIds;
		this.resultFields = resultFields;
	}

	public String[] get_idtypes() {
		return this.idtypes;
	}

	public void set_idtypes(String[] idtypes) {
		this.idtypes = idtypes;
	}

	public Fields get_fields() {
		return this.fields;
	}

	public void set_fields(Fields fields) {
		this.fields = fields;
	}

	public Ids get_ids() {
		return this.ids;
	}

	public void set_ids(Ids ids) {
		this.ids = ids;
	}

	public SearchIds[] get_searchIds() {
		return this.searchIds;
	}

	public void set_searchIds(SearchIds[] searchIds) {
		this.searchIds = searchIds;
	}

	public String[] get_resultFields() {
		return this.resultFields;
	}

	public void set_resultFields(String[] resultFields) {
		this.resultFields = resultFields;
	}

	public String[] get_resultIds() {
		return this.resultIds;
	}

	public void set_resultIds(String[] resultIds) {
		this.resultIds = resultIds;
	}

}
