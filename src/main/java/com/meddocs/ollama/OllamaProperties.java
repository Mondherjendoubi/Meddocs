package com.meddocs.ollama;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Connection + model settings for the local Ollama server ({@code meddocs.ollama.*}).
 * Shared by both the chat ({@link com.meddocs.rag.OllamaChatModel}) and embedding
 * ({@link com.meddocs.ingest.OllamaEmbedder}) providers, which talk to the same daemon.
 *
 * @param baseUrl        root URL of the Ollama HTTP API (e.g. {@code http://localhost:11434})
 * @param chatModel      model tag used for {@code /api/chat} (e.g. {@code llama3.1:latest})
 * @param embedModel     model tag used for {@code /api/embed} (e.g. {@code all-minilm})
 * @param connectTimeout how long to wait establishing the TCP connection
 * @param readTimeout    how long to wait for a response (generation can be slow)
 */
@ConfigurationProperties("meddocs.ollama")
public record OllamaProperties(
		String baseUrl,
		String chatModel,
		String embedModel,
		Duration connectTimeout,
		Duration readTimeout) {
}
