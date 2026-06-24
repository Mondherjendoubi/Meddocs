package com.meddocs.ingest;

/**
 * Thrown when re-embedding a user's chunks can't complete because a downstream dependency
 * failed — e.g. the active {@link Embedder} (Ollama) errored. Surfaced as a 503
 * {@code ProblemDetail}: the failure is transient/infrastructure, not the caller's fault.
 *
 * <p>Lives in {@code com.meddocs.ingest} so this package needn't depend on the RAG package's
 * {@code RagException}; {@link com.meddocs.web.ApiExceptionHandler} maps it to 503.
 */
public class ReindexException extends RuntimeException {

	public ReindexException(String message, Throwable cause) {
		super(message, cause);
	}
}
