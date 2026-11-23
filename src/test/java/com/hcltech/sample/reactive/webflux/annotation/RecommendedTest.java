package com.hcltech.sample.reactive.webflux.annotation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;

class RecommendedTest {
    @Test
    void shouldExposesExpectedSpringMetadata() {
        Profile profile = AnnotatedElementUtils.findMergedAnnotation(Sample.class, Profile.class);
        Order order = AnnotatedElementUtils.findMergedAnnotation(Sample.class, Order.class);

        Assertions.assertNotNull(profile);
        Assertions.assertEquals("recommended", profile.value()[0]);
        Assertions.assertNotNull(order);
        Assertions.assertEquals(Integer.valueOf(Ordered.HIGHEST_PRECEDENCE), order.value());
    }

    @Recommended
    static class Sample {

    }
}
