package com.meddocs.rag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response body for Ollama's {@code POST /api/chat}. We only read {@code message.content};
 * unknown fields (timings, token counts, etc.) are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record OllamaChatResponse(Message message) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Message(String content) {
	}
}
