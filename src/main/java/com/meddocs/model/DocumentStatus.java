package com.meddocs.model;

/**
 * Lifecycle of a document through the async ingestion pipeline (Stage 3).
 * PENDING on upload → INDEXED once all chunks are embedded & stored, or
 * FAILED (with a reason) if any step errors. Persisted as its String name.
 */
public enum DocumentStatus {
	PENDING,
	INDEXED,
	FAILED
}
