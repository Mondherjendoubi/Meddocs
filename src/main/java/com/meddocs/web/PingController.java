package com.meddocs.web;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Protected smoke-test endpoint. Requires a valid JWT (enforced by SecurityConfig):
 * returns 401 without one, and the caller's email with one.
 */
@RestController
@RequestMapping("/api")
public class PingController {

	@GetMapping("/ping")
	public Map<String, String> ping(Authentication authentication) {
		// JwtAuthenticationFilter set the email as the authentication name.
		return Map.of("email", authentication.getName());
	}
}
