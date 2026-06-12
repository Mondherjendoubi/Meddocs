package com.meddocs.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads {@code Authorization: Bearer <token>}, verifies it, and—if valid—populates
 * the {@link SecurityContextHolder} so downstream filters/controllers see an
 * authenticated user. Runs once per request, before Spring's
 * {@code UsernamePasswordAuthenticationFilter}.
 *
 * <p>This is a plain class (not a {@code @Component}) so Spring Boot does not also
 * auto-register it as a global servlet filter; {@code SecurityConfig} wires it into
 * the chain explicitly.
 *
 * <p>The token is self-contained: we trust the signed {@code sub} (email) without a
 * DB lookup. A bad/expired token is silently ignored — the request simply stays
 * anonymous and the authorization rules reject it with 401.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;

	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		String token = extractToken(request);
		if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			try {
				String email = jwtService.extractEmail(token);
				// No roles in this demo — an empty authority list still counts as authenticated.
				var authentication = new UsernamePasswordAuthenticationToken(email, null, List.of());
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			} catch (JwtException ex) {
				// Invalid/expired token: leave the context anonymous; the entry point returns 401.
				SecurityContextHolder.clearContext();
			}
		}
		filterChain.doFilter(request, response);
	}

	private String extractToken(HttpServletRequest request) {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith(BEARER_PREFIX)) {
			return header.substring(BEARER_PREFIX.length());
		}
		return null;
	}
}
