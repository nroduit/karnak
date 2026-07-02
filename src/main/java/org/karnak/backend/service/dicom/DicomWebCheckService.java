/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.dicom;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.karnak.backend.enums.DicomWebServiceType;
import org.karnak.backend.model.dicom.WebDestinationNode;
import org.karnak.backend.model.dicom.result.TlsCertificateInfo;
import org.karnak.backend.model.dicom.result.WebDestinationCheckResult;
import org.karnak.backend.model.dicom.result.WebNodeCheckResult;
import org.karnak.backend.model.dicom.result.WebServiceProbe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Non-invasive reachability check for a DICOMweb (STOW-RS) destination URL: TCP reach to
 * the host, TLS handshake and certificate inspection for {@code https}, and the HTTP
 * status of an {@code OPTIONS} probe. No study is uploaded.
 *
 * <p>
 * The TLS handshake and the HTTP probe deliberately accept any certificate so the check
 * can <em>report</em> an expired or self-signed certificate (and still reach the
 * endpoint) rather than failing the way a strict client would; certificate validity and
 * trust are evaluated separately and surfaced in the result. Never throws.
 */
@Service
@Slf4j
@NullUnmarked
public class DicomWebCheckService {

	private static final Duration CHECK_TIMEOUT = Duration.ofSeconds(30);

	private final Duration timeout;

	private final @Nullable WebTokenService webTokenService;

	@Autowired
	public DicomWebCheckService(@Value("${dicom-tools.web.timeout-ms:5000}") long timeoutMs,
			WebTokenService webTokenService) {
		this.timeout = Duration.ofMillis(timeoutMs);
		this.webTokenService = webTokenService;
	}

	/** Reachability-only variant (no auth) for direct URL checks and tests. */
	public DicomWebCheckService(long timeoutMs) {
		this.timeout = Duration.ofMillis(timeoutMs);
		this.webTokenService = null;
	}

	/**
	 * Checks several DICOMweb destinations concurrently (one virtual thread per
	 * destination), preserving input order. A global timeout guards against an
	 * unresponsive endpoint.
	 */
	public List<WebNodeCheckResult> check(List<WebDestinationNode> destinations) {
		if (destinations == null || destinations.isEmpty()) {
			return List.of();
		}

		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			List<Callable<WebNodeCheckResult>> tasks = destinations.stream()
				.map(destination -> (Callable<WebNodeCheckResult>) () -> new WebNodeCheckResult(destination,
						check(destination)))
				.toList();

			List<Future<WebNodeCheckResult>> futures = executor.invokeAll(tasks, CHECK_TIMEOUT.toSeconds(),
					TimeUnit.SECONDS);

			List<WebNodeCheckResult> results = new ArrayList<>(futures.size());
			for (int i = 0; i < futures.size(); i++) {
				results.add(resolve(destinations.get(i), futures.get(i)));
			}
			return results;
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while checking DICOMweb destinations", ex);
			return List.of();
		}
	}

	private WebNodeCheckResult resolve(WebDestinationNode destination, Future<WebNodeCheckResult> future) {
		try {
			return future.get();
		}
		catch (InterruptedException _) {
			Thread.currentThread().interrupt();
			return timedOut(destination);
		}
		catch (Exception ex) {
			log.error("DICOMweb check did not complete for {}: {}", destination.url(), ex.getMessage());
			return timedOut(destination);
		}
	}

	private static WebNodeCheckResult timedOut(WebDestinationNode destination) {
		WebDestinationCheckResult result = WebDestinationCheckResult.builder()
			.url(destination.url())
			.unexpectedErrorMessage("Check did not complete within " + CHECK_TIMEOUT.toSeconds() + "s")
			.build();
		return new WebNodeCheckResult(destination, result);
	}

	/** DICOMweb services probed by default (those sharing the DICOMweb base URL). */
	private static final Set<DicomWebServiceType> DEFAULT_SERVICES = EnumSet.of(DicomWebServiceType.STOW_RS,
			DicomWebServiceType.QIDO_RS, DicomWebServiceType.WADO_RS, DicomWebServiceType.UPS_RS,
			DicomWebServiceType.CAPABILITIES);

	private static final String BOGUS_UID = "1.2.826.0.1.3680043.2.1143.999999";

	private static final String HTTPS = "https";

	private static final String STUDIES_PATH = "/studies";

	private static final String APPLICATION_DICOM_JSON = "application/dicom+json";

	private static final String PROBE_NOT_SENT = "probe could not be sent";

	/** Checks a configured destination, probing the default DICOMweb services. */
	public WebDestinationCheckResult check(WebDestinationNode destination) {
		return check(destination, DEFAULT_SERVICES);
	}

	/**
	 * Checks a configured destination: URL reachability, plus (when it references an auth
	 * configuration and a token service is available) an OAuth token acquisition, plus a
	 * non-invasive probe of each requested DICOMweb service when the endpoint is
	 * reachable.
	 */
	public WebDestinationCheckResult check(WebDestinationNode destination, Set<DicomWebServiceType> services) {
		WebDestinationCheckResult reachability = check(destination.url());
		WebDestinationCheckResult.WebDestinationCheckResultBuilder builder = reachability.toBuilder();

		String token = null;
		String authConfig = destination.authConfig();
		if (webTokenService != null && authConfig != null && !authConfig.isBlank()) {
			WebTokenService.TokenResult tokenResult = webTokenService.authorize(authConfig);
			builder.auth(tokenResult.check());
			token = tokenResult.token();
		}

		if (reachability.isTcpReachable() && !reachability.isUnexpectedError() && services != null) {
			for (DicomWebServiceType service : services) {
				builder.serviceProbe(probeService(destination.url(), service, token));
			}
		}

		return builder.build();
	}

	public WebDestinationCheckResult check(String url) {
		URI uri;
		String host;
		try {
			uri = URI.create(url.trim());
			host = uri.getHost();
		}
		catch (IllegalArgumentException _) {
			return WebDestinationCheckResult.builder().url(url).unexpectedErrorMessage("Invalid URL").build();
		}

		if (host == null) {
			return WebDestinationCheckResult.builder().url(url).unexpectedErrorMessage("Invalid URL (no host)").build();
		}

		boolean secure = HTTPS.equalsIgnoreCase(uri.getScheme());
		int defaultPort = secure ? 443 : 80;
		int port = (uri.getPort() != -1) ? uri.getPort() : defaultPort;

		boolean tcpReachable = isTcpReachable(host, port);
		TlsCertificateInfo tls = (secure && tcpReachable) ? inspectTls(host, port) : null;
		HttpProbe http = tcpReachable ? httpProbe(uri, secure) : new HttpProbe(false, 0);

		return WebDestinationCheckResult.builder()
			.url(url)
			.host(host)
			.port(port)
			.secure(secure)
			.tcpReachable(tcpReachable)
			.tls(tls)
			.httpResponded(http.responded())
			.httpStatus(http.status())
			.build();
	}

	private record HttpProbe(boolean responded, int status) {
	}

	private boolean isTcpReachable(String host, int port) {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(host, port), (int) timeout.toMillis());
			return true;
		}
		catch (IOException _) {
			log.debug("Cannot reach {}:{}", host, port);
			return false;
		}
	}

	private @Nullable TlsCertificateInfo inspectTls(String host, int port) {
		try {
			SSLContext context = permissiveContext();
			try (SSLSocket socket = (SSLSocket) context.getSocketFactory().createSocket()) {
				socket.connect(new InetSocketAddress(host, port), (int) timeout.toMillis());
				socket.setSoTimeout((int) timeout.toMillis());

				SSLParameters parameters = socket.getSSLParameters();
				parameters.setServerNames(List.of(new SNIHostName(host)));
				socket.setSSLParameters(parameters);

				socket.startHandshake();
				SSLSession session = socket.getSession();
				X509Certificate[] chain = toX509Chain(session.getPeerCertificates());
				X509Certificate leaf = chain[0];

				Instant notAfter = leaf.getNotAfter().toInstant();
				Instant notBefore = leaf.getNotBefore().toInstant();
				Instant now = Instant.now();
				boolean expired = now.isAfter(notAfter) || now.isBefore(notBefore);
				long daysUntilExpiry = Duration.between(now, notAfter).toDays();

				return new TlsCertificateInfo(session.getProtocol(), leaf.getSubjectX500Principal().getName(),
						leaf.getIssuerX500Principal().getName(),
						LocalDate.ofInstant(notAfter, ZoneId.systemDefault()).toString(), daysUntilExpiry, expired,
						isTrusted(chain));
			}
		}
		catch (IOException | GeneralSecurityException ex) {
			log.debug("TLS inspection failed for {}:{} — {}", host, port, ex.getMessage());
			return null;
		}
	}

	private HttpProbe httpProbe(URI uri, boolean secure) {
		try {
			HttpClient.Builder builder = HttpClient.newBuilder()
				.connectTimeout(timeout)
				.followRedirects(HttpClient.Redirect.NEVER);
			if (secure) {
				builder.sslContext(permissiveContext());
			}

			HttpRequest request = HttpRequest.newBuilder(uri)
				.timeout(timeout)
				.method("OPTIONS", BodyPublishers.noBody())
				.build();
			try (HttpClient client = builder.build()) {
				HttpResponse<Void> response = client.send(request, BodyHandlers.discarding());
				return new HttpProbe(true, response.statusCode());
			}
		}
		catch (IOException | GeneralSecurityException ex) {
			log.debug("HTTP probe failed for {} — {}", uri, ex.getMessage());
			return new HttpProbe(false, 0);
		}
		catch (InterruptedException _) {
			Thread.currentThread().interrupt();
			return new HttpProbe(false, 0);
		}
	}

	/** Non-invasive probe of one DICOMweb service relative to the destination URL. */
	private WebServiceProbe probeService(String url, DicomWebServiceType service, @Nullable String token) {
		String base = baseOf(url);
		return switch (service) {
			case STOW_RS -> probeStow(base, token);
			case QIDO_RS -> probeQido(base, token);
			case WADO_RS -> probeWado(base, token);
			case WADO_URI -> probeWadoUri(url, token);
			case UPS_RS -> probeUps(base, token);
			case CAPABILITIES -> probeCapabilities(base, token);
		};
	}

	private WebServiceProbe probeUps(String base, @Nullable String token) {
		HttpResponse<Void> response = send("GET", URI.create(base + "/workitems?limit=1"), APPLICATION_DICOM_JSON,
				token);
		return classifyRetrieve(DicomWebServiceType.UPS_RS, response, "worklist query");
	}

	/**
	 * Fetches the server's declared Capabilities-RS document ({@code GET
	 * {base}/capabilities}) and reports whether one is published, its content type and a
	 * best-effort scan of the DICOMweb resources it mentions. The document is read but
	 * not fully parsed, since its format (WADL / XML / JSON) varies and support is
	 * inconsistent across servers.
	 */
	private WebServiceProbe probeCapabilities(String base, @Nullable String token) {
		HttpResponse<String> response = sendForBody(URI.create(base + "/capabilities"),
				"application/json, application/vnd.sun.wadl+xml;q=0.9, application/dicom+json;q=0.8, */*;q=0.5", token);
		if (response == null) {
			return new WebServiceProbe(DicomWebServiceType.CAPABILITIES, WebServiceProbe.Support.INCONCLUSIVE, 0,
					PROBE_NOT_SENT);
		}

		int status = response.statusCode();
		if (status == 401 || status == 403) {
			return authRequired(DicomWebServiceType.CAPABILITIES, status);
		}
		if (status == 404 || status == 405 || status == 501) {
			return new WebServiceProbe(DicomWebServiceType.CAPABILITIES, WebServiceProbe.Support.UNSUPPORTED, status,
					"no capabilities resource (status " + status + ")");
		}

		String body = response.body();
		if (status >= 200 && status < 300 && body != null && !body.isBlank()) {
			String contentType = response.headers().firstValue("Content-Type").orElse(null);
			return new WebServiceProbe(DicomWebServiceType.CAPABILITIES, WebServiceProbe.Support.SUPPORTED, status,
					declaredSummary(contentType, body));
		}

		return new WebServiceProbe(DicomWebServiceType.CAPABILITIES, WebServiceProbe.Support.INCONCLUSIVE, status,
				"no capabilities document (status " + status + ")");
	}

	private static String declaredSummary(@Nullable String contentType, String body) {
		String lower = body.toLowerCase(Locale.ROOT);
		List<String> mentions = new ArrayList<>();
		for (String keyword : List.of("studies", "series", "instances", "metadata", "frames", "rendered", "bulkdata",
				"wado", "workitems")) {
			if (lower.contains(keyword)) {
				mentions.add(keyword);
			}
		}
		String type = (contentType != null) ? contentType : "unknown content type";
		return mentions.isEmpty() ? type : type + "; mentions " + String.join(", ", mentions);
	}

	private WebServiceProbe probeStow(String base, @Nullable String token) {
		HttpResponse<Void> response = send("OPTIONS", URI.create(base + STUDIES_PATH), null, token);
		if (response == null) {
			return new WebServiceProbe(DicomWebServiceType.STOW_RS, WebServiceProbe.Support.INCONCLUSIVE, 0,
					PROBE_NOT_SENT);
		}
		int status = response.statusCode();
		if (status == 401 || status == 403) {
			return authRequired(DicomWebServiceType.STOW_RS, status);
		}

		String allow = headerOf(response, "Allow", "Access-Control-Allow-Methods");
		String acceptPost = response.headers().firstValue("Accept-Post").orElse(null);
		if (allow == null) {
			return new WebServiceProbe(DicomWebServiceType.STOW_RS, WebServiceProbe.Support.INCONCLUSIVE, status,
					"methods not advertised (status " + status + ")");
		}

		boolean postAllowed = allow.toUpperCase(Locale.ROOT).contains("POST");
		String detail = "Allow: " + allow + ((acceptPost != null) ? "; accepts " + acceptPost : "");
		return new WebServiceProbe(DicomWebServiceType.STOW_RS,
				postAllowed ? WebServiceProbe.Support.SUPPORTED : WebServiceProbe.Support.UNSUPPORTED, status, detail);
	}

	private WebServiceProbe probeQido(String base, @Nullable String token) {
		HttpResponse<Void> response = send("GET", URI.create(base + "/studies?limit=1"), APPLICATION_DICOM_JSON, token);
		return classifyRetrieve(DicomWebServiceType.QIDO_RS, response, "query");
	}

	private WebServiceProbe probeWado(String base, @Nullable String token) {
		HttpResponse<Void> response = send("GET", URI.create(base + "/studies/" + BOGUS_UID + "/metadata"),
				APPLICATION_DICOM_JSON, token);
		return classifyRetrieve(DicomWebServiceType.WADO_RS, response, "retrieve");
	}

	private WebServiceProbe probeWadoUri(String url, @Nullable String token) {
		String separator = url.contains("?") ? "&" : "?";
		URI uri = URI.create(url + separator + "requestType=WADO&studyUID=" + BOGUS_UID + "&seriesUID=" + BOGUS_UID
				+ "&objectUID=" + BOGUS_UID + "&contentType=application/dicom");
		return classifyRetrieve(DicomWebServiceType.WADO_URI, send("GET", uri, null, token), "retrieve");
	}

	/**
	 * Classifies a query/retrieve probe: the request uses an intentionally non-existent
	 * UID, so a "not found / bad request" answer means the service is implemented (it
	 * understood the request), while "not implemented / method not allowed" means it is
	 * not.
	 */
	private static WebServiceProbe classifyRetrieve(DicomWebServiceType service, @Nullable HttpResponse<Void> response,
			String action) {
		if (response == null) {
			return new WebServiceProbe(service, WebServiceProbe.Support.INCONCLUSIVE, 0, PROBE_NOT_SENT);
		}
		int status = response.statusCode();
		if (status == 401 || status == 403) {
			return authRequired(service, status);
		}
		if (status == 200 || status == 204 || status == 206 || status == 400 || status == 404 || status == 406) {
			return new WebServiceProbe(service, WebServiceProbe.Support.SUPPORTED, status,
					"endpoint handled the " + action + " request (status " + status + ")");
		}
		if (status == 405 || status == 501) {
			return new WebServiceProbe(service, WebServiceProbe.Support.UNSUPPORTED, status, "status " + status);
		}
		return new WebServiceProbe(service, WebServiceProbe.Support.INCONCLUSIVE, status, "status " + status);
	}

	private static WebServiceProbe authRequired(DicomWebServiceType service, int status) {
		return new WebServiceProbe(service, WebServiceProbe.Support.INCONCLUSIVE, status,
				"authentication required or insufficient (status " + status + ")");
	}

	private @Nullable HttpResponse<Void> send(String method, URI uri, @Nullable String accept, @Nullable String token) {
		try {
			HttpClient.Builder clientBuilder = HttpClient.newBuilder()
				.connectTimeout(timeout)
				.followRedirects(HttpClient.Redirect.NEVER);
			if (HTTPS.equalsIgnoreCase(uri.getScheme())) {
				clientBuilder.sslContext(permissiveContext());
			}

			HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
				.timeout(timeout)
				.method(method, BodyPublishers.noBody());
			if (accept != null) {
				requestBuilder.header("Accept", accept);
			}
			if (token != null) {
				requestBuilder.header("Authorization", "Bearer " + token);
			}

			try (HttpClient client = clientBuilder.build()) {
				return client.send(requestBuilder.build(), BodyHandlers.discarding());
			}
		}
		catch (IOException | GeneralSecurityException ex) {
			log.debug("{} {} failed — {}", method, uri, ex.getMessage());
			return null;
		}
		catch (InterruptedException _) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	private @Nullable HttpResponse<String> sendForBody(URI uri, String accept, @Nullable String token) {
		try {
			HttpClient.Builder clientBuilder = HttpClient.newBuilder()
				.connectTimeout(timeout)
				.followRedirects(HttpClient.Redirect.NEVER);
			if (HTTPS.equalsIgnoreCase(uri.getScheme())) {
				clientBuilder.sslContext(permissiveContext());
			}

			HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
				.timeout(timeout)
				.GET()
				.header("Accept", accept);
			if (token != null) {
				requestBuilder.header("Authorization", "Bearer " + token);
			}

			try (HttpClient client = clientBuilder.build()) {
				return client.send(requestBuilder.build(), BodyHandlers.ofString());
			}
		}
		catch (IOException | GeneralSecurityException ex) {
			log.debug("GET {} failed — {}", uri, ex.getMessage());
			return null;
		}
		catch (InterruptedException _) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	private static @Nullable String headerOf(HttpResponse<Void> response, String primary, String fallback) {
		return response.headers()
			.firstValue(primary)
			.orElseGet(() -> response.headers().firstValue(fallback).orElse(null));
	}

	/**
	 * Strips a trailing {@code /studies} (and slash) so service paths derive from a base.
	 */
	private static String baseOf(String url) {
		String base = url.trim();
		if (base.endsWith("/")) {
			base = base.substring(0, base.length() - 1);
		}
		if (base.endsWith(STUDIES_PATH)) {
			base = base.substring(0, base.length() - STUDIES_PATH.length());
		}
		return base;
	}

	private static boolean isTrusted(X509Certificate[] chain) {
		try {
			TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			factory.init((java.security.KeyStore) null);
			for (TrustManager trustManager : factory.getTrustManagers()) {
				if (trustManager instanceof X509TrustManager x509) {
					x509.checkServerTrusted(chain, chain[0].getPublicKey().getAlgorithm());
					return true;
				}
			}
			return false;
		}
		catch (GeneralSecurityException _) {
			return false;
		}
	}

	private static X509Certificate[] toX509Chain(java.security.cert.Certificate[] chain) throws CertificateException {
		X509Certificate[] x509 = new X509Certificate[chain.length];
		for (int i = 0; i < chain.length; i++) {
			if (!(chain[i] instanceof X509Certificate certificate)) {
				throw new CertificateException("Unexpected non-X509 certificate in the chain");
			}
			x509[i] = certificate;
		}
		return x509;
	}

	// Sonar java:S4830 — certificate verification is intentionally disabled: this is a
	// read-only reachability/diagnostic probe (never uploads a study) whose purpose is to
	// reach an endpoint despite an expired/self-signed certificate and report the
	// certificate's validity and trust separately (see inspectTls / isTrusted), rather
	// than
	// failing the handshake the way a strict client would.
	@SuppressWarnings("java:S4830")
	private static SSLContext permissiveContext() throws GeneralSecurityException {
		TrustManager[] trustAll = { new X509TrustManager() {
			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) {
				// Diagnostic probe: accept any client certificate.
			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) {
				// Diagnostic probe: accept any server certificate so an
				// expired/self-signed
				// certificate can still be inspected and reported.
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}
		} };
		SSLContext context = SSLContext.getInstance("TLS");
		context.init(new KeyManager[0], trustAll, new SecureRandom());
		return context;
	}

}