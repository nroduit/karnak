/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.dicom;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.dcm4che3.net.Status;
import org.karnak.backend.model.dicom.ConfigNode;
import org.karnak.backend.model.dicom.DicomNodeList;
import org.karnak.backend.model.dicom.WadoNode;
import org.karnak.backend.model.dicom.WadoNodeList;
import org.weasis.dicom.op.Echo;
import org.weasis.dicom.param.AdvancedParams;
import org.weasis.dicom.param.ConnectOptions;
import org.weasis.dicom.param.DicomNode;
import org.weasis.dicom.param.DicomState;

public class Util {

	private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();

	private Util() {
	}

	private static <T> T timedCall(Callable<T> c, long timeout, TimeUnit timeUnit)
			throws InterruptedException, ExecutionException, TimeoutException {
		FutureTask<T> task = new FutureTask<>(c);
		THREAD_POOL.execute(task);
		return task.get(timeout, timeUnit);
	}

	public static Future<Boolean> portIsOpen(final ExecutorService es, final String host, final int port,
			final int timeout) {
		return es.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() {
				try (Socket socket = new Socket()) {
					socket.connect(new InetSocketAddress(host, port), timeout);
					socket.close();
					return true;
				}
				catch (Exception ex) {
					return false;
				}
			}
		});
	}

	public static void getNetworkInfo(StringBuilder result) {
		try {
			Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();

			while (e.hasMoreElements()) {
				NetworkInterface netint = e.nextElement();
				if (netint != null) {
					result.append("Display name: ");
					result.append(netint.getDisplayName());
					result.append("<br>Name: ");
					result.append(netint.getName());

					byte[] mac_byte = netint.getHardwareAddress();
					if (mac_byte != null) {
						result.append("<br>MAC address: ");
						for (int i = 0; i < mac_byte.length; i++) {
							result.append(String.format("%02X%s", mac_byte[i], (i < mac_byte.length - 1) ? "-" : ""));
						}
					}

					Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
					for (InetAddress inetAddress : Collections.list(inetAddresses)) {
						// Force getting hostname
						inetAddress.getHostName();
						result.append("<br>Inet address: ");
						result.append(inetAddress);
					}
					result.append("<hr>");
				}
			}
		}
		catch (Throwable e1) {
			result.append("<br>error: ");
			result.append(e1.getMessage());
		}
	}

	public static boolean getNetworkResponse(StringBuilder result, String aet, String host, int port,
			boolean fontIcon) {

		return getNetworkResponse(result, aet, host, port, fontIcon, "HTML");
	}

	public static boolean getNetworkResponse(StringBuilder result, String aet, String host, int port, boolean fontIcon,
			String format) {

		boolean reachable = false;
		boolean xml = "XML".equalsIgnoreCase(format);
		try {
			if ("XML".equalsIgnoreCase(format)) {
				result.append("<DcmNetworkStatus>");
			}

			InetAddress address = InetAddress.getByName(host);
			reachable = isReachableByPing(host);

			if (reachable) {

				if (xml) {
					result.append(address);
					result.append(" machine is turned on and can be pinged");
				}
				else {
					result.append("<span style=\"color:green\">");
					result.append(getOKItem(fontIcon));
					result.append("</span> ");
					result.append(address);
					result.append(" machine is turned on and can be pinged.<br>");
				}
			}
			else if (address.getHostAddress().equals(address.getHostName())) {
				if (xml) {
					result.append(address);
					result.append(" host address and host name are equal, meaning the host name could not be resolved");
				}
				else {
					result.append("<span style=\"color:orange\">");
					result.append(getWarningItem(fontIcon));
					result.append("</span> ");
					result.append(address);
					result.append(
							" host address and host name are equal, meaning the host name could not be resolved.<br>");
				}
			}
			else {
				if (xml) {
					result.append(address);
					result.append(" machine is known in a DNS lookup but cannot be pinged");
				}
				else {
					result.append("<span style=\"color:orange\">");
					result.append(getWarningItem(fontIcon));
					result.append("</span> ");
					result.append(address);
					result.append(" machine is known in a DNS lookup but cannot be pinged.<br>");
				}
			}

			if (xml) {
				result.append("</DcmNetworkStatus>");
			}

		}
		catch (Throwable e) {
			if (xml) {
				result.append("Network unexpected error: ");
				result.append(e.getMessage());
				result.append("</DcmNetworkStatus>");
			}
			else {
				result.append("<span style=\"color:red\">");
				result.append(getWarningItem(fontIcon));
				result.append(" Network unexpected error: ");
				result.append(e.getMessage());
				result.append("</span><br>");
			}
		}

		if (!reachable) {

			if (xml) {
				// Do nothing
			}
			else {
				Future<Boolean> future = portIsOpen(THREAD_POOL, host, port, 2500);
				try {
					reachable = future.get();
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				catch (Exception e) {
					// Do nothing
				}

				result.append(reachable ? "<span style=\"color:green\">" + getOKItem(fontIcon)
						: "<span style=\"color:red\">" + getWarningItem(fontIcon));
				result.append("</span> ");
				result.append(host);
				result.append(reachable ? " is listening on port " : " is not listening on port ");
				result.append(port);
				result.append(".<br>");
			}
		}
		return reachable;
	}

	public static boolean isReachableByPing(String host) throws Exception {
		final String cmd;
		if (System.getProperty("os.name").startsWith("Win")) {
			// For Windows
			cmd = "ping -n 1 " + host;
		}
		else {
			// For Linux and OSX
			cmd = "ping -c 1 " + host;
		}

		try {
			int returnCode = timedCall(() -> {
				Process process = Runtime.getRuntime().exec(cmd);
				process.waitFor();
				return process.exitValue();
			}, 2500, TimeUnit.MILLISECONDS);
			return returnCode == 0;
		}
		catch (TimeoutException e) {
			System.out.println("Ping timeout after 2.5 sec of " + host);
		}

		return false;
	}

	public static boolean getEchoResponse(StringBuilder result, String callingAET, DicomNode calledNode,
			boolean fontIcon, String format) {

		return getEchoResponse(result, callingAET, calledNode, fontIcon, format, null);
	}

	public static boolean getEchoResponse(StringBuilder result, String callingAET, DicomNode calledNode,
			boolean fontIcon, String format, Integer connectTimeout) {

		boolean success = false;
		boolean xml = "XML".equalsIgnoreCase(format);
		try {
			AdvancedParams params = new AdvancedParams();

			DicomState state;
			if (connectTimeout != null) {
				ConnectOptions connectOptions = new ConnectOptions();
				connectOptions.setConnectTimeout(connectTimeout);
				params.setConnectOptions(connectOptions);
				state = Echo.process(params, new DicomNode(callingAET), calledNode);
			}
			else {
				state = Echo.process(callingAET, calledNode);
			}
			success = state.getStatus() == Status.Success;

			if (xml) {
				result.append("<DcmStatus>");

				if (success) {
					result.append("Success").append("</DcmStatus>");
					result.append("<DcmStatusMessage>").append(state.getMessage()).append("</DcmStatusMessage>");
				}
				else {
					result.append("Error ").append(Integer.toHexString(state.getStatus())).append("</DcmStatus>");
					result.append("<DcmStatusMessage>").append(state.getMessage()).append("</DcmStatusMessage>");
				}
			}
			else {
				// "HTML" and anything else
				result.append(success ? "<span style=\"color:green\">" + getOKItem(fontIcon)
						: "<span style=\"color:red\">" + getWarningItem(fontIcon));

				result.append("</span> DICOM Status: ");
				if (success) {
					result.append("Success");
				}
				else {
					result.append("error code ");
					result.append(Integer.toHexString(state.getStatus()));
				}
				result.append("<br>DICOM Message: ");
				result.append(state.getMessage());
				result.append("<br>");
			}

		}
		catch (Throwable e) {
			if (xml) {
				result.append("<DcmStatus>DICOM unexpected error</DcmStatus>");
				result.append("<DcmStatusMessage>").append(e.getMessage()).append("</DcmStatusMessage>");
			}
			else {
				// "HTML" and anything else
				result.append("<span style=\"color:red\">");
				result.append(getWarningItem(fontIcon));
				result.append(" DICOM unexpected error: ");
				result.append(e.getMessage());
				result.append("</span><br>");
			}
		}
		return success;
	}

	public static void getWadoResponse(StringBuilder result, WadoNode node, boolean fontIcon, String format) {
		getWadoResponse(result, node, fontIcon, format, 10000);
	}

	public static void getWadoResponse(StringBuilder result, WadoNode node, boolean fontIcon, String format,
			long connectTimeout) {
		long startimeExt = System.currentTimeMillis();

		boolean xml = "XML".equalsIgnoreCase(format);
		try {
			HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofMillis(connectTimeout))
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
			HttpRequest.Builder builder = HttpRequest.newBuilder();
			for (String tag : node.getTags()) {
				String[] val = tag.split(":");
				if (val.length == 2) {
					builder.header(val[0].trim(), val[1].trim());
				}
			}
			HttpRequest request = builder.GET()
				.uri(new URI("https://httpbin.org/get"))
				.header("User-Agent", "Mozilla/5.0 Firefox/43.0") // add request
				// header
				.build();

			long starTime = System.currentTimeMillis();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			boolean success = response.statusCode() == HttpURLConnection.HTTP_OK;
			String message = response.body();
			if (xml) {
				result.append("<WadoStatus elapsedTime=\"")
					.append(System.currentTimeMillis() - starTime)
					.append("ms\">");
				result.append(message);
				result.append("</WadoStatus>");
			}
			else {
				result.append(success ? "<span style=\"color:green\">" + getOKItem(fontIcon)
						: "<span style=\"color:red\">" + getWarningItem(fontIcon));

				result.append(" Response Message in ");
				result.append(System.currentTimeMillis() - starTime);
				result.append(" ms: ");
				result.append(message);
				result.append("</span><br>");
			}
		}
		catch (Throwable e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			if (xml) {
				result.append("<WadoStatus elapsedTime=\"")
					.append(System.currentTimeMillis() - startimeExt)
					.append("ms\">");
				result.append("WADO unexpected error: ");
				result.append(e.getMessage());
				result.append("</WadoStatus>");
			}
			else {
				result.append("<span style=\"color:red\">");
				result.append(getWarningItem(fontIcon));
				result.append(" WADO unexpected error in ");
				result.append(System.currentTimeMillis() - startimeExt);
				result.append(" ms: ");
				result.append(e.getMessage());
				result.append("</span><br>");
			}
		}
	}

	public static DicomNodeList readnodes(URL url, String name) {
		DicomNodeList list = new DicomNodeList(name);
		if (url != null) {
			Scanner scan = null;
			try {
				scan = new Scanner(url.openStream(), StandardCharsets.UTF_8); // $NON-NLS-1$

				while (scan.hasNext()) {
					String val = scan.nextLine();
					if (val.startsWith("#")) {
						continue;
					}

					String[] line = val.split(",(?=([^\"]*\"[^\"]*\")*+[^\"]*$)", -1); // $NON-NLS-1$
					if (line.length >= 4) {
						try {
							ConfigNode node = new ConfigNode(trimSplit(line[0]), new DicomNode(trimSplit(line[1]),
									trimSplit(line[2]), Integer.parseInt(trimSplit(line[3]))));
							list.add(node);
						}
						catch (Exception e) {
							System.out.println("Cannot read dicom node:  " + line[2]);
						}
					}
				}
			}
			catch (Exception e) {
				System.out.println("Cannot read dicom nodes files:  " + url);
			}
			finally {
				if (scan != null) {
					scan.close();
				}
			}
		}

		return list;
	}

	public static WadoNodeList readWadoNodes(URL url, String name) {
		WadoNodeList list = new WadoNodeList(name);
		if (url != null) {
			Scanner scan = null;
			try {
				scan = new Scanner(url.openStream(), StandardCharsets.UTF_8); // $NON-NLS-1$

				while (scan.hasNext()) {
					String val = scan.nextLine();
					if (val.startsWith("#")) {
						continue;
					}

					String[] line = val.split(",(?=([^\"]*\"[^\"]*\")*+[^\"]*$)", -1); // $NON-NLS-1$
					if (line.length >= 2) {
						try {
							WadoNode node = new WadoNode(trimSplit(line[0]), new URL(trimSplit(line[1])));
							list.add(node);

							for (int i = 2; i < line.length; i++) {
								node.getTags().add(trimSplit(line[i]));
							}
						}
						catch (Exception e) {
							System.out.println("Cannot read wado node:  " + line[2]);
						}
					}
				}
			}
			catch (Exception e) {
				System.out.println("Cannot read wado nodes files:  " + url);
			}
			finally {
				if (scan != null) {
					scan.close();
				}
			}
		}

		return list;
	}

	private static String trimSplit(String val) {
		if (val != null) {
			String res = val.trim();
			if (res.startsWith("\"") && res.length() > 2 && res.endsWith("\"")) {
				return res.substring(1, res.length() - 1);
			}
			return res;
		}
		return "";
	}

	private static String getWarningItem(boolean fontIcon) {
		if (fontIcon) {
			return "<vaadin-icon icon=\"vaadin:exclamation-circle\" style=\"width:1em; height:1em;\"></vaadin-icon>";
		}
		return "WARN";
	}

	private static String getOKItem(boolean fontIcon) {
		if (fontIcon) {
			return "<vaadin-icon icon=\"vaadin:check-circle\" style=\"width:1em; height:1em;\"></vaadin-icon>";
		}
		return "OK";
	}

}
