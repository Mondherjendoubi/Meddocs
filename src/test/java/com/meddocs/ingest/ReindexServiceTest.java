package com.meddocs.ingest;

import com.meddocs.repository.ChunkRepository;
import com.meddocs.repository.ChunkRepository.ChunkContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.transaction.support.TransactionCallback;

/**
 * Pure unit test for {@link ReindexService} — no Spring, no DB. Mocks the repository, embedder
 * and {@link TransactionTemplate}; verifies every chunk is re-embedded from its content, each
 * new vector is persisted, the count is returned, and an embedder failure surfaces as
 * {@link ReindexException}.
 */
class ReindexServiceTest {

	private ChunkRepository chunkRepository;
	private Embedder embedder;
	private TransactionTemplate transactionTemplate;
	private ReindexService service;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		chunkRepository = mock(ChunkRepository.class);
		embedder = mock(Embedder.class);
		transactionTemplate = mock(TransactionTemplate.class);
		// Run the persist callback inline (no real transaction in a pure unit test) and surface
		// its return value, mirroring TransactionTemplate.execute's contract.
		when(transactionTemplate.execute(any())).thenAnswer(
				invocation -> invocation.getArgument(0, TransactionCallback.class).doInTransaction(null));
		service = new ReindexService(chunkRepository, embedder, transactionTemplate);
	}

	@Test
	void reembedsEachChunkAndReturnsCount() {
		ChunkContent a = content(1L, "first chunk");
		ChunkContent b = content(2L, "second chunk");
		when(chunkRepository.findContentByDocumentUserId(anyLong())).thenReturn(List.of(a, b));

		float[] vecA = {1f, 1f, 1f};
		float[] vecB = {2f, 2f, 2f};
		when(embedder.embed("first chunk")).thenReturn(vecA);
		when(embedder.embed("second chunk")).thenReturn(vecB);
		when(chunkRepository.updateEmbedding(anyLong(), anyLong(), any())).thenReturn(1);

		int count = service.reembedUserDocuments(7L);

		assertThat(count).isEqualTo(2);
		verify(chunkRepository).updateEmbedding(eq(1L), eq(7L), eq(vecA));
		verify(chunkRepository).updateEmbedding(eq(2L), eq(7L), eq(vecB));
	}

	@Test
	void countSumsActualAffectedRows_notChunkListSize() {
		// Two chunks read, but one vanished between read and write so its UPDATE affects 0 rows.
		ChunkContent a = content(1L, "still here");
		ChunkContent b = content(2L, "deleted meanwhile");
		when(chunkRepository.findContentByDocumentUserId(anyLong())).thenReturn(List.of(a, b));
		when(embedder.embed(anyString())).thenReturn(new float[] {1f, 1f, 1f});
		when(chunkRepository.updateEmbedding(eq(1L), anyLong(), any())).thenReturn(1);
		when(chunkRepository.updateEmbedding(eq(2L), anyLong(), any())).thenReturn(0);

		assertThat(service.reembedUserDocuments(7L)).isEqualTo(1);
	}

	@Test
	void multiChunkEmbedderFailure_stopsAtFailureAndEarlierWritesStand() {
		// Three chunks; the second one's embed blows up. The first should already be persisted,
		// the loop must stop, and the third must never be embedded or written.
		ChunkContent first = content(1L, "ok one");
		ChunkContent second = content(2L, "boom");
		ChunkContent third = content(3L, "never reached");
		when(chunkRepository.findContentByDocumentUserId(anyLong())).thenReturn(List.of(first, second, third));

		float[] vecFirst = {1f, 1f, 1f};
		when(embedder.embed("ok one")).thenReturn(vecFirst);
		when(embedder.embed("boom")).thenThrow(new IllegalStateException("ollama down"));
		when(chunkRepository.updateEmbedding(anyLong(), anyLong(), any())).thenReturn(1);

		assertThatThrownBy(() -> service.reembedUserDocuments(7L))
				.isInstanceOf(ReindexException.class)
				.hasCauseInstanceOf(IllegalStateException.class);

		// First chunk's write already committed (per-chunk transaction) and stands.
		verify(chunkRepository).updateEmbedding(eq(1L), eq(7L), eq(vecFirst));
		// Loop stopped at the failure: the third chunk was never embedded nor written.
		verify(embedder, never()).embed("never reached");
		verify(chunkRepository, never()).updateEmbedding(eq(3L), anyLong(), any());
	}

	@Test
	void noChunks_returnsZero() {
		when(chunkRepository.findContentByDocumentUserId(anyLong())).thenReturn(List.of());

		assertThat(service.reembedUserDocuments(7L)).isZero();
	}

	@Test
	void embedderFailure_throwsReindexException() {
		when(chunkRepository.findContentByDocumentUserId(anyLong())).thenReturn(List.of(content(1L, "boom")));
		when(embedder.embed("boom")).thenThrow(new IllegalStateException("ollama down"));

		assertThatThrownBy(() -> service.reembedUserDocuments(7L))
				.isInstanceOf(ReindexException.class)
				.hasCauseInstanceOf(IllegalStateException.class);
	}

	private static ChunkContent content(Long id, String text) {
		return new ChunkContent() {
			@Override
			public Long getId() {
				return id;
			}

			@Override
			public String getContent() {
				return text;
			}
		};
	}
}
