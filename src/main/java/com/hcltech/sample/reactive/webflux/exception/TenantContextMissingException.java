package com.hcltech.sample.reactive.webflux.exception;

public class TenantContextMissingException extends RuntimeException {
    public TenantContextMissingException(String message) {
        super(message);
    }
}
