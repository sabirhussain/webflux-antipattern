package com.hcltech.sample.reactive.webflux.filter.antipattern;

import com.hcltech.sample.reactive.webflux.annotation.AntiPattern;
import com.hcltech.sample.reactive.webflux.ctx.TenantContext;
import com.hcltech.sample.reactive.webflux.ctx.TenantContextHolder;
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
@AntiPattern
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

        return headerResolver.getValue(exchange, TENANT_ID_HEADER).map(tenantId -> {
            try {
                long tenantIdLong = Long.parseLong(tenantId);
                log.debug("Setting tenant ID {} in the thread local context", tenantIdLong);

                TenantContextHolder.setContext(new TenantContext(tenantIdLong), true);
                return chain.filter(exchange).doFinally(signalType -> TenantContextHolder.removeContext());
            } catch (NumberFormatException ex) {
                log.error("Request aborted: Header {} is not a valid long value.", TENANT_ID_HEADER);
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant ID must be a valid long value."));
            }
        }).orElseGet(() -> {
            log.error("Request aborted: Header {} is missing in the request headers.", TENANT_ID_HEADER);
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant ID is required in the request headers."));
        }).then();
    }
}
