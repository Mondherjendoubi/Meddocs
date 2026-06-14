package com.meddocs.rag;

/**
 * One retrieved chunk and its similarity to the query, projected straight from the native
 * vector search. {@code score} is cosine similarity in [0, 1] (1 = identical direction),
 * derived as {@code 1 - cosineDistance}.
 */
public record VectorSearchResult(
		Long chunkId,
		Long documentId,
		String sourceLabel,
		String sectionRef,
		String content,
		double score) {
}
