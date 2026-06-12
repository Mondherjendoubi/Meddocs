package com.meddocs.ingest.dto;

import com.meddocs.model.Document;
import com.meddocs.model.DocumentStatus;

import java.time.Instant;

/** Public view of a document and its ingestion status. */
public record DocumentResponse(
		Long id,
		String sourceLabel,
		String filename,
		DocumentStatus status,
		String failureReason,
		Instant createdAt,
		long chunkCount) {

	public static DocumentResponse from(Document document, long chunkCount) {
		return new DocumentResponse(
				document.getId(),
				document.getSourceLabel(),
				document.getFilename(),
				document.getStatus(),
				document.getFailureReason(),
				document.getCreatedAt(),
				chunkCount);
	}
}
