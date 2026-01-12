/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.data.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;
import org.hibernate.annotations.ColumnTransformer;
import org.karnak.backend.enums.AuthConfigType;

@Entity(name = "AuthConfig")
@Table(name = "auth_config")
public class AuthConfigEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@JsonIgnore
	private Long id;

	@NotNull
	private String code;

	@ColumnTransformer(
			read = "pgp_sym_decrypt(" + "    client_secret, " + "    current_setting('encryption.key')" + ")",
			write = "pgp_sym_encrypt( " + "   ?, " + "    current_setting('encryption.key')" + ") ")
	@Column(columnDefinition = "bytea")
	private String clientSecret;

	@ColumnTransformer(read = "pgp_sym_decrypt(" + "    client_id, " + "    current_setting('encryption.key')" + ")",
			write = "pgp_sym_encrypt( " + "   ?, " + "    current_setting('encryption.key')" + ") ")
	@Column(columnDefinition = "bytea")
	private String clientId;

	@ColumnTransformer(
			read = "pgp_sym_decrypt(" + "    access_token_url, " + "    current_setting('encryption.key')" + ")",
			write = "pgp_sym_encrypt( " + "   ?, " + "    current_setting('encryption.key')" + ") ")
	@Column(columnDefinition = "bytea")
	private String accessTokenUrl;

	@ColumnTransformer(read = "pgp_sym_decrypt(" + "    scope, " + "    current_setting('encryption.key')" + ")",
			write = "pgp_sym_encrypt( " + "   ?, " + "    current_setting('encryption.key')" + ") ")
	@Column(columnDefinition = "bytea")
	private String scope;

	@Column(name = "type")
	@NotNull
	private AuthConfigType authConfigType;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getAccessTokenUrl() {
		return accessTokenUrl;
	}

	public void setAccessTokenUrl(String accessTokenUrl) {
		this.accessTokenUrl = accessTokenUrl;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public AuthConfigType getAuthConfigType() {
		return authConfigType;
	}

	public void setAuthConfigType(AuthConfigType authConfigType) {
		this.authConfigType = authConfigType;
	}

	@Override
	public String toString() {
		return this.code;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AuthConfigEntity that = (AuthConfigEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(code, that.code)
				&& Objects.equals(clientSecret, that.clientSecret) && Objects.equals(clientId, that.clientId)
				&& Objects.equals(accessTokenUrl, that.accessTokenUrl) && Objects.equals(scope, that.scope);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, code, clientSecret, clientId, accessTokenUrl, scope);
	}

}
