package com.meddocs.ingest;

import com.meddocs.TestcontainersConfiguration;
import com.meddocs.model.Document;
import com.meddocs.model.DocumentStatus;
import com.meddocs.model.User;
import com.meddocs.repository.ChunkRepository;
import com.meddocs.repository.DocumentRepository;
import com.meddocs.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the transactional indexing core against the real DB, synchronously (no async
 * timing). NOT @Transactional: index()/markFailed() commit in their own transactions exactly
 * as in production, so we clean up created rows by hand.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DocumentIndexerIntegrationTest {

	@Autowired
	private DocumentIndexer indexer;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private DocumentRepository documentRepository;
	@Autowired
	private ChunkRepository chunkRepository;

	private final List<Long> createdDocumentIds = new ArrayList<>();
	private final List<Long> createdUserIds = new ArrayList<>();

	@AfterEach
	void cleanUp() {
		createdDocumentIds.forEach(documentRepository::deleteById); // FK cascade removes chunks
		createdUserIds.forEach(userRepository::deleteById);
	}

	@Test
	void indexesDocumentIntoEmbeddedChunks() {
		Document document = newPendingDocument();
		byte[] content = "Patient stable. ".repeat(500).getBytes(StandardCharsets.UTF_8);

		indexer.index(document.getId(), content, "text/plain");

		Document reloaded = documentRepository.findById(document.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(DocumentStatus.INDEXED);
		assertThat(chunkRepository.countByDocumentId(document.getId())).isGreaterThan(0);
	}

	@Test
	void blankUploadThrows_andMarkFailedRecordsReason() {
		Document document = newPendingDocument();

		assertThatThrownBy(() -> indexer.index(document.getId(), "   \n  ".getBytes(StandardCharsets.UTF_8), "text/plain"))
				.isInstanceOf(DocumentParseException.class);
		// The failed transaction rolled back, so no chunks and status is still PENDING...
		assertThat(chunkRepository.countByDocumentId(document.getId())).isZero();

		// ...until IngestionService records the failure in a fresh transaction.
		indexer.markFailed(document.getId(), "No extractable text found in the upload");

		Document reloaded = documentRepository.findById(document.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(DocumentStatus.FAILED);
		assertThat(reloaded.getFailureReason()).isNotBlank();
	}

	private Document newPendingDocument() {
		User user = userRepository.save(new User("ingest-" + UUID.randomUUID() + "@example.com", "x"));
		createdUserIds.add(user.getId());
		Document document = documentRepository.save(new Document(user, "notes.txt", "notes.txt"));
		createdDocumentIds.add(document.getId());
		return document;
	}
}
