package com.meddocs.ingest;

/** Thrown when an upload is rejected up front (empty, too large, wrong type). Maps to HTTP 400. */
public class InvalidUploadException extends RuntimeException {

	public InvalidUploadException(String message) {
		super(message);
	}
}
