package com.meddocs.rag.dto;

import java.util.List;

/**
 * The answer to a query. When {@code grounded} is false the question couldn't be answered
 * from the user's documents: {@code answer} is the refusal message and {@code sources} is empty.
 */
public record QueryResponse(
		boolean grounded,
		String answer,
		List<SourceResponse> sources) {

	/** A refusal: retrieval was too weak (or empty) to ground an answer. */
	public static QueryResponse refusal(String message) {
		return new QueryResponse(false, message, List.of());
	}

	/** A grounded answer backed by the given cited sources. */
	public static QueryResponse grounded(String answer, List<SourceResponse> sources) {
		return new QueryResponse(true, answer, sources);
	}
}
