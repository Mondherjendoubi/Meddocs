package com.meddocs.ingest;

import com.meddocs.ollama.OllamaProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the {@code meddocs.embedder} {@link org.springframework.boot.autoconfigure.condition.ConditionalOnProperty}
 * wiring with an {@link ApplicationContextRunner} slice — catches a {@code havingValue} typo that
 * would silently leave the fake active (or break the default). Registers the real component
 * classes so their class-level conditions are actually evaluated. No DB.
 */
class EmbedderProviderSelectionTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(OllamaTestBeans.class, FakeEmbedder.class, OllamaEmbedder.class);

	@Test
	void defaultIsFakeEmbedder() {
		runner.run(context -> assertThat(context.getBean(Embedder.class)).isInstanceOf(FakeEmbedder.class));
	}

	@Test
	void fakeExplicitlyIsFakeEmbedder() {
		runner.withPropertyValues("meddocs.embedder=fake")
				.run(context -> assertThat(context.getBean(Embedder.class)).isInstanceOf(FakeEmbedder.class));
	}

	@Test
	void ollamaSwapsInOllamaEmbedder() {
		runner.withPropertyValues("meddocs.embedder=ollama")
				.run(context -> assertThat(context.getBean(Embedder.class)).isInstanceOf(OllamaEmbedder.class));
	}

	@Configuration
	static class OllamaTestBeans {
		@Bean
		RestClient ollamaRestClient() {
			return RestClient.builder().baseUrl("http://localhost:11434").build();
		}

		@Bean
		OllamaProperties ollamaProperties() {
			return new OllamaProperties("http://localhost:11434", "llama3.1:latest", "all-minilm",
					Duration.ofSeconds(5), Duration.ofSeconds(60));
		}
	}
}
