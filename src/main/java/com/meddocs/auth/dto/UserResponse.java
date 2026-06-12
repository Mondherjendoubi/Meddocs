package com.meddocs.auth.dto;

import com.meddocs.model.User;

/** Public view of a user — never exposes the password hash. */
public record UserResponse(Long id, String email) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getEmail());
	}
}
