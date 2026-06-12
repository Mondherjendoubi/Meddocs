package com.meddocs.ingest;

/** Thrown when an uploaded file's text cannot be extracted. Marks a document FAILED. */
public class DocumentParseException extends RuntimeException {

	public DocumentParseException(String message) {
		super(message);
	}

	public DocumentParseException(String message, Throwable cause) {
		super(message, cause);
	}
}
