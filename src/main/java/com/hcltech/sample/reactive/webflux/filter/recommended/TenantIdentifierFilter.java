package com.hcltech.sample.reactive.webflux.filter.recommended;

import com.hcltech.sample.reactive.webflux.annotation.Recommended;
import com.hcltech.sample.reactive.webflux.ctx.TenantContext;
import com.hcltech.sample.reactive.webflux.filter.HeaderResolver;
import com.hcltech.sample.reactive.webflux.filter.policy.TenantIdentifierExclusionPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@Recommended
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class TenantIdentifierFilter implements WebFilter {
    private static final String TENANT_ID_HEADER = "X-Tenant-ID";

    private final TenantIdentifierExclusionPolicy exclusionPolicy;
    private final HeaderResolver headerResolver;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (exclusionPolicy.isPathExcluded(exchange)) {
            return chain.filter(exchange);
        }

        return Mono.justOrEmpty(headerResolver.getValue(exchange, TENANT_ID_HEADER))
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("Request aborted: Header {} is missing in the request headers.", TENANT_ID_HEADER);
                    return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant ID is required in the request headers."));
                }))
                .map(Long::parseLong)
                .onErrorMap(NumberFormatException.class, ex -> {
                    log.error("Request aborted: Header {} is not a valid long value.", TENANT_ID_HEADER);
                    return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant ID must be a valid long value.");
                })
                .flatMap(tenantId -> {
                    log.debug("Setting tenant ID {} in the reactive context", tenantId);
                    return chain.filter(exchange).contextWrite(ctx -> ctx.put(TenantContext.class, new TenantContext(tenantId)));
                });
    }
}
