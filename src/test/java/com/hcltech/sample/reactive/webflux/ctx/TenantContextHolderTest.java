package com.hcltech.sample.reactive.webflux.ctx;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TenantContextHolderTest {

    @AfterEach
    void clearContext() {
        TenantContextHolder.removeContext();
    }

    @Test
    void shouldReturnsNullWhenNoContextIsSet() {
        assertNull(TenantContextHolder.getContext());
    }

    @Test
    void shouldStoresAndReturnsContext() {
        TenantContext context = new TenantContext(42L);

        TenantContextHolder.setContext(context);

        assertEquals(context, TenantContextHolder.getContext());
    }

    @Test
    void shouldRemoveExistingContext() {
        TenantContextHolder.setContext(new TenantContext(42L));

        TenantContextHolder.removeContext();

        assertNull(TenantContextHolder.getContext());
    }

    @Test
    void shouldRemoveContextWhenNullIsSet() {
        TenantContextHolder.setContext(new TenantContext(42L));

        TenantContextHolder.setContext(null);

        assertNull(TenantContextHolder.getContext());
    }

    @Test
    void shouldInheritContextToChildThread() throws InterruptedException {
        TenantContext context = new TenantContext(42L);
        TenantContextHolder.setContext(context, true);
        AtomicReference<TenantContext> childContext = new AtomicReference<>();

        Thread child = new Thread(() -> childContext.set(TenantContextHolder.getContext()));
        child.start();
        child.join();

        assertEquals(context, childContext.get());
    }

    @Test
    void shouldNotInheritContextToChildThread() throws InterruptedException {
        TenantContext context = new TenantContext(42L);
        TenantContextHolder.setContext(context, false);
        AtomicReference<TenantContext> childContext = new AtomicReference<>();

        Thread child = new Thread(() -> childContext.set(TenantContextHolder.getContext()));
        child.start();
        child.join();

        assertNull(childContext.get());
    }

    @Test
    void shouldClearInheritableContextWhenRegularContextIsSet() {
        TenantContextHolder.setContext(new TenantContext(1L), true);
        TenantContext regularContext = new TenantContext(2L);

        TenantContextHolder.setContext(regularContext);

        assertEquals(regularContext, TenantContextHolder.getContext());
    }

    @Test
    void shouldClearRegularContextWhenInheritableContextIsSet() {
        TenantContextHolder.setContext(new TenantContext(1L));
        TenantContext inheritableContext = new TenantContext(2L);

        TenantContextHolder.setContext(inheritableContext, true);

        assertEquals(inheritableContext, TenantContextHolder.getContext());
    }
}
