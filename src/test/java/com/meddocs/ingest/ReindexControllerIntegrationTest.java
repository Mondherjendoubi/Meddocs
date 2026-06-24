package com.meddocs.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meddocs.TestcontainersConfiguration;
import com.meddocs.auth.dto.AuthResponse;
import com.meddocs.auth.dto.LoginRequest;
import com.meddocs.auth.dto.RegisterRequest;
import com.meddocs.model.Chunk;
import com.meddocs.model.Document;
import com.meddocs.model.User;
import com.meddocs.repository.ChunkRepository;
import com.meddocs.repository.DocumentRepository;
import com.meddocs.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-boundary test for {@link ReindexController} against the real pgvector DB. Unlike
 * {@link ReindexIntegrationTest} (which calls the service with an explicit userId), this exercises
 * the security-critical {@code Authentication → findByEmail → getId()} mapping: a JWT is minted via
 * register/login (mirroring {@code AuthFlowIntegrationTest}) and the endpoint must reindex only the
 * caller's chunks.
 *
 * <p>NOT @Transactional — we commit seeded rows, POST /api/reindex, then re-read to prove the new
 * vectors were flushed; @AfterEach deletes the created users (FK cascade removes their docs+chunks).
 * The embedder stays the deterministic {@link FakeEmbedder}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ReindexControllerIntegrationTest {

	private static final String PASSWORD = "supersecret123";

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private Embedder embedder;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private DocumentRepository documentRepository;
	@Autowired
	private ChunkRepository chunkRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final List<Long> createdUserIds = new ArrayList<>();

	@AfterEach
	void cleanUp() {
		createdUserIds.forEach(userRepository::deleteById); // FK cascade removes documents + chunks
	}

	@Test
	void authenticatedReindexRewritesOnlyCallersChunks() throws Exception {
		// Caller, registered + logged in over HTTP so the JWT carries their email.
		String callerEmail = uniqueEmail();
		register(callerEmail);
		String token = login(callerEmail);
		User caller = userRepository.findByEmail(callerEmail).orElseThrow();
		createdUserIds.add(caller.getId());

		Document callerDoc = documentRepository.save(new Document(caller, "notes.txt", "notes.txt"));
		String content = "blood pressure reading 120 over 80";
		Chunk callerChunk = chunkRepository.save(new Chunk(callerDoc, 0, content, null, sentinel()));

		// Another user whose chunk must stay untouched.
		User other = newUser();
		Document otherDoc = documentRepository.save(new Document(other, "b.txt", "b.txt"));
		Chunk otherChunk = chunkRepository.save(new Chunk(otherDoc, 0, "other content", null, sentinel()));

		mockMvc.perform(post("/api/reindex").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.chunksReembedded").value(1));

		float[] reindexed = chunkRepository.findById(callerChunk.getId()).orElseThrow().getEmbedding();
		assertThat(reindexed).containsExactly(embedder.embed(content));
		assertThat(reindexed).isNotEqualTo(sentinel());

		float[] untouched = chunkRepository.findById(otherChunk.getId()).orElseThrow().getEmbedding();
		assertThat(untouched).containsExactly(sentinel());
	}

	@Test
	void reindexWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/reindex"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void reindexWithNoChunksReturnsZero() throws Exception {
		String email = uniqueEmail();
		register(email);
		String token = login(email);
		// Track for cleanup even though this user owns no documents.
		createdUserIds.add(userRepository.findByEmail(email).orElseThrow().getId());

		mockMvc.perform(post("/api/reindex").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.chunksReembedded").value(0));
	}

	private User newUser() {
		User user = userRepository.save(new User("reindex-" + UUID.randomUUID() + "@example.com", "x"));
		createdUserIds.add(user.getId());
		return user;
	}

	private void register(String email) throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new RegisterRequest(email, PASSWORD))))
				.andExpect(status().isCreated());
	}

	private String login(String email) throws Exception {
		String body = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new LoginRequest(email, PASSWORD))))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readValue(body, AuthResponse.class).token();
	}

	private String uniqueEmail() {
		return "reindex-ctrl-" + UUID.randomUUID() + "@example.com";
	}

	/** A fixed unit vector unlike anything the FakeEmbedder produces, so a rewrite is detectable. */
	private static float[] sentinel() {
		float[] v = new float[384];
		v[0] = 1.0f;
		return v;
	}
}
