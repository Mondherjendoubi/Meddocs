package com.meddocs.ingest;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Wires the ingestion pipeline: binds {@link IngestProperties}, builds the {@link Chunker}
 * from config, and enables {@code @Async} so uploads return immediately while indexing runs
 * on a background thread.
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(IngestProperties.class)
public class IngestConfig {

	@Bean
	public Chunker chunker(IngestProperties properties) {
		return new Chunker(properties.chunkSize(), properties.chunkOverlap());
	}
}
