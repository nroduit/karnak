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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.group.GroupSequenceProvider;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.validator.DestinationGroupSequenceProvider;
import org.karnak.backend.data.validator.DestinationGroupSequenceProvider.DestinationDicomGroup;
import org.karnak.backend.data.validator.DestinationGroupSequenceProvider.DestinationStowGroup;
import org.karnak.backend.enums.DestinationType;
import org.karnak.backend.enums.PseudonymType;

@GroupSequenceProvider(value = DestinationGroupSequenceProvider.class)
@Entity(name = "Destination")
@Table(name = "destination")
@NullUnmarked
public class DestinationEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 4835879567037810171L;

	private Long id;

	private String description;

	private DestinationType destinationType;

	private boolean activate;

	private String condition;

	private boolean activateTagMorphing;

	private boolean desidentification;

	private String issuerByDefault;

	@Setter
	@Getter
	private boolean skipIssuerOfPatientId;

	private PseudonymType pseudonymType;

	private String tag;

	private String delimiter;

	private Integer position;

	private String pseudonymUrl;

	private String responsePath;

	private String body;

	private String method;

	private String authConfig;

	private Boolean savePseudonym;

	private boolean filterBySOPClasses;

	private Set<SOPClassUIDEntity> SOPClassUIDEntityFilters = new HashSet<>();

	private List<KheopsAlbumsEntity> kheopsAlbumEntities;

	private ProjectEntity deIdentificationProjectEntity;

	private ProjectEntity tagMorphingProjectEntity;

	private ForwardNodeEntity forwardNodeEntity;

	// Activate notification
	private boolean activateNotification;

	// Build and email a DICOM conformance validation report for each study sent
	private boolean buildConformanceReport;

	// Also check that attribute values obey their VR length/format rules (PS3.5).
	// Off by default: real-world data deviates often and this is noisy.
	private boolean checkValueConformity;

	// Recurse the conformance checks through every sequence level (SR content tree,
	// enhanced multiframe functional groups, …) instead of only the first one.
	// Off by default: deeper recursion enlarges the snapshot kept in memory.
	private boolean deepSequenceValidation;

	// When true, this destination is a virtual "report-only" destination: the
	// de-identified / tag-morphed dataset is validated and a conformance report is
	// emailed, but nothing is forwarded to a real node (the DICOM is routed to
	// devnull). Delivery settings (AETitle/host/port or URL, transfer syntax,
	// notification, …) are therefore irrelevant and disabled in the UI.
	private boolean virtualDestination;

	// list of emails (comma separated) the conformance report is sent to. When
	// blank, the notification email list (notify) is used as a fallback.
	private String conformanceReportNotify;

	// list of emails (comma separated) used when the images have been sent (or
	// partially sent) to the final destination. Note: if an issue appears before
	// sending to the final destination then no email is delivered.
	private String notify;

	// Prefix of the email object when containing an issue. Default value: **ERROR**
	private String notifyObjectErrorPrefix;

	// Prefix of the email object when a rejection occurred. Default value: **REJECTED**
	private String notifyObjectRejectionPrefix;

	// Pattern of the email object, see
	// https://dzone.com/articles/java-string-format-examples.
	// Default value:
	// [Karnak Notification] %s %.30s
	private String notifyObjectPattern;

	// Values injected in the pattern [PatientID StudyDescription StudyDate
	// StudyInstanceUID]. Default value: PatientID,StudyDescription
	private String notifyObjectValues;

	// Interval in seconds for sending a notification (when no new image is arrived
	// in the archive folder). Default value: 45
	private Integer notifyInterval;

	// DICOM properties
	// the AETitle of the destination node.
	// mandatory[type=dicom]
	private String aeTitle;

	// the host or IP of the destination node.
	// mandatory[type=dicom]
	private String hostname;

	// the port of the destination node.
	// mandatory[type=dicom]
	private Integer port;

	// false by default; if "true" then use the destination AETitle as the calling
	// AETitle at the gateway side. Otherwise with "false" the calling AETitle is
	// the AETitle defined in the property "listener.aet" of the file
	// gateway.properties.
	private Boolean useaetdest;

	// STOW properties
	// the destination STOW-RS URL.
	// mandatory[type=stow]
	private String url;

	// headers for HTTP request.
	private String headers;

	// UID corresponding to the Transfer Syntax
	private String transferSyntax;

	// Transcode Only Uncompressed
	private boolean transcodeOnlyUncompressed;

	// Number of concurrent DICOM associations (forward connection pool)
	private Integer concurrentConnections = 1;

	// Use HTTP/2 for STOW-RS uploads. Default false (HTTP/1.1)
	private boolean http2;

	// Flag to know if there are some transfer activities on this destination
	private boolean transferInProgress;

	// Date of the last transfer for this destination
	private LocalDateTime lastTransfer;

	// Last date of the check of email notifications for this destination
	private LocalDateTime emailLastCheck;

	public DestinationEntity() {
		this(null);
	}

	protected DestinationEntity(DestinationType destinationType) {
		this.destinationType = destinationType;
		this.activate = true;
		this.condition = "";
		this.description = "";
		this.desidentification = false;
		this.issuerByDefault = "";
		this.skipIssuerOfPatientId = true;
		this.pseudonymType = PseudonymType.CACHE_EXTID;
		this.tag = null;
		this.delimiter = null;
		this.position = null;
		this.savePseudonym = null;
		this.pseudonymUrl = null;
		this.method = null;
		this.body = null;
		this.authConfig = null;
		this.responsePath = null;
		this.filterBySOPClasses = false;

		this.notify = "";
		this.notifyObjectErrorPrefix = "";
		this.notifyObjectRejectionPrefix = "";
		this.notifyObjectPattern = "";
		this.notifyObjectValues = "";
		this.notifyInterval = 0;
		this.aeTitle = "";
		this.hostname = "";
		this.port = 0;
		this.useaetdest = Boolean.FALSE;
		this.url = "";
		this.headers = "";

		this.transcodeOnlyUncompressed = false;
		this.http2 = false;
	}

	public static DestinationEntity ofDicomEmpty() {
		return new DestinationEntity(DestinationType.dicom);
	}

	public static DestinationEntity ofDicom(String description, String aeTitle, String hostname, int port,
			Boolean useaetdest) {
		DestinationEntity destinationEntity = new DestinationEntity(DestinationType.dicom);
		destinationEntity.setDescription(description);
		destinationEntity.setAeTitle(aeTitle);
		destinationEntity.setHostname(hostname);
		destinationEntity.setPort(port);
		destinationEntity.setUseaetdest(useaetdest);
		return destinationEntity;
	}

	public static DestinationEntity ofStowEmpty() {
		return new DestinationEntity(DestinationType.stow);
	}

	public static DestinationEntity ofStow(String description, String url, String headers) {
		DestinationEntity destinationEntity = new DestinationEntity(DestinationType.stow);
		destinationEntity.setDescription(description);
		destinationEntity.setUrl(url);
		destinationEntity.setHeaders(headers);
		return destinationEntity;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public boolean isActivate() {
		return activate;
	}

	public void setActivate(boolean activate) {
		this.activate = activate;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@NotNull(message = "Type is mandatory")
	@Column(name = "type")
	public DestinationType getDestinationType() {
		return destinationType;
	}

	public void setDestinationType(DestinationType destinationType) {
		this.destinationType = destinationType;
	}

	public boolean isDesidentification() {
		return desidentification;
	}

	public void setDesidentification(boolean desidentification) {
		this.desidentification = desidentification;
	}

	public String getIssuerByDefault() {
		return issuerByDefault;
	}

	public void setIssuerByDefault(String issuerByDefault) {
		this.issuerByDefault = issuerByDefault;
	}

	public boolean isFilterBySOPClasses() {
		return filterBySOPClasses;
	}

	public void setFilterBySOPClasses(boolean filterBySOPClasses) {
		this.filterBySOPClasses = filterBySOPClasses;
	}

	public String getNotify() {
		return notify;
	}

	public void setNotify(String notify) {
		this.notify = notify;
	}

	public String getNotifyObjectErrorPrefix() {
		return notifyObjectErrorPrefix;
	}

	public void setNotifyObjectErrorPrefix(String notifyObjectErrorPrefix) {
		this.notifyObjectErrorPrefix = notifyObjectErrorPrefix;
	}

	public String getNotifyObjectRejectionPrefix() {
		return notifyObjectRejectionPrefix;
	}

	public void setNotifyObjectRejectionPrefix(String notifyObjectRejectionPrefix) {
		this.notifyObjectRejectionPrefix = notifyObjectRejectionPrefix;
	}

	public String getNotifyObjectPattern() {
		return notifyObjectPattern;
	}

	public void setNotifyObjectPattern(String notifyObjectPattern) {
		this.notifyObjectPattern = notifyObjectPattern;
	}

	public String getNotifyObjectValues() {
		return notifyObjectValues;
	}

	public void setNotifyObjectValues(String notifyObjectValues) {
		this.notifyObjectValues = notifyObjectValues;
	}

	public Integer getNotifyInterval() {
		return notifyInterval;
	}

	public void setNotifyInterval(Integer notifyInterval) {
		this.notifyInterval = notifyInterval;
	}

	@NotBlank(groups = DestinationDicomGroup.class, message = "AETitle is mandatory")
	@Size(groups = DestinationDicomGroup.class, max = 16, message = "AETitle has more than 16 characters")
	public String getAeTitle() {
		return aeTitle;
	}

	public void setAeTitle(String aeTitle) {
		this.aeTitle = aeTitle;
	}

	@NotBlank(groups = DestinationDicomGroup.class, message = "Hostname is mandatory")
	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	@NotNull(groups = DestinationDicomGroup.class, message = "Port is mandatory")
	@Min(groups = DestinationDicomGroup.class, value = 1, message = "Port should be between 1 and 65535")
	@Max(groups = DestinationDicomGroup.class, value = 65535, message = "Port should be between 1 and 65535")
	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public Boolean getUseaetdest() {
		return useaetdest;
	}

	public void setUseaetdest(Boolean useaetdest) {
		this.useaetdest = useaetdest;
	}

	@NotBlank(groups = DestinationStowGroup.class, message = "URL is mandatory")
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Size(max = 4096, message = "Headers has more than 4096 characters")
	public String getHeaders() {
		return headers;
	}

	public void setHeaders(String headers) {
		this.headers = headers;
	}

	@JsonGetter("forwardNode")
	@ManyToOne
	@JoinColumn(name = "forward_node_id")
	public ForwardNodeEntity getForwardNodeEntity() {
		return forwardNodeEntity;
	}

	@JsonSetter("forwardNode")
	public void setForwardNodeEntity(ForwardNodeEntity forwardNodeEntity) {
		this.forwardNodeEntity = forwardNodeEntity;
	}

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "sop_class_filter", joinColumns = @JoinColumn(name = "destination_id"),
			inverseJoinColumns = @JoinColumn(name = "sop_class_uid_id"))
	public Set<SOPClassUIDEntity> getSOPClassUIDEntityFilters() {
		return this.SOPClassUIDEntityFilters;
	}

	public void setSOPClassUIDEntityFilters(Set<SOPClassUIDEntity> sopClassUIDEntities) {
		this.SOPClassUIDEntityFilters = sopClassUIDEntities;
	}

	public Set<String> retrieveSOPClassUIDFiltersName() {
		return SOPClassUIDEntityFilters.stream().map(SOPClassUIDEntity::getName).collect(Collectors.toSet());
	}

	public PseudonymType getPseudonymType() {
		return pseudonymType;
	}

	public void setPseudonymType(PseudonymType pseudonymType) {
		this.pseudonymType = pseudonymType;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getDelimiter() {
		return delimiter;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	public Integer getPosition() {
		return position;
	}

	public void setPosition(Integer position) {
		this.position = position;
	}

	public Boolean getSavePseudonym() {
		return savePseudonym;
	}

	public void setSavePseudonym(Boolean savePseudonym) {
		this.savePseudonym = savePseudonym;
	}

	public String getPseudonymUrl() {
		return pseudonymUrl;
	}

	public void setPseudonymUrl(String pseudonymUrl) {
		this.pseudonymUrl = pseudonymUrl;
	}

	public String getResponsePath() {
		return responsePath;
	}

	public void setResponsePath(String responsePath) {
		this.responsePath = responsePath;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getAuthConfig() {
		return authConfig;
	}

	public void setAuthConfig(String authConfig) {
		this.authConfig = authConfig;
	}

	@JsonGetter("kheopsAlbums")
	@OneToMany(mappedBy = "destinationEntity", cascade = CascadeType.REMOVE, fetch = FetchType.EAGER)
	public List<KheopsAlbumsEntity> getKheopsAlbumEntities() {
		return kheopsAlbumEntities;
	}

	@JsonSetter("kheopsAlbums")
	public void setKheopsAlbumEntities(List<KheopsAlbumsEntity> kheopsAlbumEntities) {
		this.kheopsAlbumEntities = kheopsAlbumEntities;
	}

	@JsonGetter("deIdentificationProject")
	@ManyToOne
	@JoinColumn(name = "deidentification_project_id")
	public ProjectEntity getDeIdentificationProjectEntity() {
		return deIdentificationProjectEntity;
	}

	@JsonSetter("deIdentificationProject")
	public void setDeIdentificationProjectEntity(ProjectEntity deIdentificationProjectEntity) {
		this.deIdentificationProjectEntity = deIdentificationProjectEntity;
	}

	@JsonGetter("tagMorphingProject")
	@ManyToOne
	@JoinColumn(name = "tag_morphing_project_id")
	public ProjectEntity getTagMorphingProjectEntity() {
		return tagMorphingProjectEntity;
	}

	@JsonSetter("tagMorphingProject")
	public void setTagMorphingProjectEntity(ProjectEntity tagMorphingProjectEntity) {
		this.tagMorphingProjectEntity = tagMorphingProjectEntity;
	}

	public boolean isActivateNotification() {
		return activateNotification;
	}

	public void setActivateNotification(boolean activateNotification) {
		this.activateNotification = activateNotification;
	}

	public boolean isBuildConformanceReport() {
		return buildConformanceReport;
	}

	public void setBuildConformanceReport(boolean buildConformanceReport) {
		this.buildConformanceReport = buildConformanceReport;
	}

	public boolean isCheckValueConformity() {
		return checkValueConformity;
	}

	public void setCheckValueConformity(boolean checkValueConformity) {
		this.checkValueConformity = checkValueConformity;
	}

	public boolean isDeepSequenceValidation() {
		return deepSequenceValidation;
	}

	public void setDeepSequenceValidation(boolean deepSequenceValidation) {
		this.deepSequenceValidation = deepSequenceValidation;
	}

	public boolean isVirtualDestination() {
		return virtualDestination;
	}

	public void setVirtualDestination(boolean virtualDestination) {
		this.virtualDestination = virtualDestination;
	}

	public String getConformanceReportNotify() {
		return conformanceReportNotify;
	}

	public void setConformanceReportNotify(String conformanceReportNotify) {
		this.conformanceReportNotify = conformanceReportNotify;
	}

	public String getTransferSyntax() {
		return transferSyntax;
	}

	public void setTransferSyntax(String transferSyntax) {
		this.transferSyntax = transferSyntax;
	}

	public boolean isTranscodeOnlyUncompressed() {
		return transcodeOnlyUncompressed;
	}

	public void setTranscodeOnlyUncompressed(boolean transcodeOnlyUncompressed) {
		this.transcodeOnlyUncompressed = transcodeOnlyUncompressed;
	}

	@Column(name = "concurrent_connections")
	@Min(groups = DestinationDicomGroup.class, value = 1, message = "Concurrent connections must be at least 1")
	@Max(groups = DestinationDicomGroup.class, value = 50, message = "Concurrent connections must be 50 or less")
	public Integer getConcurrentConnections() {
		return concurrentConnections;
	}

	public void setConcurrentConnections(Integer concurrentConnections) {
		this.concurrentConnections = concurrentConnections;
	}

	public boolean isHttp2() {
		return http2;
	}

	public void setHttp2(boolean http2) {
		this.http2 = http2;
	}

	public boolean isTransferInProgress() {
		return transferInProgress;
	}

	public void setTransferInProgress(boolean transferInProgress) {
		this.transferInProgress = transferInProgress;
	}

	public LocalDateTime getEmailLastCheck() {
		return emailLastCheck;
	}

	public void setEmailLastCheck(LocalDateTime emailLastCheck) {
		this.emailLastCheck = emailLastCheck;
	}

	public LocalDateTime getLastTransfer() {
		return lastTransfer;
	}

	public void setLastTransfer(LocalDateTime lastTransfer) {
		this.lastTransfer = lastTransfer;
	}

	public boolean isActivateTagMorphing() {
		return activateTagMorphing;
	}

	public void setActivateTagMorphing(boolean activateTagMorphing) {
		this.activateTagMorphing = activateTagMorphing;
	}

	/**
	 * Informs if this object matches with the filter as text.
	 * @param filterText the filter as text.
	 * @return true if this object matches with the filter as text; false otherwise.
	 */
	public boolean matchesFilter(String filterText) {
		return contains(description, filterText) //
				|| contains(notify, filterText) //
				|| contains(notifyObjectErrorPrefix, filterText) //
				|| contains(notifyObjectRejectionPrefix, filterText) //
				|| contains(notifyObjectPattern, filterText) //
				|| contains(notifyObjectValues, filterText) //
				|| contains(aeTitle, filterText) //
				|| contains(hostname, filterText) //
				|| equals(port, filterText) //
				|| contains(url, filterText) //
				|| contains(headers, filterText);
	}

	private boolean contains(String value, String filterText) {
		return value != null && value.contains(filterText);
	}

	private boolean equals(Integer value, String filterText) {
		return value != null && value.toString().equals(filterText);
	}

	@Override
	public String toString() {
		if (destinationType != null) {
			switch (destinationType) {
				case dicom:
					return "Destination [id=" + id + ", description=" + description + ", type=" + destinationType
							+ ", notify=" + notify + ", notifyObjectErrorPrefix=" + notifyObjectErrorPrefix
							+ ", notifyObjectRejectionPrefix=" + notifyObjectRejectionPrefix + ", notifyObjectPattern="
							+ notifyObjectPattern + ", notifyObjectValues=" + notifyObjectValues + ", notifyInterval="
							+ notifyInterval + ", aeTitle=" + aeTitle + ", hostname=" + hostname + ", port=" + port
							+ ", useaetdest=" + useaetdest + "]";
				case stow:
					return "Destination [id=" + id + ", description=" + description + ", type=" + destinationType
							+ ", notify=" + notify + ", notifyObjectErrorPrefix=" + notifyObjectErrorPrefix
							+ ", notifyObjectRejectionPrefix=" + notifyObjectRejectionPrefix + ", notifyObjectPattern="
							+ notifyObjectPattern + ", notifyObjectValues=" + notifyObjectValues + ", notifyInterval="
							+ notifyInterval + ", url=" + url + ", headers=" + headers + "]";
			}
		}
		return "Destination [id=" + id + ", description=" + description + ", type=" + destinationType + ", notify="
				+ notify + ", notifyObjectErrorPrefix=" + notifyObjectErrorPrefix + ", notifyObjectRejectionPrefix="
				+ notifyObjectRejectionPrefix + ", notifyObjectPattern=" + notifyObjectPattern + ", notifyObjectValues="
				+ notifyObjectValues + ", notifyInterval=" + notifyInterval + "]";
	}

	public String retrieveStringReference() {
		if (destinationType == null) {
			return "Type of destination is unknown";
		}
		return switch (destinationType) {
			case dicom -> getAeTitle();
			case stow -> getUrl() + ":" + getPort();
		};
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DestinationEntity that = (DestinationEntity) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	public String toStringDicomNotificationDestination() {
		return "Host=" + getHostname() + " AET=" + getAeTitle() + " Port=" + getPort();
	}

}
