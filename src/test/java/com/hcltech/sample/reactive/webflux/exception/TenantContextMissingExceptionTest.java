package com.hcltech.sample.reactive.webflux.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TenantContextMissingExceptionTest {

    @Test
    void shouldPreserveProvidedMessage() {
        String message = "Tenant context is missing";

        TenantContextMissingException exception =
                new TenantContextMissingException(message);

        assertEquals(message, exception.getMessage());
    }
}
