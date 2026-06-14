package com.meddocs.rag;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Deterministic stand-in for a real chat model: returns a fixed, canned answer regardless of
 * input. Lets the whole query → retrieve → generate path run end-to-end (and its tests pass)
 * with no model download or API key while the plumbing is built (Stage 4).
 *
 * <p>It does <em>not</em> reason over the prompt — answer quality is meaningless until the real
 * LLM lands (Stage 5). Active by default; Stage 5 sets {@code meddocs.chat=ollama} to replace it.
 */
@Component
@ConditionalOnProperty(name = "meddocs.chat", havingValue = "fake", matchIfMissing = true)
public class FakeChatModel implements ChatModel {

	private static final String CANNED_ANSWER =
			"Based on the provided context, here is a grounded answer. "
					+ "(This is a placeholder response from the fake chat model.)";

	@Override
	public String generate(String systemPrompt, String userPrompt) {
		return CANNED_ANSWER;
	}
}
