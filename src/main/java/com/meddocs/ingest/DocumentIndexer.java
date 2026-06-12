package com.meddocs.ingest;

import com.meddocs.model.Chunk;
import com.meddocs.model.Document;
import com.meddocs.model.DocumentStatus;
import com.meddocs.repository.ChunkRepository;
import com.meddocs.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The transactional core of ingestion: parse → chunk → embed → store, all-or-nothing.
 *
 * <p>{@link #index} runs in a single transaction, so if any chunk fails to embed or store the
 * whole thing rolls back — a document is never left half-indexed. On success the document flips
 * to {@code INDEXED}; on failure {@link IngestionService} calls {@link #markFailed} in a
 * <em>separate</em> transaction (the failed one rolled back, so the status write needs its own).
 *
 * <p>These methods live in their own bean (not {@code IngestionService}) so the {@code @Async}
 * caller invokes them through the Spring proxy and the {@code @Transactional} boundary applies.
 */
@Component
@RequiredArgsConstructor
public class DocumentIndexer {

	private final DocumentRepository documentRepository;
	private final ChunkRepository chunkRepository;
	private final DocumentParser parser;
	private final Chunker chunker;
	private final Embedder embedder;

	@Transactional
	public void index(Long documentId, byte[] content, String contentType) {
		Document document = documentRepository.findById(documentId)
				.orElseThrow(() -> new IllegalStateException("Document " + documentId + " no longer exists"));

		String text = parser.parse(new ByteArrayInputStream(content), contentType);
		List<String> pieces = chunker.chunk(text);
		if (pieces.isEmpty()) {
			throw new DocumentParseException("No extractable text found in the upload");
		}

		List<Chunk> chunks = new ArrayList<>(pieces.size());
		for (int i = 0; i < pieces.size(); i++) {
			String piece = pieces.get(i);
			float[] embedding = embedder.embed(piece);
			chunks.add(new Chunk(document, i, piece, null, embedding));
		}
		chunkRepository.saveAll(chunks);

		document.setStatus(DocumentStatus.INDEXED); // managed entity — flushed on commit
	}

	@Transactional
	public void markFailed(Long documentId, String reason) {
		documentRepository.findById(documentId).ifPresent(document -> {
			document.setStatus(DocumentStatus.FAILED);
			document.setFailureReason(reason);
		});
	}
}
