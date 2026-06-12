package com.meddocs.ingest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Deterministic stand-in for a real embedding model: maps text to a stable, unit-length
 * vector seeded from a hash of the text. Lets the whole ingest → store → retrieve pipeline
 * run with no API key and no model download while the plumbing is built (Stage 3–4).
 *
 * <p>It is <em>not</em> semantic — unrelated texts get unrelated vectors — so retrieval
 * quality is meaningless until the real embedder lands (Stage 5). What it gives us is
 * determinism: the same text always yields the same vector, which is exactly what fast,
 * repeatable tests need.
 *
 * <p>Active by default; Stage 5 sets {@code meddocs.embedder=transformers} to replace it.
 */
@Component
@ConditionalOnProperty(name = "meddocs.embedder", havingValue = "fake", matchIfMissing = true)
public class FakeEmbedder implements Embedder {

	/** 384 = all-MiniLM-L6-v2, matching the pgvector vector(384) column the real embedder will use. */
	private static final int DIMENSIONS = 384;

	@Override
	public int dimensions() {
		return DIMENSIONS;
	}

	@Override
	public float[] embed(String text) {
		String input = text == null ? "" : text;
		Random rng = new Random(seedFor(input));

		float[] vector = new float[DIMENSIONS];
		double sumOfSquares = 0.0;
		for (int i = 0; i < DIMENSIONS; i++) {
			float component = (float) rng.nextGaussian();
			vector[i] = component;
			sumOfSquares += (double) component * component;
		}

		// Normalize to unit length so cosine distance behaves consistently.
		double norm = Math.sqrt(sumOfSquares);
		if (norm > 0) {
			for (int i = 0; i < DIMENSIONS; i++) {
				vector[i] /= (float) norm;
			}
		}
		return vector;
	}

	/** Stable 64-bit hash (same across JVMs, unlike a salted hashCode) used to seed the RNG. */
	private static long seedFor(String text) {
		long hash = 1125899906842597L; // a large prime
		for (int i = 0; i < text.length(); i++) {
			hash = 31 * hash + text.charAt(i);
		}
		return hash;
	}
}
