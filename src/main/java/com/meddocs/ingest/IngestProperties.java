package com.meddocs.ingest;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Tunables for the ingestion pipeline ({@code meddocs.ingest.*}).
 *
 * @param chunkSize           max characters per chunk
 * @param chunkOverlap        characters shared between consecutive chunks
 * @param maxFileSizeBytes    largest upload accepted
 * @param allowedContentTypes MIME types accepted (filename extensions .txt/.md/.pdf also pass)
 */
@ConfigurationProperties("meddocs.ingest")
public record IngestProperties(
		int chunkSize,
		int chunkOverlap,
		long maxFileSizeBytes,
		List<String> allowedContentTypes) {
}
