package com.hcltech.sample.reactive.webflux.filter;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.util.Optional;

@Component
public class HeaderResolver {
    public Optional<String> getValue(ServerWebExchange exchange, String headerName) {
        String value = exchange.getRequest().getHeaders().getFirst(headerName);

        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(value);
    }
}
