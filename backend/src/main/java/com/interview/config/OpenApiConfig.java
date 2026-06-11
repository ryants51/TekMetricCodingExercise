package com.interview.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI estimateBuilderOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Tekmetric Estimate Builder API")
                .version("1.0.0")
                .description("CRUD API for creating repair estimates, work orders, and quantity-based part line items.")
                .contact(new Contact().name("Tekmetric Interview Project")));
    }
}
