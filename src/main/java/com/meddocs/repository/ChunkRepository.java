package com.meddocs.repository;

import com.meddocs.model.Chunk;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChunkRepository extends JpaRepository<Chunk, Long> {

	long countByDocumentId(Long documentId);
}
