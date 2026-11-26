package com.hcltech.sample.reactive.webflux.resource;

import com.hcltech.sample.reactive.webflux.dto.UserDto;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/*
 * Integration-style test: boots the full application context (real H2 database seeded from
 * data.sql, the real TenantIdentifierFilter, the real UserServiceImpl, the real UserRepository)
 * and drives it over actual HTTP via WebTestClient, instead of mocking any collaborator.
 *
 * Its purpose is to prove functional parity between the two profiles: for an ordinary request,
 * both the "antipattern" and "recommended" wiring must serve the same, correct data for the
 * tenant named in the X-Tenant-ID header. Each profile needs its own ApplicationContext (only one
 * UserService bean is ever active at a time, selected by @Profile), so the two scenarios are
 * expressed as separate @Nested classes, each with its own @ActiveProfiles.
 *
 * This intentionally does not attempt to reproduce the thread-reuse/stale-context bug over real
 * HTTP - these tests only ever send a single tenant per request path, and never vary the tenant
 * across back-to-back calls that would need to land on the same reused worker thread to observe
 * staleness. That bug is instead covered deterministically at the unit level in
 * service/antipattern/UserServiceImplTest, where the Scheduler is injected and fully controlled.
 * See AntipatternProfile.DeterministicSchedulerTestConfig below for why the antipattern profile
 * still needs its own deterministic Scheduler even for this simpler, single-tenant-per-call test.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
class UserResource_IT {

    private static void assertTenantOneUsersReturned(WebTestClient webTestClient) {
        List<UserDto> users = webTestClient.get().uri("/users")
                .header("X-Tenant-ID", "1")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(UserDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(users)
                .isNotNull()
                .extracting(UserDto::username)
                .containsExactlyInAnyOrder("alice1", "bob1");
        assertThat(users).allMatch(user -> user.tenantId() == 1L);
    }

    private static void assertSingleTenantOneUserReturned(WebTestClient webTestClient) {
        webTestClient.get().uri("/users/1")
                .header("X-Tenant-ID", "1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserDto.class)
                .value(user -> {
                    assertThat(user.username()).isEqualTo("alice1");
                    assertThat(user.tenantId()).isEqualTo(1L);
                });
    }

    private static void assertBadRequestForMissingTenantHeader(WebTestClient webTestClient, String uri) {
        webTestClient.get().uri(uri)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Bad Request");
    }

    private static void assertBadRequestForInvalidTenantHeader(WebTestClient webTestClient, String uri) {
        webTestClient.get().uri(uri)
                .header("X-Tenant-ID", "invalid")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Bad Request");
    }

    private static void assertNotFoundForNonexistentUser(WebTestClient webTestClient) {
        webTestClient.get().uri("/users/1")
                .header("X-Tenant-ID", "10000000")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Not Found");
    }

    @Nested
    @ActiveProfiles("antipattern")
    class AntipatternProfile {

        @Autowired
        private WebTestClient webTestClient;

        @Test
        void shouldReturnUsersForTenantFromHeader() {
            assertTenantOneUsersReturned(webTestClient);
        }

        @Test
        void shouldReturnSingleUserByIdForTenantFromHeader() {
            assertSingleTenantOneUserReturned(webTestClient);
        }

        @ParameterizedTest
        @ValueSource(strings = {"/users", "/users/1"})
        void shouldReturnBadRequestForMissingTenantHeader(String uri) {
            assertBadRequestForMissingTenantHeader(webTestClient, uri);
        }

        @ParameterizedTest
        @ValueSource(strings = {"/users", "/users/1"})
        void shouldReturnBadRequestForInvalidTenantHeader(String uri) {
            assertBadRequestForInvalidTenantHeader(webTestClient, uri);
        }

        @Test
        void shouldReturnNotFoundForNonexistentUser() {
            assertNotFoundForNonexistentUser(webTestClient);
        }

        /*
         * Exception to "prefer the real production wiring in an integration test":
         * the antipattern UserServiceImpl reads tenant information from an InheritableThreadLocal,
         * so behaviour depends on the worker thread that executes the service logic (see service/antipattern/UserServiceImplTest).
         *
         * Production uses a pooled bounded-elastic Scheduler whose threads are reused.
         * Replacing it with a dedicated single-thread Scheduler makes execution deterministic
         * and removes thread-selection variability from these tests.
         *
         * RecommendedProfile needs no such override because it resolves the tenant
         * from Reactor Context rather than thread-local state.
         */
        @TestConfiguration
        static class DeterministicSchedulerTestConfig {

            @Bean(destroyMethod = "shutdownNow")
            ExecutorService pooledWorkerExecutor() {
                return Executors.newSingleThreadExecutor();
            }

            @Bean
            @Primary
            Scheduler deterministicScheduler(ExecutorService pooledWorkerExecutor) {
                return Schedulers.fromExecutorService(pooledWorkerExecutor);
            }
        }
    }

    @Nested
    @ActiveProfiles("recommended")
    class RecommendedProfile {

        @Autowired
        private WebTestClient webTestClient;

        @Test
        void shouldReturnUsersForTenantFromHeader() {
            assertTenantOneUsersReturned(webTestClient);
        }

        @Test
        void shouldReturnSingleUserByIdForTenantFromHeader() {
            assertSingleTenantOneUserReturned(webTestClient);
        }

        @ParameterizedTest
        @ValueSource(strings = {"/users", "/users/1"})
        void shouldReturnBadRequestForMissingTenantHeader(String uri) {
            assertBadRequestForMissingTenantHeader(webTestClient, uri);
        }

        @ParameterizedTest
        @ValueSource(strings = {"/users", "/users/1"})
        void shouldReturnBadRequestForInvalidTenantHeader(String uri) {
            assertBadRequestForInvalidTenantHeader(webTestClient, uri);
        }

        @Test
        void shouldReturnNotFoundForNonexistentUser() {
            assertNotFoundForNonexistentUser(webTestClient);
        }
    }
}
