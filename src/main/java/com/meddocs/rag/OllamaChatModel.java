package com.meddocs.rag;

import com.meddocs.ollama.OllamaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Real {@link ChatModel} backed by a local Ollama server ({@code POST /api/chat}). Replaces
 * {@link FakeChatModel} when {@code meddocs.chat=ollama}.
 *
 * <p>HTTP/IO failures (server down, 5xx, timeout) are left to propagate as RuntimeExceptions:
 * {@link DefaultRagService} wraps the {@code generate} call in a catch that maps them to a
 * transient {@code RagException} (HTTP 503), so this class deliberately does no error handling.
 */
@Component
@ConditionalOnProperty(name = "meddocs.chat", havingValue = "ollama")
@RequiredArgsConstructor
public class OllamaChatModel implements ChatModel {

	private final RestClient ollamaRestClient;
	private final OllamaProperties properties;

	@Override
	public String generate(String systemPrompt, String userPrompt) {
		OllamaChatRequest request = OllamaChatRequest.of(properties.chatModel(), systemPrompt, userPrompt);
		OllamaChatResponse response = ollamaRestClient.post()
				.uri("/api/chat")
				.body(request)
				.retrieve()
				.body(OllamaChatResponse.class);
		if (response == null || response.message() == null) {
			throw new IllegalStateException("Ollama returned no message content for /api/chat");
		}
		String content = response.message().content();
		if (content == null || content.isBlank()) {
			// A well-formed 200 with empty content (model not loaded, aborted generation) must
			// not pass as a grounded answer with an empty body — treat it as a generation failure.
			throw new IllegalStateException("Ollama returned blank message content for /api/chat");
		}
		return content;
	}
}
