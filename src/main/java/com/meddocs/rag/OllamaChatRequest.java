package com.meddocs.rag;

import java.util.List;

/**
 * Request body for Ollama's {@code POST /api/chat}. {@code stream=false} forces a single,
 * fully-buffered JSON response (no SSE) so we can deserialize it in one shot.
 */
record OllamaChatRequest(String model, List<Message> messages, boolean stream) {

	/** One chat turn: {@code role} is {@code system} or {@code user}. */
	record Message(String role, String content) {
	}

	/** Builds the standard system + user two-message conversation. */
	static OllamaChatRequest of(String model, String systemPrompt, String userPrompt) {
		return new OllamaChatRequest(
				model,
				List.of(new Message("system", systemPrompt), new Message("user", userPrompt)),
				false);
	}
}
