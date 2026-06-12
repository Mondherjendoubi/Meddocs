package com.meddocs.ingest;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Runs ingestion off the request thread. The upload endpoint returns immediately with a
 * {@code PENDING} document; this picks it up on a background thread and drives it to
 * {@code INDEXED} or {@code FAILED}. Errors are caught and recorded rather than thrown —
 * nobody is waiting on this thread, so a failure must persist as document state.
 */
@Service
@RequiredArgsConstructor
public class IngestionService {

	private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

	private final DocumentIndexer indexer;

	@Async
	public void ingest(Long documentId, byte[] content, String contentType) {
		try {
			indexer.index(documentId, content, contentType);
		} catch (Exception ex) {
			log.warn("Ingestion failed for document {}: {}", documentId, ex.getMessage());
			indexer.markFailed(documentId, ex.getMessage());
		}
	}
}
