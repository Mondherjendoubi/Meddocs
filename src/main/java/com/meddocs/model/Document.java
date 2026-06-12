package com.meddocs.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // required by JPA, not for app code
public class Document {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Owner of this document — drives per-user isolation in retrieval (Stage 4). */
	@Setter
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	/** Human-friendly citation label; defaults to the filename if blank. */
	@Setter
	@Column(name = "source_label", nullable = false)
	private String sourceLabel;

	@Setter
	@Column(name = "filename")
	private String filename;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private DocumentStatus status = DocumentStatus.PENDING;

	/** Populated only when status == FAILED. */
	@Setter
	@Column(name = "failure_reason")
	private String failureReason;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public Document(User user, String sourceLabel, String filename) {
		this.user = user;
		this.sourceLabel = sourceLabel;
		this.filename = filename;
	}
}
