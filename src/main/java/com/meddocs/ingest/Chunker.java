package com.meddocs.ingest;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;

import java.util.List;

/**
 * Splits document text into overlapping chunks using LangChain4j's recursive splitter
 * ({@link DocumentSplitters#recursive}). "Recursive" = it cuts at the largest natural boundary
 * available (paragraph → line → sentence → word) and only descends to a hard character split when
 * a span has none. Honoring those boundaries keeps each chunk a coherent unit, which makes its
 * embedding sharper; the overlap repeats the seam so a fact straddling a cut survives whole.
 *
 * <p>We wrap the library rather than expose it directly so the rest of the pipeline depends on our
 * own type and gets consistent null/blank handling. <b>Unit = characters</b> (~4 chars ≈ 1 token);
 * the production defaults approximate ~800 tokens with ~100 of overlap. When Spring AI lands
 * (Stage 5) this could swap to a token-accurate splitter without touching anything downstream.
 */
public class Chunker {

	/** ~800 tokens at ~4 chars/token. */
	public static final int DEFAULT_CHUNK_SIZE = 3200;
	/** ~100 tokens at ~4 chars/token. */
	public static final int DEFAULT_OVERLAP = 400;

	private final DocumentSplitter splitter;

	public Chunker() {
		this(DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
	}

	/**
	 * @param chunkSize max characters per chunk (must be > 0)
	 * @param overlap   characters shared between consecutive chunks (0 <= overlap < chunkSize)
	 */
	public Chunker(int chunkSize, int overlap) {
		if (chunkSize <= 0) {
			throw new IllegalArgumentException("chunkSize must be positive, was " + chunkSize);
		}
		if (overlap < 0) {
			throw new IllegalArgumentException("overlap must be non-negative, was " + overlap);
		}
		if (overlap >= chunkSize) {
			throw new IllegalArgumentException(
					"overlap (" + overlap + ") must be smaller than chunkSize (" + chunkSize + ")");
		}
		this.splitter = DocumentSplitters.recursive(chunkSize, overlap);
	}

	/**
	 * Split {@code text} into overlapping chunks. Outer whitespace is stripped first; blank or
	 * null input yields an empty list (nothing to index).
	 */
	public List<String> chunk(String text) {
		if (text == null) {
			return List.of();
		}
		String normalized = text.strip();
		if (normalized.isEmpty()) {
			return List.of();
		}
		return splitter.split(Document.from(normalized)).stream()
				.map(TextSegment::text)
				.toList();
	}
}
