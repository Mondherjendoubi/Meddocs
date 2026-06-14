package com.meddocs.rag;

/**
 * Generates an answer from a system instruction and a user prompt. This interface is the seam
 * that keeps {@link RagService} provider-agnostic: Stage 4 ships a {@link FakeChatModel}, and
 * the real local LLM (Ollama) drops in behind it in Stage 5 without touching the RAG core.
 */
public interface ChatModel {

	String generate(String systemPrompt, String userPrompt);
}
