package com.meddocs.ingest;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentParserTest {

	private final DocumentParser parser = new DocumentParser();

	@Test
	void extractsPlainText() {
		String text = "Patient presents with a mild fever.\nNo other symptoms reported.";
		String parsed = parser.parse(stream(text), "text/plain");
		assertThat(parsed).contains("mild fever").contains("No other symptoms reported");
	}

	@Test
	void extractsMarkdownAsText() {
		String md = "# Discharge Summary\n\n- Rest\n- Fluids\n";
		String parsed = parser.parse(stream(md), "text/markdown");
		assertThat(parsed).contains("Discharge Summary").contains("Fluids");
	}

	private ByteArrayInputStream stream(String s) {
		return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
	}
}
