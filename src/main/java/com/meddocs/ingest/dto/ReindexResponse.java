package com.meddocs.ingest.dto;

/** Result of a re-embedding run: how many chunks had their embedding vectors recomputed. */
public record ReindexResponse(int chunksReembedded) {
}
