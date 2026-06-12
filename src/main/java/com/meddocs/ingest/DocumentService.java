package com.meddocs.ingest;

import com.meddocs.ingest.dto.DocumentResponse;
import com.meddocs.model.Document;
import com.meddocs.model.User;
import com.meddocs.repository.ChunkRepository;
import com.meddocs.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/** Upload orchestration and per-user listing. The heavy lifting happens async in {@link IngestionService}. */
@Service
@RequiredArgsConstructor
public class DocumentService {

	private final DocumentRepository documentRepository;
	private final ChunkRepository chunkRepository;
	private final IngestionService ingestionService;

	/**
	 * Persist a PENDING document and kick off async ingestion. Deliberately not
	 * {@code @Transactional}: {@code save()} commits on its own so the background thread can
	 * find the row (triggering async inside an open transaction would race the commit).
	 */
	public DocumentResponse upload(User user, MultipartFile file, String sourceLabel) throws IOException {
		String filename = file.getOriginalFilename();
		String label = (sourceLabel == null || sourceLabel.isBlank())
				? (filename == null ? "untitled" : filename)
				: sourceLabel.strip();

		// Read bytes now: a MultipartFile is request-scoped and gone by the time async runs.
		byte[] content = file.getBytes();
		Document document = documentRepository.save(new Document(user, label, filename));

		ingestionService.ingest(document.getId(), content, file.getContentType());
		return DocumentResponse.from(document, 0);
	}

	@Transactional(readOnly = true)
	public List<DocumentResponse> list(User user) {
		return documentRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
				.map(document -> DocumentResponse.from(document, chunkRepository.countByDocumentId(document.getId())))
				.toList();
	}
}
