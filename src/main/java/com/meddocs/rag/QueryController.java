package com.meddocs.rag;

import com.meddocs.model.User;
import com.meddocs.rag.dto.QueryRequest;
import com.meddocs.rag.dto.QueryResponse;
import com.meddocs.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ask-your-documents endpoint, scoped to the authenticated user. Answers are grounded in the
 * caller's own documents only (per-user isolation enforced in {@link VectorSearchRepository}).
 */
@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
public class QueryController {

	private final RagService ragService;
	private final UserRepository userRepository;

	@PostMapping
	public QueryResponse query(Authentication authentication, @Valid @RequestBody QueryRequest request) {
		return ragService.answer(currentUser(authentication), request.question());
	}

	private User currentUser(Authentication authentication) {
		return userRepository.findByEmail(authentication.getName())
				.orElseThrow(() -> new IllegalStateException(
						"Authenticated user not found: " + authentication.getName()));
	}
}
