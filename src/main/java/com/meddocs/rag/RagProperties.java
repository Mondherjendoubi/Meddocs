package com.meddocs.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunables for retrieval-augmented generation ({@code meddocs.rag.*}).
 *
 * @param topK                number of nearest chunks to retrieve per query
 * @param similarityThreshold minimum top-hit cosine similarity to answer; below this we refuse
 * @param refusalMessage      canned reply returned when retrieval is too weak to ground an answer
 * @param snippetMaxChars     max characters of chunk content surfaced in each cited source
 */
@ConfigurationProperties("meddocs.rag")
public record RagProperties(
		int topK,
		double similarityThreshold,
		String refusalMessage,
		int snippetMaxChars) {
}
