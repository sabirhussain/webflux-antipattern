package com.hcltech.sample.reactive.webflux.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenAPIConfig {

    @Bean
    OpenAPI openAPI() {
        Components components = new Components().addParameters("TenantHeader",
                new Parameter()
                        .in("header")
                        .name("X-Tenant-ID")
                        .required(true)
                        .description("Tenant ID for multi-tenancy")
                        .example("1"));
        return new OpenAPI().components(components);
    }
}
