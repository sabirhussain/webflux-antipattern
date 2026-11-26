package com.hcltech.sample.reactive.webflux.filter.policy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.util.Set;
import java.util.function.Predicate;

@Component
@Slf4j
public class TenantIdentifierExclusionPolicy {
    //FIXME: externalize this to application.yml or properties file, and make it configurable.
    private static final Set<String> EXCLUDED_PATHS = Set.of(
            "/swagger-ui.html",
            "/swagger-ui",
            "/v3/api-docs",
            "/actuator",
            "/tenants"
    );

    private final Predicate<String> isPathExcluded = path -> EXCLUDED_PATHS.stream().anyMatch(path::startsWith);

    public boolean isPathExcluded(ServerWebExchange exchange) {
        if (exchange == null) {
            throw new IllegalArgumentException("ServerWebExchange cannot be null");
        }

        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        log.debug("Checking if path {} is excluded from tenant identification", path);
        return isPathExcluded.test(path);
    }
}
