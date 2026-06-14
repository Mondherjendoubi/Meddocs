package com.meddocs.rag;

/**
 * Renders a {@code float[]} embedding as the text literal pgvector expects, e.g.
 * {@code "[0.013,-0.22,1.0]"} — square brackets, comma-separated, no spaces. Used to bind
 * a query vector into a native SQL parameter cast with {@code CAST(:queryVec AS vector)}.
 */
public final class EmbeddingFormat {

	private EmbeddingFormat() {
	}

	/** Format {@code vector} as a pgvector literal: {@code "[v0,v1,...]"} with no spaces. */
	public static String toPgVectorLiteral(float[] vector) {
		StringBuilder builder = new StringBuilder(vector.length * 8 + 2);
		builder.append('[');
		for (int i = 0; i < vector.length; i++) {
			if (i > 0) {
				builder.append(',');
			}
			builder.append(vector[i]);
		}
		builder.append(']');
		return builder.toString();
	}
}
