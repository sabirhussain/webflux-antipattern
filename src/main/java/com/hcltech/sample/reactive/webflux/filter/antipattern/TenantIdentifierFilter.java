package com.hcltech.sample.reactive.webflux.filter.antipattern;

import com.hcltech.sample.reactive.webflux.annotation.AntiPattern;
import com.hcltech.sample.reactive.webflux.ctx.TenantContext;
import com.hcltech.sample.reactive.webflux.ctx.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.function.Predicate;

@Component
@Slf4j
@AntiPattern
public final class TenantIdentifierFilter implements WebFilter {
    private static final String TENANT_ID_HEADER = "X-Tenant-ID";

    //FIXME: externalize this to application.yml or properties file, and make it configurable.
    private static final Set<String> EXCLUDED_PATHS = Set.of(
            "/swagger-ui.html",
            "/swagger-ui",
            "/v3/api-docs",
            "/tenants"
    );

    private final Predicate<String> isExcludedPath = path -> EXCLUDED_PATHS.stream().anyMatch(path::startsWith);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        log.info("Incoming request path: {}, Tenant from context: {}", path, TenantContextHolder.getContext() != null ? TenantContextHolder.getContext().tenantId() : "N/A");

        if (isExcludedPath.test(path)) {
            return chain.filter(exchange);
        }

        String tenantId = exchange.getRequest().getHeaders().getFirst(TENANT_ID_HEADER);

        if (tenantId == null || tenantId.isBlank()) {
            log.warn("Tenant ID is missing in the request headers.");
            return Mono.error(new IllegalArgumentException("Tenant ID is required in the request headers."));
        }

        TenantContextHolder.setContext(new TenantContext(Long.parseLong(tenantId)), true);

        return chain.filter(exchange).doFinally(signalType -> TenantContextHolder.removeContext());
    }
}
