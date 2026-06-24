package com.meddocs.ingest;

import com.meddocs.ollama.OllamaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit tests for {@link OllamaEmbedder} using {@link MockRestServiceServer} — no real Ollama.
 * Verifies a 384-length response parses to {@code float[384]}, {@link OllamaEmbedder#dimensions()},
 * that a wrong-length response fails fast with {@link IllegalStateException} (corruption guard),
 * and that a 5xx surfaces as a {@link RuntimeException}.
 */
class OllamaEmbedderTest {

	private static final String BASE_URL = "http://localhost:11434";

	private MockRestServiceServer server;
	private OllamaEmbedder embedder;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		server = MockRestServiceServer.bindTo(builder).build();
		OllamaProperties properties = new OllamaProperties(
				BASE_URL, "llama3.1:latest", "all-minilm", Duration.ofSeconds(5), Duration.ofSeconds(60));
		embedder = new OllamaEmbedder(builder.build(), properties);
	}

	@Test
	void dimensionsIs384() {
		assertThat(embedder.dimensions()).isEqualTo(384);
	}

	@Test
	void postsEmbedBodyAndReturnsVector() {
		server.expect(requestTo(BASE_URL + "/api/embed"))
				.andExpect(method(org.springframework.http.HttpMethod.POST))
				.andExpect(jsonPath("$.model").value("all-minilm"))
				.andExpect(jsonPath("$.input").value("blood pressure"))
				.andRespond(withSuccess(embeddingJson(384), MediaType.APPLICATION_JSON));

		float[] vector = embedder.embed("blood pressure");

		assertThat(vector).hasSize(384);
		assertThat(vector[0]).isEqualTo(0.0f);
		assertThat(vector[1]).isEqualTo(0.5f);
		server.verify();
	}

	@Test
	void wrongLength_failsFastWithIllegalState() {
		server.expect(requestTo(BASE_URL + "/api/embed"))
				.andRespond(withSuccess(embeddingJson(128), MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> embedder.embed("anything"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("128");
		server.verify();
	}

	@Test
	void serverError_surfacesAsRuntimeException() {
		server.expect(requestTo(BASE_URL + "/api/embed"))
				.andRespond(withServerError());

		assertThatThrownBy(() -> embedder.embed("anything"))
				.isInstanceOf(RuntimeException.class);
		server.verify();
	}

	/** {@code {"embeddings":[[0.0,0.5,1.0, ...n values...]]}} — each value is index/2. */
	private static String embeddingJson(int n) {
		String values = IntStream.range(0, n)
				.mapToObj(i -> String.valueOf(i / 2.0f))
				.collect(Collectors.joining(","));
		return "{\"embeddings\":[[" + values + "]]}";
	}
}
