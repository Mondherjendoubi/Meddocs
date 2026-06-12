package com.meddocs.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Body for {@code POST /api/auth/login}. */
public record LoginRequest(
		@NotBlank String email,
		@NotBlank String password) {
}
