package com.meddocs.ingest;

import com.meddocs.TestcontainersConfiguration;
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
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link ReindexService} against the real pgvector DB. NOT @Transactional — we commit
 * seeded rows, reindex, then re-read to prove the new vectors were flushed; cleanup deletes the
 * created users (FK cascade removes their documents + chunks).
 *
 * <p>The embedder stays the deterministic {@link FakeEmbedder} in tests. We seed chunks with a
 * sentinel embedding that differs from the fake's output, then assert reindex overwrote it with
 * exactly what the fake produces for the chunk's content.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ReindexIntegrationTest {

	@Autowired
	private ReindexService reindexService;
	@Autowired
	private Embedder embedder;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private DocumentRepository documentRepository;
	@Autowired
	private ChunkRepository chunkRepository;

	private final List<Long> createdUserIds = new ArrayList<>();

	@AfterEach
	void cleanUp() {
		createdUserIds.forEach(userRepository::deleteById); // FK cascade removes documents + chunks
	}

	@Test
	void rewritesEmbeddingsAndReturnsCount() {
		User user = newUser();
		Document document = documentRepository.save(new Document(user, "notes.txt", "notes.txt"));
		String contentA = "blood pressure reading 120 over 80";
		String contentB = "patient prescribed amoxicillin";
		Chunk chunkA = chunkRepository.save(new Chunk(document, 0, contentA, null, sentinel()));
		Chunk chunkB = chunkRepository.save(new Chunk(document, 1, contentB, null, sentinel()));

		int count = reindexService.reembedUserDocuments(user.getId());

		assertThat(count).isEqualTo(2);

		// Re-read from the DB to confirm the new vectors were flushed on commit.
		float[] reA = chunkRepository.findById(chunkA.getId()).orElseThrow().getEmbedding();
		float[] reB = chunkRepository.findById(chunkB.getId()).orElseThrow().getEmbedding();
		assertThat(reA).containsExactly(embedder.embed(contentA));
		assertThat(reB).containsExactly(embedder.embed(contentB));
		assertThat(reA).isNotEqualTo(sentinel()); // the sentinel was actually overwritten
	}

	@Test
	void otherUsersChunksAreUntouched() {
		User owner = newUser();
		Document ownerDoc = documentRepository.save(new Document(owner, "a.txt", "a.txt"));
		chunkRepository.save(new Chunk(ownerDoc, 0, "owner content", null, sentinel()));

		User other = newUser();
		Document otherDoc = documentRepository.save(new Document(other, "b.txt", "b.txt"));
		Chunk otherChunk = chunkRepository.save(new Chunk(otherDoc, 0, "other content", null, sentinel()));

		int count = reindexService.reembedUserDocuments(owner.getId());

		assertThat(count).isEqualTo(1);
		float[] untouched = chunkRepository.findById(otherChunk.getId()).orElseThrow().getEmbedding();
		assertThat(untouched).containsExactly(sentinel());
	}

	private User newUser() {
		User user = userRepository.save(new User("reindex-" + UUID.randomUUID() + "@example.com", "x"));
		createdUserIds.add(user.getId());
		return user;
	}

	/** A fixed unit vector unlike anything the FakeEmbedder produces, so a rewrite is detectable. */
	private static float[] sentinel() {
		float[] v = new float[384];
		v[0] = 1.0f;
		return v;
	}
}
