package com.meddocs;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Provides a throwaway PostgreSQL+pgvector container for integration tests.
 *
 * <p>{@code @ServiceConnection} tells Spring Boot to wire {@code spring.datasource.*} straight
 * from this container — a random free port, a fresh empty DB — so tests never touch the dev
 * database (no port clashes, no leftover data) and run the same on any machine or in CI.
 *
 * <p>Defined as a {@code @Bean} (not a static {@code @Container}) so Spring owns its lifecycle and
 * caches it across every test that imports this config — one container for the whole run.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer<?> pgvectorContainer() {
		// The pgvector image isn't named "postgres", so mark it a compatible substitute.
		return new PostgreSQLContainer<>(
				DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));
	}
}
