package com.meddocs.repository;

import com.meddocs.model.Chunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChunkRepository extends JpaRepository<Chunk, Long> {

	long countByDocumentId(Long documentId);

	/** Id + content only, so re-embedding needn't hold managed entities while embedding is slow. */
	@Query("select c.id as id, c.content as content from Chunk c where c.document.user.id = :userId")
	List<ChunkContent> findContentByDocumentUserId(@Param("userId") Long userId);

	/**
	 * Targeted update of a single chunk's embedding — keeps the persist transaction short. Scoped
	 * by {@code userId} as defence-in-depth (every other ownership-sensitive query carries the user
	 * predicate); returns the rows actually affected (0 if the chunk vanished or isn't the user's).
	 */
	@Modifying
	@Query("update Chunk c set c.embedding = :embedding where c.id = :id and c.document.user.id = :userId")
	int updateEmbedding(@Param("id") Long id, @Param("userId") Long userId, @Param("embedding") float[] embedding);

	/** Projection of just the columns reindex needs to recompute a vector. */
	interface ChunkContent {
		Long getId();
		String getContent();
	}
}
