package com.meddocs.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body for {@code POST /api/auth/register}. */
public record RegisterRequest(
		@NotBlank @Email String email,
		@NotBlank @Size(min = 8, max = 100, message = "password must be 8-100 characters") String password) {
}
