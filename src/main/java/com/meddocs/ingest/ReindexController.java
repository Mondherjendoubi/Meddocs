package com.meddocs.ingest;

import com.meddocs.ingest.dto.ReindexResponse;
import com.meddocs.model.User;
import com.meddocs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint to re-embed the authenticated user's chunks with the active embedder. Scoped to the
 * caller's own documents (same per-user isolation as the rest of the app) — it's a per-user
 * operation, not an admin one, so it lives at {@code /api/reindex}.
 */
@RestController
@RequestMapping("/api/reindex")
@RequiredArgsConstructor
public class ReindexController {

	private final ReindexService reindexService;
	private final UserRepository userRepository;

	@PostMapping
	public ReindexResponse reindex(Authentication authentication) {
		int chunksReembedded = reindexService.reembedUserDocuments(currentUser(authentication).getId());
		return new ReindexResponse(chunksReembedded);
	}

	private User currentUser(Authentication authentication) {
		return userRepository.findByEmail(authentication.getName())
				.orElseThrow(() -> new IllegalStateException(
						"Authenticated user not found: " + authentication.getName()));
	}
}
