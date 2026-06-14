package com.meddocs.rag;

import com.meddocs.TestcontainersConfiguration;
import com.meddocs.ingest.Embedder;
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
 * Exercises {@link VectorSearchRepository} against the real pgvector DB. NOT @Transactional —
 * we commit seeded rows so the native search sees them, then clean up by hand (FK cascade
 * removes documents + chunks when the user is deleted).
 *
 * <p>The headline assertion is the ISOLATION proof: a search as user A must never surface any
 * of user B's documents, however similar their embeddings.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class VectorSearchRepositoryIntegrationTest {

	@Autowired
	private VectorSearchRepository vectorSearchRepository;
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
	void returnsNearestChunksWithDescendingScores() {
		User user = newUser();
		Document document = newDocument(user, "clinical-notes.txt");
		// The query text is embedded identically to one chunk's content, so that chunk is the
		// single nearest neighbour (similarity ~1.0) and ranks above the unrelated chunks.
		String query = "blood pressure reading 120 over 80";
		seedChunk(document, 0, query, null);
		seedChunk(document, 1, "completely unrelated text about quarterly budgets", null);
		seedChunk(document, 2, "another off-topic paragraph entirely", null);

		String queryLiteral = EmbeddingFormat.toPgVectorLiteral(embedder.embed(query));
		List<VectorSearchResult> results = vectorSearchRepository.search(user.getId(), queryLiteral, 4);

		assertThat(results).isNotEmpty();
		assertThat(results.get(0).content()).isEqualTo(query);
		// Scores are non-increasing (most similar first).
		for (int i = 1; i < results.size(); i++) {
			assertThat(results.get(i).score()).isLessThanOrEqualTo(results.get(i - 1).score());
		}
		assertThat(results).allSatisfy(r -> assertThat(r.documentId()).isEqualTo(document.getId()));
	}

	@Test
	void isolation_searchAsUserANeverReturnsUserBDocuments() {
		User userA = newUser();
		Document docA = newDocument(userA, "a-notes.txt");
		seedChunk(docA, 0, "shared topic about cardiology", null);

		User userB = newUser();
		Document docB = newDocument(userB, "b-notes.txt");
		// Identical content, so absent isolation it would be the top match for A's query.
		seedChunk(docB, 0, "shared topic about cardiology", null);

		String queryLiteral = EmbeddingFormat.toPgVectorLiteral(embedder.embed("shared topic about cardiology"));
		List<VectorSearchResult> results = vectorSearchRepository.search(userA.getId(), queryLiteral, 10);

		assertThat(results).isNotEmpty();
		assertThat(results).allSatisfy(r -> assertThat(r.documentId()).isEqualTo(docA.getId()));
		assertThat(results).noneSatisfy(r -> assertThat(r.documentId()).isEqualTo(docB.getId()));
	}

	@Test
	void emptyCorpus_returnsEmptyList() {
		User user = newUser(); // a user with no documents

		String queryLiteral = EmbeddingFormat.toPgVectorLiteral(embedder.embed("anything"));
		List<VectorSearchResult> results = vectorSearchRepository.search(user.getId(), queryLiteral, 4);

		assertThat(results).isEmpty();
	}

	private User newUser() {
		User user = userRepository.save(new User("rag-" + UUID.randomUUID() + "@example.com", "x"));
		createdUserIds.add(user.getId());
		return user;
	}

	private Document newDocument(User user, String label) {
		return documentRepository.save(new Document(user, label, label));
	}

	private void seedChunk(Document document, int index, String content, String sectionRef) {
		chunkRepository.save(new Chunk(document, index, content, sectionRef, embedder.embed(content)));
	}
}
