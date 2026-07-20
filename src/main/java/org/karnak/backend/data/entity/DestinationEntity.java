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
import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Getter
@Setter
public class DestinationEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	private String description;

	@NotNull(message = "Type is mandatory")
	@Column(name = "type")
	private DestinationType destinationType;

	private boolean activate;

	private String condition;

	private boolean activateTagMorphing;

	private boolean desidentification;

	private String issuerByDefault;

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

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "sop_class_filter", joinColumns = @JoinColumn(name = "destination_id"),
			inverseJoinColumns = @JoinColumn(name = "sop_class_uid_id"))
	private Set<SOPClassUIDEntity> SOPClassUIDEntityFilters = new HashSet<>();

	@OneToMany(mappedBy = "destinationEntity", cascade = CascadeType.REMOVE, fetch = FetchType.EAGER)
	private List<KheopsAlbumsEntity> kheopsAlbumEntities;

	@ManyToOne
	@JoinColumn(name = "deidentification_project_id")
	private ProjectEntity deIdentificationProjectEntity;

	@ManyToOne
	@JoinColumn(name = "tag_morphing_project_id")
	private ProjectEntity tagMorphingProjectEntity;

	@ManyToOne
	@JoinColumn(name = "forward_node_id")
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
	@NotBlank(groups = DestinationDicomGroup.class, message = "AETitle is mandatory")
	@Size(groups = DestinationDicomGroup.class, max = 16, message = "AETitle has more than 16 characters")
	private String aeTitle;

	// the host or IP of the destination node.
	// mandatory[type=dicom]
	@NotBlank(groups = DestinationDicomGroup.class, message = "Hostname is mandatory")
	private String hostname;

	// the port of the destination node.
	// mandatory[type=dicom]
	@NotNull(groups = DestinationDicomGroup.class, message = "Port is mandatory")
	@Min(groups = DestinationDicomGroup.class, value = 1, message = "Port should be between 1 and 65535")
	@Max(groups = DestinationDicomGroup.class, value = 65535, message = "Port should be between 1 and 65535")
	private Integer port;

	// false by default; if "true" then use the destination AETitle as the calling
	// AETitle at the gateway side. Otherwise with "false" the calling AETitle is
	// the AETitle defined in the property "listener.aet" of the file
	// gateway.properties.
	private Boolean useaetdest;

	// STOW properties
	// the destination STOW-RS URL.
	// mandatory[type=stow]
	@NotBlank(groups = DestinationStowGroup.class, message = "URL is mandatory")
	private String url;

	// headers for HTTP request.
	@Size(max = 4096, message = "Headers has more than 4096 characters")
	private String headers;

	// UID corresponding to the Transfer Syntax
	private String transferSyntax;

	// Transcode Only Uncompressed
	private boolean transcodeOnlyUncompressed;

	// Number of concurrent DICOM associations (forward connection pool)
	@Column(name = "concurrent_connections")
	@Min(groups = DestinationDicomGroup.class, value = 1, message = "Concurrent connections must be at least 1")
	@Max(groups = DestinationDicomGroup.class, value = 50, message = "Concurrent connections must be 50 or less")
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

	@JsonIgnore
	public ForwardNodeEntity getForwardNodeEntity() {
		return forwardNodeEntity;
	}

	@JsonSetter("forwardNode")
	public void setForwardNodeEntity(ForwardNodeEntity forwardNodeEntity) {
		this.forwardNodeEntity = forwardNodeEntity;
	}

	public Set<String> retrieveSOPClassUIDFiltersName() {
		return SOPClassUIDEntityFilters.stream().map(SOPClassUIDEntity::getName).collect(Collectors.toSet());
	}

	@JsonGetter("kheopsAlbums")
	public List<KheopsAlbumsEntity> getKheopsAlbumEntities() {
		return kheopsAlbumEntities;
	}

	@JsonSetter("kheopsAlbums")
	public void setKheopsAlbumEntities(List<KheopsAlbumsEntity> kheopsAlbumEntities) {
		this.kheopsAlbumEntities = kheopsAlbumEntities;
	}

	@JsonGetter("deIdentificationProject")
	public ProjectEntity getDeIdentificationProjectEntity() {
		return deIdentificationProjectEntity;
	}

	@JsonSetter("deIdentificationProject")
	public void setDeIdentificationProjectEntity(ProjectEntity deIdentificationProjectEntity) {
		this.deIdentificationProjectEntity = deIdentificationProjectEntity;
	}

	@JsonGetter("tagMorphingProject")
	public ProjectEntity getTagMorphingProjectEntity() {
		return tagMorphingProjectEntity;
	}

	@JsonSetter("tagMorphingProject")
	public void setTagMorphingProjectEntity(ProjectEntity tagMorphingProjectEntity) {
		this.tagMorphingProjectEntity = tagMorphingProjectEntity;
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
