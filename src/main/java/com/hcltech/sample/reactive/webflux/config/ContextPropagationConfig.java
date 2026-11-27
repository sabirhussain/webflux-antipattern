package com.hcltech.sample.reactive.webflux.config;

import com.hcltech.sample.reactive.webflux.annotation.ThreadLocalPropagation;
import com.hcltech.sample.reactive.webflux.ctx.TenantContextHolder;
import io.micrometer.context.ContextRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
@ThreadLocalPropagation
class ContextPropagationConfig {

    @PostConstruct
    void registerThreadLocalAccessors() {
        ContextRegistry.getInstance()
                .registerThreadLocalAccessor("TenantContext", TenantContextHolder::getContext,
                        TenantContextHolder::setContext, TenantContextHolder::removeContext);
    }
}
