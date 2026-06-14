package com.meddocs.rag;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the RAG core: binds {@link RagProperties} so the retrieval/refusal/generation
 * tunables are configurable via {@code meddocs.rag.*}.
 */
@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class RagConfig {
}
