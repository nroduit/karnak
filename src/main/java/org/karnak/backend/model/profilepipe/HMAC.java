/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profilepipe;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.weasis.core.util.StringUtil;

@Slf4j
public class HMAC {

	public static final int KEY_BYTE_LENGTH = 16;

	private static final String HMAC_SHA256 = "HmacSHA256";

	private Mac mac;

	private HashContext hashContext;

	public HMAC(byte[] hmacKey) {
		initHMAC(hmacKey);
	}

	public HMAC(HashContext hashContext) {
		this.hashContext = hashContext;
		initHMAC(hashContext.getSecret());
	}

	/*
	 * Generate a random secret key of 32bytes
	 */
	public static byte[] generateRandomKey() {
		SecureRandom random = new SecureRandom();
		byte[] bytes = new byte[KEY_BYTE_LENGTH];
		random.nextBytes(bytes);
		return bytes;
	}

	public static String byteToHex(byte[] key) {
		return Hex.encodeHexString(key);
	}

	public static String showHexKey(String key) {
		return String.format("%s-%s-%s-%s-%s", key.substring(0, 8), key.substring(8, 12), key.substring(12, 16),
				key.substring(16, 20), key.substring(20));
	}

	public static byte[] hexToByte(String hexKey) {
		try {
			return Hex.decodeHex(hexKey.replaceAll("-", ""));
		}
		catch (DecoderException e) {
			return null;
		}
	}

	public static boolean validateKey(String hexKey) {
		String cleanHexKey = hexKey.replaceAll("-", "");
		if (cleanHexKey.length() == 32) {
			return hexToByte(cleanHexKey) != null;
		}
		return false;
	}

	private void initHMAC(byte[] keyValue) {
		try {
			SecretKeySpec key = new SecretKeySpec(keyValue, HMAC_SHA256);
			this.mac = Mac.getInstance(HMAC_SHA256);
			this.mac.init(key);
		}
		catch (NoSuchAlgorithmException e) {
			log.error("Invalid algorithm for the HMAC", e);
		}
		catch (InvalidKeyException e) {
			log.error("Invalid key for the HMAC init", e);
		}
	}

	// returns value in [scaleMin..scaleMax)
	public double scaleHash(String value, int scaledMin, int scaledMax) {
		final byte[] hash = new byte[6];
		final double max = 0x1000000000000L;
		final double scale = scaledMax - (double) scaledMin;

		System.arraycopy(byteHash(value), 0, hash, 0, 6);
		double fraction = new BigInteger(1, hash).doubleValue() / max;
		return (int) (fraction * scale) + (double) scaledMin;
	}

	public String uidHash(String inputUID) {
		if (!StringUtil.hasText(inputUID)) {
			throw new IllegalArgumentException();
		}
		byte[] uuid = new byte[16];
		System.arraycopy(byteHash(inputUID), 0, uuid, 0, 16);
		// https://en.wikipedia.org/wiki/Universally_unique_identifier
		// GUID type 4
		// Version -> 4
		uuid[6] &= 0x0F;
		uuid[6] |= 0x40;
		// Variant 1 -> 10b
		uuid[8] &= 0x3F;
		uuid[8] |= 0x80;
		return "2.25." + new BigInteger(1, uuid);
	}

	public HashContext getHashContext() {
		return hashContext;
	}

	public byte[] byteHash(String value) {
		byte[] bytes = null;
		bytes = mac.doFinal(value.getBytes(StandardCharsets.US_ASCII));
		return bytes;
	}

}
