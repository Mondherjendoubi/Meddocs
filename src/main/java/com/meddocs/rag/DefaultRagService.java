package com.meddocs.rag;

import com.meddocs.ingest.Embedder;
import com.meddocs.model.User;
import com.meddocs.rag.dto.QueryResponse;
import com.meddocs.rag.dto.SourceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The RAG core. For each question:
 * <ol>
 *   <li>embed the question with the <em>same</em> {@link Embedder} used at ingest;</li>
 *   <li>retrieve the top-k nearest chunks among the user's own documents;</li>
 *   <li><b>refusal gate</b> — if nothing was retrieved, or the best hit is below the
 *       similarity threshold, return the canned refusal and <em>never</em> call the
 *       {@link ChatModel} (don't let the LLM hallucinate past weak grounding);</li>
 *   <li>otherwise build the prompts, generate an answer, and return it with its sources.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class DefaultRagService implements RagService {

	private final Embedder embedder;
	private final VectorSearchRepository vectorSearchRepository;
	private final ChatModel chatModel;
	private final RagProperties properties;

	@Override
	public QueryResponse answer(User user, String question) {
		List<VectorSearchResult> hits;
		try {
			float[] queryVector = embedder.embed(question);
			String queryLiteral = EmbeddingFormat.toPgVectorLiteral(queryVector);
			hits = vectorSearchRepository.search(user.getId(), queryLiteral, properties.topK());
		} catch (DataAccessException ex) {
			// Only genuine data-access/infrastructure failures become a transient 503.
			// A programming bug (NPE, IllegalState, ...) propagates as a 500 instead of
			// being masked as "temporarily unavailable".
			throw new RagException("Retrieval failed for query", ex);
		}

		// Refusal gate: no grounding, or the best hit is too weak — refuse without the LLM.
		if (hits.isEmpty() || hits.get(0).score() < properties.similarityThreshold()) {
			return QueryResponse.refusal(properties.refusalMessage());
		}

		// Build prompts outside the try: this is our own pure code, so a failure here is a
		// bug and should surface as a 500, not a transient 503.
		String systemPrompt = PromptTemplates.SYSTEM_INSTRUCTION;
		String userPrompt = PromptTemplates.userPrompt(hits, question);

		String answer;
		try {
			// ChatModel is our own interface with no checked exceptions. The Stage 5 Ollama
			// impl will throw IO/HTTP RuntimeExceptions here, so we wrap RuntimeException —
			// but only around this single downstream call, not the mapping/prompt logic.
			answer = chatModel.generate(systemPrompt, userPrompt);
		} catch (RuntimeException ex) {
			throw new RagException("Answer generation failed", ex);
		}

		List<SourceResponse> sources = hits.stream().map(this::toSource).toList();
		return QueryResponse.grounded(answer, sources);
	}

	private SourceResponse toSource(VectorSearchResult hit) {
		return new SourceResponse(
				hit.documentId(),
				hit.sourceLabel(),
				hit.sectionRef(),
				truncate(hit.content(), properties.snippetMaxChars()),
				hit.score());
	}

	private static String truncate(String content, int maxChars) {
		if (content == null || content.length() <= maxChars) {
			return content;
		}
		return content.substring(0, maxChars);
	}
}
