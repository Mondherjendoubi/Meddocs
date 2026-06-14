package com.meddocs.rag;

import com.meddocs.ingest.Embedder;
import com.meddocs.model.User;
import com.meddocs.rag.dto.QueryResponse;
import com.meddocs.rag.dto.SourceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link DefaultRagService} — no Spring, no DB. The collaborators are
 * Mockito mocks; the focus is the refusal gate and prompt assembly, the bits that don't need
 * a real vector store to verify.
 */
class RagServiceTest {

	private static final String REFUSAL = "I don't have enough information.";
	private static final double THRESHOLD = 0.75;
	private static final int SNIPPET_MAX = 320;

	private Embedder embedder;
	private VectorSearchRepository searchRepository;
	private ChatModel chatModel;
	private DefaultRagService service;

	private final User user = new User("ask@example.com", "x");

	@BeforeEach
	void setUp() {
		embedder = mock(Embedder.class);
		searchRepository = mock(VectorSearchRepository.class);
		chatModel = mock(ChatModel.class);
		RagProperties properties = new RagProperties(4, THRESHOLD, REFUSAL, SNIPPET_MAX);
		service = new DefaultRagService(embedder, searchRepository, chatModel, properties);

		when(embedder.embed(anyString())).thenReturn(new float[] {0.1f, 0.2f, 0.3f});
	}

	@Test
	void groundedPath_userPromptCarriesRetrievedContentAndSourceLabel() {
		String sentinel = "Patient was prescribed amoxicillin 500mg three times daily.";
		VectorSearchResult hit = new VectorSearchResult(
				11L, 7L, "discharge-summary.pdf", "p. 2", sentinel, 0.90);
		when(searchRepository.search(nullable(Long.class), anyString(), anyInt())).thenReturn(List.of(hit));
		when(chatModel.generate(anyString(), anyString())).thenReturn("Amoxicillin, per the summary.");

		QueryResponse response = service.answer(user, "What antibiotic was prescribed?");

		assertThat(response.grounded()).isTrue();
		assertThat(response.answer()).isEqualTo("Amoxicillin, per the summary.");

		ArgumentCaptor<String> systemCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);
		verify(chatModel).generate(systemCaptor.capture(), userCaptor.capture());

		// (1) the user prompt carries the retrieved content + its source label
		assertThat(userCaptor.getValue())
				.contains(sentinel)
				.contains("discharge-summary.pdf")
				.contains("What antibiotic was prescribed?");
		// (2) the system prompt is the pinned anti-hallucination instruction
		assertThat(systemCaptor.getValue()).isEqualTo(PromptTemplates.SYSTEM_INSTRUCTION);
	}

	@Test
	void belowThreshold_refusesWithoutCallingChatModel() {
		VectorSearchResult weakHit = new VectorSearchResult(
				1L, 1L, "notes.txt", null, "loosely related text", 0.40);
		when(searchRepository.search(nullable(Long.class), anyString(), anyInt())).thenReturn(List.of(weakHit));

		QueryResponse response = service.answer(user, "unrelated question");

		assertThat(response.grounded()).isFalse();
		assertThat(response.answer()).isEqualTo(REFUSAL);
		assertThat(response.sources()).isEmpty();
		verify(chatModel, never()).generate(any(), any());
	}

	@Test
	void emptyHits_refusesWithoutCallingChatModel() {
		when(searchRepository.search(nullable(Long.class), anyString(), anyInt())).thenReturn(List.of());

		QueryResponse response = service.answer(user, "anything at all");

		assertThat(response.grounded()).isFalse();
		assertThat(response.answer()).isEqualTo(REFUSAL);
		assertThat(response.sources()).isEmpty();
		verify(chatModel, never()).generate(any(), any());
	}

	@Test
	void groundedPath_mapsSourcesAndTruncatesSnippet() {
		String longContent = "x".repeat(SNIPPET_MAX + 200);
		VectorSearchResult hit = new VectorSearchResult(
				42L, 9L, "labs.pdf", "§3", longContent, 0.88);
		when(searchRepository.search(nullable(Long.class), anyString(), anyInt())).thenReturn(List.of(hit));
		when(chatModel.generate(anyString(), anyString())).thenReturn("answer");

		QueryResponse response = service.answer(user, "question");

		assertThat(response.sources()).hasSize(1);
		SourceResponse source = response.sources().get(0);
		assertThat(source.documentId()).isEqualTo(9L);
		assertThat(source.sourceLabel()).isEqualTo("labs.pdf");
		assertThat(source.sectionRef()).isEqualTo("§3");
		assertThat(source.score()).isEqualTo(0.88);
		assertThat(source.snippet()).hasSize(SNIPPET_MAX);
	}
}
