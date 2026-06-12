package com.meddocs.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Issues and verifies signed JWTs. The token is the whole session: it carries the
 * user's email as the {@code sub} claim and is signed (HMAC-SHA), so a tampered or
 * expired token fails verification and we never trust its contents.
 *
 * <p>Note a JWT is <em>signed, not encrypted</em> — anyone can base64-decode the
 * payload, so we put only the email (non-secret) in it, never a password.
 */
@Service
public class JwtService {

	private final SecretKey key;
	private final long expirationMs;

	public JwtService(JwtProperties properties) {
		// hmacShaKeyFor enforces the >= 256-bit (32-byte) minimum for HS256.
		this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
		this.expirationMs = properties.expirationMs();
	}

	/** Mint a token whose subject is the user's email. */
	public String generateToken(String email) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(email)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusMillis(expirationMs)))
				.signWith(key)
				.compact();
	}

	/**
	 * Verify the signature + expiry and return the subject (email).
	 *
	 * @throws JwtException if the token is malformed, tampered, or expired.
	 */
	public String extractEmail(String token) {
		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload()
				.getSubject();
	}
}
