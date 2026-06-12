package com.meddocs.ingest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class FakeEmbedderTest {

	private final FakeEmbedder embedder = new FakeEmbedder();

	@Test
	void producesVectorOfDeclaredDimension() {
		assertThat(embedder.embed("hello")).hasSize(embedder.dimensions());
		assertThat(embedder.dimensions()).isEqualTo(384);
	}

	@Test
	void isDeterministic_sameTextYieldsSameVector() {
		assertThat(embedder.embed("medical records")).containsExactly(embedder.embed("medical records"));
	}

	@Test
	void differentTextYieldsDifferentVector() {
		assertThat(embedder.embed("aspirin")).isNotEqualTo(embedder.embed("ibuprofen"));
	}

	@Test
	void vectorIsUnitLength() {
		float[] v = embedder.embed("some clinical note");
		double sumOfSquares = 0.0;
		for (float c : v) {
			sumOfSquares += (double) c * c;
		}
		assertThat(Math.sqrt(sumOfSquares)).isCloseTo(1.0, within(1e-5));
	}

	@Test
	void handlesNullAndEmptyWithoutError() {
		assertThat(embedder.embed(null)).hasSize(384);
		assertThat(embedder.embed("")).hasSize(384);
	}
}
