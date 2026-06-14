package com.meddocs.rag;

/**
 * Thrown when the RAG core can't complete a query because a downstream dependency failed —
 * e.g. the embedder, the vector search, or the chat model errored. Surfaced as a 5xx
 * {@code ProblemDetail} (the failure is on our side, not the caller's).
 */
public class RagException extends RuntimeException {

	public RagException(String message, Throwable cause) {
		super(message, cause);
	}
}
