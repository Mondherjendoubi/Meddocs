package com.meddocs.rag.dto;

/**
 * A cited passage backing a grounded answer: enough to let the client show where the answer
 * came from. {@code snippet} is the chunk content truncated to the configured limit.
 */
public record SourceResponse(
		Long documentId,
		String sourceLabel,
		String sectionRef,
		String snippet,
		double score) {
}
