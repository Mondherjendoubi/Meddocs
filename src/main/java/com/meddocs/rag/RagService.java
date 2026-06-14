package com.meddocs.rag;

import com.meddocs.model.User;
import com.meddocs.rag.dto.QueryResponse;

/**
 * Answers a user's question from their own documents: embed → retrieve → (refuse or generate).
 * The seam keeps the controller free of retrieval/generation detail.
 */
public interface RagService {

	QueryResponse answer(User user, String question);
}
