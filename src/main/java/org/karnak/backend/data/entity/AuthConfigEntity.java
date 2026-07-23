/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.data.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.enums.AuthConfigType;

@Entity(name = "AuthConfig")
@Table(name = "auth_config")
@NullUnmarked
@Getter
@Setter
public class AuthConfigEntity implements Serializable {

	@Serial
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
	@JsonProperty(access = Access.WRITE_ONLY)
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
