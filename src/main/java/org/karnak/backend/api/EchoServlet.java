/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.api;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serial;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.karnak.backend.dicom.DicomForwardDestination;
import org.karnak.backend.dicom.ForwardDestination;
import org.karnak.backend.dicom.ForwardDicomNode;
import org.karnak.backend.dicom.WebForwardDestination;
import org.karnak.backend.service.gateway.GatewaySetUpService;
import org.karnak.backend.util.ServletUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.weasis.dicom.op.Echo;
import org.weasis.dicom.param.AdvancedParams;
import org.weasis.dicom.param.ConnectOptions;
import org.weasis.dicom.param.DicomNode;
import org.weasis.dicom.param.DicomState;

@WebServlet(urlPatterns = "/echo")
@Slf4j
public class EchoServlet extends HttpServlet {

	@Serial
	private static final long serialVersionUID = -8349040600894140520L;

	@Autowired
	private GatewaySetUpService globalConfig;

	@Override
	public final void init() throws ServletException {
		if (globalConfig == null) {
			log.error("EchoServlet service cannot start: GatewaySetUpService is missing.");
			destroy();
		}
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse res) {
		res.setContentType("text/xml");
		PrintWriter out = null;
		try {
			out = res.getWriter();
		}
		catch (IOException e) {
			String errorMsg = "Cannot write response";
			log.error(errorMsg);
			ServletUtil.sendResponseError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMsg);
			return;
		}
		String aet = req.getParameter("srcAET");
		if (globalConfig == null) {
			String errorMsg = "Missing 'GlobalConfig' from current ServletContext";
			log.error(errorMsg);
			ServletUtil.sendResponseError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMsg);
			return;
		}
		// Echo service, only work for the out stream configuration
		AdvancedParams params = new AdvancedParams();
		ConnectOptions connectOptions = new ConnectOptions();
		connectOptions.setConnectTimeout(3000);
		connectOptions.setAcceptTimeout(5000);
		params.setConnectOptions(connectOptions);
		StringBuilder sb = new StringBuilder("<destinations>");

		Optional<ForwardDicomNode> srcNode = globalConfig.getDestinationNode(aet);
		if (srcNode.isPresent()) {
			List<ForwardDestination> list = globalConfig.getDestinations(srcNode.get());
			for (ForwardDestination val : list) {
				if (val instanceof DicomForwardDestination) {
					DicomNode calledNode = ((DicomForwardDestination) val).getStreamSCU().getCalledNode();
					// TODO isUseAetDest
					DicomState dicomState = Echo.process(params, srcNode.get(), calledNode);
					sb.append("<destination ");
					sb.append("aet=");
					sb.append(calledNode.getAet());
					sb.append(">");
					sb.append(dicomState.getStatus());
					sb.append("</destination>\n");
				}
				else if (val instanceof WebForwardDestination) {
					WebForwardDestination d = (WebForwardDestination) val;
					sb.append("<destination ");
					sb.append("url=");
					sb.append(d.getRequestURL());
					sb.append(">");
					// To implement
					// sb.append(d.getStowRS().);
					sb.append("</destination>\n");
				}
			}
		}
		sb.append("</destinations>\n");
		out.println(sb);
	}

}
