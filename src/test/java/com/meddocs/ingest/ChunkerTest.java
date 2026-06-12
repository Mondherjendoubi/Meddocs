package com.meddocs.ingest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies our thin wrapper around LangChain4j's recursive splitter: the null/blank handling and
 * config validation we own, plus the behavioural invariants we rely on (chunks produced, size
 * bounded, content preserved). Exact boundary positions are the library's concern, not ours, so
 * we assert properties rather than precise cut points. Pure logic — no Spring, no DB.
 */
class ChunkerTest {

	@Test
	void nullInput_returnsEmptyList() {
		assertThat(new Chunker(50, 10).chunk(null)).isEmpty();
	}

	@Test
	void blankInput_returnsEmptyList() {
		assertThat(new Chunker(50, 10).chunk("   \n\t  ")).isEmpty();
	}

	@Test
	void shortText_yieldsSingleChunkWithTheText() {
		List<String> chunks = new Chunker(200, 20).chunk("a short clinical note");
		assertThat(chunks).hasSize(1);
		assertThat(chunks.get(0)).contains("short clinical note");
	}

	@Test
	void longText_splitsIntoMultipleChunks() {
		String text = "Sentence number " + "lorem ipsum dolor sit amet. ".repeat(200);
		assertThat(new Chunker(300, 30).chunk(text)).hasSizeGreaterThan(1);
	}

	@Test
	void noChunkExceedsChunkSize() {
		String text = "lorem ipsum dolor sit amet consectetur. ".repeat(300);
		assertThat(new Chunker(300, 30).chunk(text)).allMatch(c -> c.length() <= 300);
	}

	@Test
	void preservesContent_everyWordSurvivesSomewhere() {
		String text = "alpha beta gamma delta epsilon zeta eta theta iota kappa lambda mu";
		List<String> chunks = new Chunker(25, 5).chunk(text);
		for (String word : text.split(" ")) {
			assertThat(chunks).anyMatch(c -> c.contains(word));
		}
	}

	@Test
	void hugeInput_terminatesAndStaysWithinSize() {
		List<String> chunks = new Chunker(800, 100).chunk("word ".repeat(100_000));
		assertThat(chunks).isNotEmpty();
		assertThat(chunks).allMatch(c -> c.length() <= 800);
	}

	@Test
	void overlapNotLessThanChunkSize_isRejected() {
		assertThatThrownBy(() -> new Chunker(10, 10)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new Chunker(10, 11)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void nonPositiveChunkSize_isRejected() {
		assertThatThrownBy(() -> new Chunker(0, 0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new Chunker(-5, 0)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void negativeOverlap_isRejected() {
		assertThatThrownBy(() -> new Chunker(10, -1)).isInstanceOf(IllegalArgumentException.class);
	}
}
