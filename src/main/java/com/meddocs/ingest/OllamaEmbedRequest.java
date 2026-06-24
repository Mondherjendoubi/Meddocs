package com.meddocs.ingest;

/**
 * Request body for Ollama's {@code POST /api/embed}: a model tag and a single piece of text.
 */
record OllamaEmbedRequest(String model, String input) {
}
