package dev.prasadgaikwad.langchain4jdemo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Top-level OpenAPI metadata shown in the Swagger UI header.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI langchain4jDemoOpenApi() {
        return new OpenAPI().info(new Info()
                .title("LangChain4j Demo API")
                .description("REST endpoints exposing the LangChain4j demo features: "
                        + "conversation memory, RAG, tool-using agents, prompting techniques, "
                        + "semantic search, and streaming chat.")
                .version("1.0.0"));
    }
}
