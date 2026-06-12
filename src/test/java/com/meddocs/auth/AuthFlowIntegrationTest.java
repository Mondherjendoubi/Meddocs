package com.meddocs.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meddocs.auth.dto.AuthResponse;
import com.meddocs.auth.dto.LoginRequest;
import com.meddocs.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end auth flow against the running Postgres (Stage 2 checkpoint). Each test
 * runs in a transaction that rolls back, so registered users don't persist.
 *
 * Proves: register → login → token unlocks the protected /api/ping, and that ping is
 * 401 without (or with a bad) token, plus the register/login error paths.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthFlowIntegrationTest {

	private static final String EMAIL = "alice@example.com";
	private static final String PASSWORD = "supersecret123";

	@Autowired
	private MockMvc mockMvc;

	// Constructed directly: Spring Boot 4 doesn't expose a standalone ObjectMapper bean
	// in this context, and Jackson serializes/deserializes our DTO records out of the box.
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void registerLoginAndAccessProtectedPing() throws Exception {
		register(EMAIL, PASSWORD)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andExpect(jsonPath("$.id").isNumber());

		String token = login(EMAIL, PASSWORD);

		// Protected endpoint returns the caller's email when given a valid token.
		mockMvc.perform(get("/api/ping").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(EMAIL));
	}

	@Test
	void pingWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/ping"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void pingWithInvalidTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/ping").header("Authorization", "Bearer not.a.real.token"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void duplicateRegistrationIsConflict() throws Exception {
		register(EMAIL, PASSWORD).andExpect(status().isCreated());
		register(EMAIL, PASSWORD).andExpect(status().isConflict());
	}

	@Test
	void loginWithWrongPasswordIsUnauthorized() throws Exception {
		register(EMAIL, PASSWORD).andExpect(status().isCreated());
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, "wrong-password"))))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void registerWithInvalidEmailIsBadRequest() throws Exception {
		register("not-an-email", PASSWORD).andExpect(status().isBadRequest());
	}

	private org.springframework.test.web.servlet.ResultActions register(String email, String password)
			throws Exception {
		return mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new RegisterRequest(email, password))));
	}

	private String login(String email, String password) throws Exception {
		String body = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readValue(body, AuthResponse.class).token();
	}
}
