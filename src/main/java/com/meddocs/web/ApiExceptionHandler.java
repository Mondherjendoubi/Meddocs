package com.meddocs.web;

import com.meddocs.auth.EmailAlreadyUsedException;
import com.meddocs.ingest.InvalidUploadException;
import com.meddocs.rag.RagException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * Translates exceptions into RFC 9457 {@link ProblemDetail} responses with the right
 * status, so failures aren't leaked as 500s or stack traces.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	/** Duplicate registration → 409 Conflict. */
	@ExceptionHandler(EmailAlreadyUsedException.class)
	public ProblemDetail handleEmailAlreadyUsed(EmailAlreadyUsedException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	/**
	 * Wrong email or password → 401 Unauthorized. Both unknown-user and bad-password
	 * map to the same generic message so we don't reveal which emails exist.
	 */
	@ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
	public ProblemDetail handleBadCredentials(RuntimeException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password");
	}

	/** Bean-validation failures (@Valid) → 400 with a field-by-field summary. */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
		String detail = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
	}

	/** Rejected upload (empty, too large, or unsupported type) → 400. */
	@ExceptionHandler(InvalidUploadException.class)
	public ProblemDetail handleInvalidUpload(InvalidUploadException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	/** Multipart body over the configured limit → 413 Payload Too Large. */
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ProblemDetail handleMaxUploadSize(MaxUploadSizeExceededException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, "Upload exceeds the maximum allowed size");
	}

	/** A downstream RAG dependency (retrieval or the chat model) failed → 503 Service Unavailable. */
	@ExceptionHandler(RagException.class)
	public ProblemDetail handleRagFailure(RagException ex) {
		// The client only sees a generic 503; log the wrapped cause so retrieval/LLM
		// failures stay observable on the server side.
		log.error("RAG query failed", ex);
		return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
				"The question service is temporarily unavailable");
	}
}
