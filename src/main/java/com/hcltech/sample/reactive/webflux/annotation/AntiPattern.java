package com.hcltech.sample.reactive.webflux.annotation;

import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Profile("antipattern")
@Order(Ordered.HIGHEST_PRECEDENCE)
public @interface AntiPattern {
}
