package com.meddocs.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meddocs.TestcontainersConfiguration;
import com.meddocs.auth.dto.AuthResponse;
import com.meddocs.auth.dto.LoginRequest;
import com.meddocs.auth.dto.RegisterRequest;
import com.meddocs.ingest.Embedder;
import com.meddocs.rag.dto.QueryRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-boundary test for {@link QueryController}: when a downstream RAG dependency fails, the
 * request must surface as a 503 {@link org.springframework.http.ProblemDetail}, not a raw 500.
 * The {@link Embedder} is replaced by a throwing mock so the query path's embed call blows up;
 * {@link DefaultRagService} wraps it in a {@link RagException}, mapped to 503 by the advice.
 *
 * <p>Authenticated via the real register/login JWT flow (mirroring AuthFlowIntegrationTest) so
 * the {@code Authentication → user} mapping is exercised end to end. @Transactional rolls back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class QueryControllerIntegrationTest {

	private static final String EMAIL = "query-boundary@example.com";
	private static final String PASSWORD = "supersecret123";

	@Autowired
	private MockMvc mockMvc;

	// Replace the deterministic FakeEmbedder with a throwing one, simulating an Ollama/IO failure
	// on the query embed call so the 503 error-mapping branch is exercised over real HTTP.
	@MockitoBean
	private Embedder embedder;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void embedderFailureDuringQueryMapsToServiceUnavailable() throws Exception {
		when(embedder.embed(anyString())).thenThrow(new IllegalStateException("ollama embed down"));

		register();
		String token = login();

		mockMvc.perform(post("/api/query")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new QueryRequest("anything"))))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.status").value(503))
				.andExpect(jsonPath("$.detail").value("The question service is temporarily unavailable"));
	}

	private void register() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new RegisterRequest(EMAIL, PASSWORD))))
				.andExpect(status().isCreated());
	}

	private String login() throws Exception {
		String body = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD))))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readValue(body, AuthResponse.class).token();
	}
}
