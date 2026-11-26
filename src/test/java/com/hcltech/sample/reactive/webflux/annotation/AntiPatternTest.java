package com.hcltech.sample.reactive.webflux.annotation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.AnnotatedElementUtils;

class AntiPatternTest {
    @Test
    void shouldExposesExpectedSpringMetadata() {
        Profile profile = AnnotatedElementUtils.findMergedAnnotation(Sample.class, Profile.class);

        Assertions.assertNotNull(profile);
        Assertions.assertEquals("antipattern", profile.value()[0]);
    }

    @AntiPattern
    static class Sample {

    }
}
