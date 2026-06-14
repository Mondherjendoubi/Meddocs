package com.meddocs.rag;

import java.util.List;

/**
 * Builds the prompts handed to the {@link ChatModel}. The system instruction pins the model
 * to the retrieved context (anti-hallucination); the user prompt stitches the numbered
 * passages — each tagged with its source label and section — ahead of the question.
 */
public final class PromptTemplates {

	public static final String SYSTEM_INSTRUCTION =
			"Answer ONLY from the provided context. Cite your sources by their label. "
					+ "If the context is insufficient to answer, say you don't know.";

	private PromptTemplates() {
	}

	/** Format the retrieved passages plus the question into a single user prompt. */
	public static String userPrompt(List<VectorSearchResult> passages, String question) {
		StringBuilder builder = new StringBuilder();
		builder.append("Context:\n");
		int index = 1;
		for (VectorSearchResult passage : passages) {
			builder.append('[').append(index++).append("] ");
			builder.append(passage.sourceLabel());
			if (passage.sectionRef() != null && !passage.sectionRef().isBlank()) {
				builder.append(" (").append(passage.sectionRef()).append(')');
			}
			builder.append('\n');
			builder.append(passage.content()).append("\n\n");
		}
		builder.append("Question: ").append(question);
		return builder.toString();
	}
}
