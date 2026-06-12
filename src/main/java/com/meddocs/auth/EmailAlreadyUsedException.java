package com.meddocs.auth;

/** Thrown when registration is attempted with an email that already exists. Maps to HTTP 409. */
public class EmailAlreadyUsedException extends RuntimeException {

	public EmailAlreadyUsedException(String email) {
		super("Email already registered: " + email);
	}
}
