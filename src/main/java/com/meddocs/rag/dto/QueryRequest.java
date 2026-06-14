package com.meddocs.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A natural-language question to answer from the caller's own documents. */
public record QueryRequest(
		@NotBlank @Size(max = 4000) String question) {
}
