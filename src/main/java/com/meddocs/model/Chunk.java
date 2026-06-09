package com.meddocs.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
	name = "chunks",
	uniqueConstraints = @UniqueConstraint(columnNames = {"document_id", "chunk_index"})
)
public class Chunk {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "document_id", nullable = false)
	private Document document;

	/** 0-based position of this chunk within its document. */
	@Column(name = "chunk_index", nullable = false)
	private int chunkIndex;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	/** Optional locator within the source (e.g. "p. 4", "§2.1"). */
	@Column(name = "section_ref")
	private String sectionRef;

	/**
	 * The embedding for {@link #content}, fixed at 384 dims (all-MiniLM-L6-v2).
	 * Mapped to the pgvector vector(384) column via hibernate-vector.
	 */
	@JdbcTypeCode(SqlTypes.VECTOR)
	@Array(length = 384)
	@Column(name = "embedding")
	private float[] embedding;

	protected Chunk() {
	}

	public Chunk(Document document, int chunkIndex, String content, String sectionRef, float[] embedding) {
		this.document = document;
		this.chunkIndex = chunkIndex;
		this.content = content;
		this.sectionRef = sectionRef;
		this.embedding = embedding;
	}

	public Long getId() {
		return id;
	}

	public Document getDocument() {
		return document;
	}

	public void setDocument(Document document) {
		this.document = document;
	}

	public int getChunkIndex() {
		return chunkIndex;
	}

	public void setChunkIndex(int chunkIndex) {
		this.chunkIndex = chunkIndex;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getSectionRef() {
		return sectionRef;
	}

	public void setSectionRef(String sectionRef) {
		this.sectionRef = sectionRef;
	}

	public float[] getEmbedding() {
		return embedding;
	}

	public void setEmbedding(float[] embedding) {
		this.embedding = embedding;
	}
}
