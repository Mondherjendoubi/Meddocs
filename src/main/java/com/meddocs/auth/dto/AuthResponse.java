package com.meddocs.auth.dto;

/** Returned on successful login: the signed JWT to send as {@code Authorization: Bearer <token>}. */
public record AuthResponse(String token, String tokenType) {

	public static AuthResponse bearer(String token) {
		return new AuthResponse(token, "Bearer");
	}
}
