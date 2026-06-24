package com.meddocs.ingest;

import com.meddocs.repository.ChunkRepository;
import com.meddocs.repository.ChunkRepository.ChunkContent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Recomputes the embedding vectors for a user's existing chunks using the currently-active
 * {@link Embedder}. This is the bridge for swapping embedders (e.g. fake → Ollama, Stage 5):
 * the stored {@code content} stays, but its vector is regenerated so old rows become comparable
 * to queries embedded by the new model.
 *
 * <p><b>Connection hygiene:</b> the embedder may make blocking HTTP calls (Ollama, ~60s read
 * timeout each). We therefore <em>don't</em> hold a DB connection across the embedding loop:
 * content is read up front, embedded outside any transaction, and each new vector is persisted
 * in its own short transaction. The trade-off is that this is no longer strictly all-or-nothing
 * — a mid-run embedder failure leaves already-persisted vectors committed — but each row is
 * internally consistent (content + its freshly computed vector), so the store is never corrupt,
 * and re-running reindex is idempotent.
 */
@Service
@RequiredArgsConstructor
public class ReindexService {

	private static final Logger log = LoggerFactory.getLogger(ReindexService.class);

	private final ChunkRepository chunkRepository;
	private final Embedder embedder;
	private final TransactionTemplate transactionTemplate;

	/**
	 * Re-embeds every chunk owned by {@code userId} from its stored content.
	 *
	 * @return the number of chunks re-embedded
	 * @throws ReindexException if the embedder fails (mapped to HTTP 503)
	 */
	public int reembedUserDocuments(Long userId) {
		List<ChunkContent> chunks = chunkRepository.findContentByDocumentUserId(userId);
		int reembedded = 0;
		for (ChunkContent chunk : chunks) {
			float[] embedding;
			try {
				// Outside any transaction: a blocking Ollama call must not pin a DB connection.
				embedding = embedder.embed(chunk.getContent());
			} catch (RuntimeException ex) {
				log.error("Re-embedding failed for chunk {} (user {})", chunk.getId(), userId, ex);
				throw new ReindexException("Re-embedding failed for user " + userId, ex);
			}
			// Short transaction per chunk: connection held only for the write, not the embed.
			// Count rows actually affected — a chunk deleted between read and write writes nothing.
			reembedded += transactionTemplate.execute(
					status -> chunkRepository.updateEmbedding(chunk.getId(), userId, embedding));
		}
		return reembedded;
	}
}
