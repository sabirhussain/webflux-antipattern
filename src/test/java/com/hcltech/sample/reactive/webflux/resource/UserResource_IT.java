package com.hcltech.sample.reactive.webflux.resource;

import com.hcltech.sample.reactive.webflux.dto.UserDto;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

        /*
         * Exception to "prefer the real production wiring in an integration test": this profile's
         * UserServiceImpl is the one implementation whose correctness depends on which physical
         * thread ends up running TenantContextHolder.getContext() - it reads an
         * InheritableThreadLocal and then hops onto Schedulers.boundedElastic(), a pooled scheduler
         * that reuses worker threads across unrelated requests (see
         * service/antipattern/UserServiceImplTest for the full explanation and a deterministic
         * reproduction of the resulting stale-tenant bug).
         *
         * Without this override, this profile's production SchedulerConfig bean supplies the real,
         * process-wide Schedulers.boundedElastic() singleton - the exact same singleton instance
         * that every other Spring context in this JVM (including RecommendedProfile's) and every
         * other test in the whole module may have already used. Which worker thread a request lands
         * on, and what that worker happens to have inherited, would then depend on prior, unrelated
         * activity on that shared singleton - something this test has no control over and cannot
         * see. These tests only ever send tenant "1", so that happens not to surface as a visible
         * failure today, but the underlying nondeterminism would still be there, waiting to make
         * this suite flaky the moment someone varies the tenant across requests.
         *
         * Swapping in a dedicated, test-owned single-thread Scheduler removes that dependency: this
         * context gets its own worker thread, created only once something in this test actually
         * subscribes, so its behaviour depends solely on what this test does - not on global JVM
         * state left behind by other tests. RecommendedProfile needs no such override because its
         * UserServiceImpl resolves the tenant via Reactor Context, which travels with the
         * subscription regardless of which thread executes it.
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
    }
}
