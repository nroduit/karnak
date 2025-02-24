/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.util;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServletUtil {

	private ServletUtil() {
	}

	public static String getFirstParameter(Object val) {
		if (val instanceof String[]) {
			String[] params = (String[]) val;
			if (params.length > 0) {
				return params[0];
			}
		}
		else if (val != null) {
			return val.toString();
		}
		return null;
	}

	public static String[] getParameters(Object val) {
		if (val instanceof String[]) {
			return (String[]) val;
		}
		else if (val != null) {
			return new String[] { val.toString() };
		}
		return null;
	}

	public static Object addParameter(Object val, String arg) {
		if (val instanceof String[]) {
			String[] array = (String[]) val;
			String[] arr = Arrays.copyOf(array, array.length + 1);
			arr[array.length] = arg;
			return arr;
		}
		else if (val != null) {
			return new String[] { val.toString(), arg };
		}
		return arg;
	}

	public static int getIntProperty(Properties prop, String key, int def) {
		int result = def;
		final String value = prop.getProperty(key);
		if (value != null) {
			try {
				result = Integer.parseInt(value);
			}
			catch (NumberFormatException ignore) {
				// return the default value
			}
		}
		return result;
	}

	public static long getLongProperty(Properties prop, String key, long def) {
		long result = def;
		final String value = prop.getProperty(key);
		if (value != null) {
			try {
				result = Long.parseLong(value);
			}
			catch (NumberFormatException ignore) {
				// return the default value
			}
		}
		return result;
	}

	public static void write(InputStream in, OutputStream out) throws IOException {
		try {
			copy(in, out, 2048);
		}
		catch (Exception e) {
			handleException(e);
		}
		finally {
			try {
				in.close();
				out.flush();
			}
			catch (IOException e) {
				// jetty 6 throws broken pipe exception here too
				handleException(e);
			}
		}
	}

	private static int copy(final InputStream in, final OutputStream out, final int bufSize) throws IOException {
		final byte[] buffer = new byte[bufSize];
		int bytesCopied = 0;
		while (true) {
			int byteCount = in.read(buffer, 0, buffer.length);
			if (byteCount <= 0) {
				break;
			}
			out.write(buffer, 0, byteCount);
			bytesCopied += byteCount;
		}
		return bytesCopied;
	}

	private static void handleException(Exception e) {
		Throwable throwable = e;
		boolean ignoreException = false;
		while (throwable != null) {
			if (throwable instanceof SQLException) {
				break; // leave false and quit loop
			}
			else if (throwable instanceof SocketException) {
				String message = throwable.getMessage();
				ignoreException = message != null
						&& (message.indexOf("Connection reset") != -1 || message.indexOf("Broken pipe") != -1
								|| message.indexOf("Socket closed") != -1 || message.indexOf("connection abort") != -1);
			}
			else {
				ignoreException = throwable.getClass().getName().indexOf("ClientAbortException") >= 0
						|| throwable.getClass().getName().indexOf("EofException") >= 0;
			}
			if (ignoreException) {
				break;
			}
			throwable = throwable.getCause();
		}

		if (!ignoreException) {
			throw new IllegalStateException("Unable to write the response", e);
		}
	}

	public static void write(String str, ServletOutputStream out) {
		try {
			byte[] bytes = str.getBytes();
			out.write(bytes, 0, bytes.length);
		}
		catch (Exception e) {
			handleException(e);
		}
		finally {
			try {
				out.flush();
			}
			catch (IOException e) {
				// jetty 6 throws broken pipe exception here too
				handleException(e);
			}
		}
	}

	public static void sendResponseError(HttpServletResponse response, int code, String message) {
		try {
			response.sendError(code, message);
		}
		catch (IOException e) {
			log.error("Cannot send http response message!", e);
		}
	}

}
