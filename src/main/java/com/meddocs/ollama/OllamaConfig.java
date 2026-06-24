package com.meddocs.ollama;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Wires the Ollama HTTP client: binds {@link OllamaProperties} and exposes a single
 * {@link RestClient} pre-configured with the base URL and connect/read timeouts, shared by
 * the chat and embedding providers.
 *
 * <p>The bean is unconditional on purpose: a {@link RestClient} holds no resources until a
 * request is sent, so building it costs nothing even when both providers stay on their fakes.
 */
@Configuration
@EnableConfigurationProperties(OllamaProperties.class)
public class OllamaConfig {

	@Bean
	public RestClient ollamaRestClient(OllamaProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.connectTimeout());
		requestFactory.setReadTimeout(properties.readTimeout());
		return RestClient.builder()
				.baseUrl(properties.baseUrl())
				.requestFactory(requestFactory)
				.build();
	}
}
