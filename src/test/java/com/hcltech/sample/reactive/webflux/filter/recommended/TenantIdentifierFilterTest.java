package com.hcltech.sample.reactive.webflux.filter.recommended;

import com.hcltech.sample.reactive.webflux.ctx.TenantContext;
import com.hcltech.sample.reactive.webflux.ctx.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantIdentifierFilterTest {

    private static final String TENANT_ID_HEADER = "X-Tenant-ID";

    private final TenantIdentifierFilter filter = new TenantIdentifierFilter();

    @Mock
    private WebFilterChain chain;

    @AfterEach
    void clearContext() {
        TenantContextHolder.removeContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/swagger-ui.html", "/swagger-ui/index.html", "/v3/api-docs", "/v3/api-docs/swagger-config", "/tenants"})
    void shouldBypassTenantCheckForExcludedPaths(String path) {
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        ArgumentCaptor<ServerWebExchange> exchangeCaptor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(exchangeCaptor.capture());
        assertSame(exchange, exchangeCaptor.getValue());
    }

    @Test
    void shouldReturnErrorWhenTenantIdHeaderIsMissing() {
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/orders").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyError(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void shouldReturnErrorWhenTenantIdHeaderIsBlank(String tenantId) {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/orders").header(TENANT_ID_HEADER, tenantId));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyError(IllegalArgumentException.class);
    }

    @Test
    void shouldReturnErrorWhenTenantIdHeaderIsNotNumeric() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/orders").header(TENANT_ID_HEADER, "not-a-number"));
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyError(NumberFormatException.class);
    }

    @Test
    void shouldStoreTenantId() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/orders").header(TENANT_ID_HEADER, "42"));
        AtomicReference<TenantContext> capturedContext = new AtomicReference<>();
        when(chain.filter(any(ServerWebExchange.class)))
                .thenReturn(Mono.deferContextual(ctx -> {
                    capturedContext.set(ctx.get(TenantContext.class));
                    return Mono.empty();
                }));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        ArgumentCaptor<ServerWebExchange> exchangeCaptor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(exchangeCaptor.capture());
        assertSame(exchange, exchangeCaptor.getValue());
        assertEquals(new TenantContext(42L), capturedContext.get());
    }

    @Test
    void shouldIsolateTenantIdPerSubscription() {
        ServerWebExchange firstExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/orders").header(TENANT_ID_HEADER, "1"));
        ServerWebExchange secondExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/orders").header(TENANT_ID_HEADER, "2"));
        AtomicReference<TenantContext> firstCapturedContext = new AtomicReference<>();
        AtomicReference<TenantContext> secondCapturedContext = new AtomicReference<>();
        when(chain.filter(firstExchange))
                .thenReturn(Mono.deferContextual(ctx -> {
                    firstCapturedContext.set(ctx.get(TenantContext.class));
                    return Mono.empty();
                }));
        when(chain.filter(secondExchange))
                .thenReturn(Mono.deferContextual(ctx -> {
                    secondCapturedContext.set(ctx.get(TenantContext.class));
                    return Mono.empty();
                }));

        StepVerifier.create(filter.filter(firstExchange, chain)).verifyComplete();
        StepVerifier.create(filter.filter(secondExchange, chain)).verifyComplete();

        assertEquals(new TenantContext(1L), firstCapturedContext.get());
        assertEquals(new TenantContext(2L), secondCapturedContext.get());
    }

    @Test
    void shouldNotPopulateThreadLocalTenantContext() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/orders").header(TENANT_ID_HEADER, "42"));
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertNull(TenantContextHolder.getContext());
    }
}
