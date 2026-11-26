package com.hcltech.sample.reactive.webflux.annotation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.AnnotatedElementUtils;

class RecommendedTest {
    @Test
    void shouldExposesExpectedSpringMetadata() {
        Profile profile = AnnotatedElementUtils.findMergedAnnotation(Sample.class, Profile.class);

        Assertions.assertNotNull(profile);
        Assertions.assertEquals("recommended", profile.value()[0]);
    }

    @Recommended
    static class Sample {

    }
}
