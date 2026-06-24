package com.meddocs.rag;

import com.meddocs.ollama.OllamaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit tests for {@link OllamaChatModel} using {@link MockRestServiceServer} — no real Ollama.
 * Verifies the request body (model, system + user messages, {@code stream:false}), that a 200
 * parses {@code message.content}, and that a 5xx surfaces as a {@link RuntimeException}.
 */
class OllamaChatModelTest {

	private static final String BASE_URL = "http://localhost:11434";

	private MockRestServiceServer server;
	private OllamaChatModel chatModel;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		server = MockRestServiceServer.bindTo(builder).build();
		OllamaProperties properties = new OllamaProperties(
				BASE_URL, "llama3.1:latest", "all-minilm", Duration.ofSeconds(5), Duration.ofSeconds(60));
		chatModel = new OllamaChatModel(builder.build(), properties);
	}

	@Test
	void postsChatBodyAndReturnsMessageContent() {
		server.expect(requestTo(BASE_URL + "/api/chat"))
				.andExpect(method(org.springframework.http.HttpMethod.POST))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.model").value("llama3.1:latest"))
				.andExpect(jsonPath("$.stream").value(false))
				.andExpect(jsonPath("$.messages[0].role").value("system"))
				.andExpect(jsonPath("$.messages[0].content").value("you are a doctor"))
				.andExpect(jsonPath("$.messages[1].role").value("user"))
				.andExpect(jsonPath("$.messages[1].content").value("what is up"))
				.andRespond(withSuccess("{\"message\":{\"content\":\"hi\"}}", MediaType.APPLICATION_JSON));

		String answer = chatModel.generate("you are a doctor", "what is up");

		assertThat(answer).isEqualTo("hi");
		server.verify();
	}

	@Test
	void serverError_surfacesAsRuntimeException() {
		server.expect(requestTo(BASE_URL + "/api/chat"))
				.andRespond(withServerError());

		assertThatThrownBy(() -> chatModel.generate("sys", "usr"))
				.isInstanceOf(RuntimeException.class);
		server.verify();
	}

	@Test
	void emptyContent_throwsRatherThanReturningBlankAnswer() {
		// A well-formed 200 with empty content (model not loaded, aborted generation) must not
		// pass through as a grounded answer with an empty body.
		server.expect(requestTo(BASE_URL + "/api/chat"))
				.andRespond(withSuccess("{\"message\":{\"content\":\"\"}}", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> chatModel.generate("sys", "usr"))
				.isInstanceOf(IllegalStateException.class);
		server.verify();
	}

	@Test
	void nullContent_throwsRatherThanReturningNullAnswer() {
		server.expect(requestTo(BASE_URL + "/api/chat"))
				.andRespond(withSuccess("{\"message\":{\"content\":null}}", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> chatModel.generate("sys", "usr"))
				.isInstanceOf(IllegalStateException.class);
		server.verify();
	}
}
