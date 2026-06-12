package com.meddocs.ingest;

import com.meddocs.ingest.dto.DocumentResponse;
import com.meddocs.model.User;
import com.meddocs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Document upload + listing, scoped to the authenticated user. Upload returns 202 Accepted:
 * the document is saved PENDING and indexed asynchronously, so clients poll GET to watch
 * status move PENDING → INDEXED | FAILED.
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

	private final DocumentService documentService;
	private final UserRepository userRepository;
	private final IngestProperties properties;

	@PostMapping
	@ResponseStatus(HttpStatus.ACCEPTED)
	public DocumentResponse upload(Authentication authentication,
			@RequestParam("file") MultipartFile file,
			@RequestParam(value = "sourceLabel", required = false) String sourceLabel) throws IOException {
		validate(file);
		return documentService.upload(currentUser(authentication), file, sourceLabel);
	}

	@GetMapping
	public List<DocumentResponse> list(Authentication authentication) {
		return documentService.list(currentUser(authentication));
	}

	private void validate(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new InvalidUploadException("A non-empty 'file' part is required");
		}
		if (file.getSize() > properties.maxFileSizeBytes()) {
			throw new InvalidUploadException("File exceeds the " + properties.maxFileSizeBytes() + "-byte limit");
		}
		if (!isAccepted(file)) {
			throw new InvalidUploadException("Unsupported file type: " + file.getContentType());
		}
	}

	/** Accept by declared MIME type, falling back to a known extension (MIME for .md is unreliable). */
	private boolean isAccepted(MultipartFile file) {
		String contentType = file.getContentType();
		if (contentType != null && properties.allowedContentTypes().contains(contentType)) {
			return true;
		}
		String name = file.getOriginalFilename();
		if (name == null) {
			return false;
		}
		String lower = name.toLowerCase();
		return lower.endsWith(".pdf") || lower.endsWith(".txt") || lower.endsWith(".md");
	}

	private User currentUser(Authentication authentication) {
		return userRepository.findByEmail(authentication.getName())
				.orElseThrow(() -> new IllegalStateException(
						"Authenticated user not found: " + authentication.getName()));
	}
}
