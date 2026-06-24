package com.meddocs.ingest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response body for Ollama's {@code POST /api/embed}. The API batches, so {@code embeddings}
 * is a list of vectors; for our single-input requests we take {@code embeddings[0]}. Unknown
 * fields (model, timings, ...) are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record OllamaEmbedResponse(List<float[]> embeddings) {
}
