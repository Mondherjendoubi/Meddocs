package com.meddocs.ingest;

/**
 * Turns text into a fixed-length embedding vector. The interface is the seam that keeps
 * the rest of the pipeline provider-agnostic: ingestion and retrieval depend only on this,
 * so we can ship a {@link FakeEmbedder} now and swap in a real model (Stage 5) without
 * touching them.
 *
 * <p>Critically, the <em>same</em> embedder must embed both documents and queries — vectors
 * from different models live in incomparable spaces.
 */
public interface Embedder {

	/** Vector length produced by this embedder; must equal the {@code chunks.embedding} column dims. */
	int dimensions();

	/** Embed a single piece of text into a vector of length {@link #dimensions()}. */
	float[] embed(String text);
}
