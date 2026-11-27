package com.hcltech.sample.reactive.webflux.config;

import com.hcltech.sample.reactive.webflux.annotation.ThreadLocalPropagation;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

@Configuration
@ThreadLocalPropagation
class ReactorHooksConfig {

    @PostConstruct
    void enableHooks() {
        Hooks.enableAutomaticContextPropagation();
    }
}
