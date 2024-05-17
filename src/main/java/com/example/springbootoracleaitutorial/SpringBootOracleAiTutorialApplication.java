package com.example.springbootoracleaitutorial;

import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class SpringBootOracleAiTutorialApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBootOracleAiTutorialApplication.class, args);
	}

	@Bean
	public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingClient embeddingClient) {
		return new OracleVectorStore(jdbcTemplate, embeddingClient);
	}

}
