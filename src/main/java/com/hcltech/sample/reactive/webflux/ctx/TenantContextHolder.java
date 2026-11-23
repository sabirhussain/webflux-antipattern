package com.hcltech.sample.reactive.webflux.ctx;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TenantContextHolder {
    private static final ThreadLocal<TenantContext> CONTEXT = new ThreadLocal<>();

    private static final ThreadLocal<TenantContext> INHERITABLE_CONTEXT = new InheritableThreadLocal<>();

    public static void removeContext() {
        CONTEXT.remove();
        INHERITABLE_CONTEXT.remove();
    }

    public static void setContext(TenantContext context, boolean inheritable) {
        if (context == null) {
            removeContext();
            return;
        }

        if (!inheritable) {
            CONTEXT.set(context);
            INHERITABLE_CONTEXT.remove();
        } else {
            INHERITABLE_CONTEXT.set(context);
            CONTEXT.remove();
        }
    }

    public static TenantContext getContext() {
        TenantContext context = CONTEXT.get();
        if (context == null) {
            context = INHERITABLE_CONTEXT.get();
        }
        return context;
    }

    public static void setContext(TenantContext context) {
        setContext(context, false);
    }
}
