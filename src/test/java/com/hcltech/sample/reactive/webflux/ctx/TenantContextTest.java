package com.hcltech.sample.reactive.webflux.ctx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TenantContextTest {

    @Test
    void shouldStoreTenantId() {
        TenantContext context = new TenantContext(42L);

        assertEquals(42L, context.tenantId());
    }
}
