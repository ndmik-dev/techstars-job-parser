package io.ndmik.tsparser.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI techstarsJobParserOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Techstars Job Parser API")
                        .version("v1")
                        .description("REST API for scraped Techstars job listings, filters, and scrape run history."));
    }
}
