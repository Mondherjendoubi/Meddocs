package com.meddocs.ingest;

import com.meddocs.ollama.OllamaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Real {@link Embedder} backed by a local Ollama server ({@code POST /api/embed}). Replaces
 * {@link FakeEmbedder} when {@code meddocs.embedder=ollama}.
 *
 * <p>Fixed at 384 dimensions to match the {@code chunks.embedding} pgvector column. As a
 * fail-fast guard, an embedding of any other length throws {@link IllegalStateException}: a
 * mis-configured model (wrong dims) must never silently write corrupt vectors into the DB.
 */
@Component
@ConditionalOnProperty(name = "meddocs.embedder", havingValue = "ollama")
@RequiredArgsConstructor
public class OllamaEmbedder implements Embedder {

	/** 384 = all-MiniLM-L6-v2, matching the pgvector vector(384) column. */
	private static final int DIMENSIONS = 384;

	private final RestClient ollamaRestClient;
	private final OllamaProperties properties;

	@Override
	public int dimensions() {
		return DIMENSIONS;
	}

	@Override
	public float[] embed(String text) {
		OllamaEmbedRequest request = new OllamaEmbedRequest(properties.embedModel(), text == null ? "" : text);
		OllamaEmbedResponse response = ollamaRestClient.post()
				.uri("/api/embed")
				.body(request)
				.retrieve()
				.body(OllamaEmbedResponse.class);

		List<float[]> embeddings = response == null ? null : response.embeddings();
		if (embeddings == null || embeddings.isEmpty() || embeddings.get(0) == null) {
			throw new IllegalStateException("Ollama returned no embedding for /api/embed");
		}
		float[] vector = embeddings.get(0);
		if (vector.length != DIMENSIONS) {
			throw new IllegalStateException(
					"Ollama embedding has " + vector.length + " dims, expected " + DIMENSIONS
							+ " (wrong embed model '" + properties.embedModel() + "'?)");
		}
		return vector;
	}
}
