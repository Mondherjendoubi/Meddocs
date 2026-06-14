package com.meddocs.rag;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * k-nearest-neighbour chunk search over the pgvector {@code chunks.embedding} column,
 * scoped to a single user. Uses a native query because Spring Data / JPQL can't express the
 * pgvector {@code <=>} cosine-distance operator.
 *
 * <p>The {@code d.user_id = :userId} predicate is the non-negotiable per-user isolation
 * clause: a query can only ever match the caller's own documents.
 */
@Repository
@RequiredArgsConstructor
public class VectorSearchRepository {

	private static final String SEARCH_SQL = """
			SELECT c.id, c.document_id, d.source_label, c.section_ref, c.content,
			       1 - (c.embedding <=> CAST(:queryVec AS vector)) AS score
			FROM chunks c
			JOIN documents d ON d.id = c.document_id
			WHERE d.user_id = :userId
			  AND c.embedding IS NOT NULL
			ORDER BY c.embedding <=> CAST(:queryVec AS vector)
			LIMIT :k
			""";

	private final EntityManager entityManager;

	/**
	 * Return the {@code k} chunks nearest to {@code queryVecLiteral} among {@code userId}'s
	 * documents, ordered most-similar first.
	 *
	 * @param queryVecLiteral the query embedding as a pgvector literal (see {@link EmbeddingFormat})
	 */
	public List<VectorSearchResult> search(Long userId, String queryVecLiteral, int k) {
		Query query = entityManager.createNativeQuery(SEARCH_SQL)
				.setParameter("userId", userId)
				.setParameter("queryVec", queryVecLiteral)
				.setParameter("k", k);

		@SuppressWarnings("unchecked")
		List<Object[]> rows = query.getResultList();
		return rows.stream().map(VectorSearchRepository::toResult).toList();
	}

	private static VectorSearchResult toResult(Object[] row) {
		return new VectorSearchResult(
				((Number) row[0]).longValue(),
				((Number) row[1]).longValue(),
				(String) row[2],
				(String) row[3],
				(String) row[4],
				((Number) row[5]).doubleValue());
	}
}
