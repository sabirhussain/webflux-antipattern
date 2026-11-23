package com.hcltech.sample.reactive.webflux.service.antipattern;

import com.hcltech.sample.reactive.webflux.ctx.TenantContext;
import com.hcltech.sample.reactive.webflux.ctx.TenantContextHolder;
import com.hcltech.sample.reactive.webflux.dto.UserDto;
import com.hcltech.sample.reactive.webflux.exception.TenantContextMissingException;
import com.hcltech.sample.reactive.webflux.model.User;
import com.hcltech.sample.reactive.webflux.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    /*
     * A dedicated, test-owned single-thread executor standing in for the pooled
     * Schedulers.boundedElastic() used in production. Its one worker thread is created lazily on
     * first use and then reused for every later task, exactly like a pooled scheduler reuses idle
     * workers across unrelated requests - giving these tests full, deterministic control over when
     * that thread is created and when it is reused, instead of depending on the JVM-wide
     * Schedulers.boundedElastic() singleton shared with unrelated tests.
     */
    private ExecutorService pooledWorkerExecutor;
    private Scheduler scheduler;
    private UserServiceImpl userService;

    private static User user(long id, long tenantId, String username, String email) {
        User user = new User();
        user.setId(id);
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setEmail(email);
        Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
        user.setCreatedAt(createdAt);
        user.setUpdatedAt(createdAt);
        return user;
    }

    @BeforeEach
    void setUp() {
        // Schedulers.newSingle() won't do here: it starts its worker thread immediately on
        // creation, before any test has set a TenantContext, so that thread would always inherit
        // "no context" and every test would see it as pre-poisoned rather than control exactly
        // when the worker is born. Executors.newSingleThreadExecutor() only creates its thread
        // lazily, on the first submitted task, so wrapping it with Schedulers.fromExecutorService()
        // lets a test decide precisely which TenantContext is active at that creation moment, then
        // reuse the same executor/thread for a later call to prove what sticks around afterwards.
        pooledWorkerExecutor = Executors.newSingleThreadExecutor();
        scheduler = Schedulers.fromExecutorService(pooledWorkerExecutor);
        userService = new UserServiceImpl(userRepository, scheduler);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.removeContext();
        scheduler.dispose();
        pooledWorkerExecutor.shutdownNow();
    }

    @Test
    void shouldReturnErrorWhenNoTenantContextIsSet() {
        StepVerifier.create(userService.getAllUsers())
                .verifyError(TenantContextMissingException.class);

        verify(userRepository, never()).findAllByTenantId(anyLong());
    }

    @Test
    void shouldReturnErrorEvenWhenTenantContextIsSetOnCallingThread() {
        // Demonstrates the antipattern: subscribeOn() moves execution to a different (pooled)
        // thread before the ThreadLocal context is read, so a regular (non-inheritable) context
        // set on the calling thread is invisible to the service and is reported as missing.
        TenantContextHolder.setContext(new TenantContext(42L));

        StepVerifier.create(userService.getAllUsers())
                .verifyError(TenantContextMissingException.class);

        verify(userRepository, never()).findAllByTenantId(anyLong());
    }

    @Test
    void shouldReturnStaleTenantIdWhenPooledWorkerThreadIsReusedAcrossRequests() {
        // The filter always sets an inheritable context, and this service always subscribes on a
        // pooled scheduler. InheritableThreadLocal copies a value onto a worker thread only once,
        // at the moment that thread is created. A pooled worker that inherited tenant 1 for the
        // first request keeps that value forever, even once the request thread moves on to tenant
        // 2, because the pool reuses the same worker instead of creating a fresh one per request.
        User userForTenantOne = user(1L, 1L, "tenant-one-user", "one@example.com");
        when(userRepository.findAllByTenantId(1L)).thenReturn(List.of(userForTenantOne));

        TenantContextHolder.setContext(new TenantContext(1L), true);
        StepVerifier.create(userService.getAllUsers())
                .expectNext(UserDto.from(userForTenantOne))
                .verifyComplete();
        TenantContextHolder.removeContext();

        TenantContextHolder.setContext(new TenantContext(2L), true);
        StepVerifier.create(userService.getAllUsers())
                .expectNext(UserDto.from(userForTenantOne))
                .verifyComplete();

        ArgumentCaptor<Long> tenantIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(userRepository, times(2)).findAllByTenantId(tenantIdCaptor.capture());
        assertEquals(List.of(1L, 1L), tenantIdCaptor.getAllValues());
        verify(userRepository, never()).findAllByTenantId(2L);
    }

    @Test
    void shouldThrowExceptionWhenNoTenantContextIsSetForGetUser() {
        assertThrows(TenantContextMissingException.class, () -> userService.getUser(1L));

        verify(userRepository, never()).findByIdAndTenantId(anyLong(), anyLong());
    }

    @Test
    void shouldReturnUserDtoWhenUserExistsForTenant() {
        User userForTenantOne = user(1L, 1L, "tenant-one-user", "one@example.com");
        when(userRepository.findByIdAndTenantId(1L, 1L)).thenReturn(userForTenantOne);
        TenantContextHolder.setContext(new TenantContext(1L));

        StepVerifier.create(userService.getUser(1L))
                .expectNext(UserDto.from(userForTenantOne))
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenUserDoesNotExistForTenant() {
        when(userRepository.findByIdAndTenantId(1L, 1L)).thenReturn(null);
        TenantContextHolder.setContext(new TenantContext(1L));

        StepVerifier.create(userService.getUser(1L))
                .verifyComplete();
    }

    @Test
    void shouldResolveTenantIdEagerlyAtCallTimeNotAtSubscriptionTime() {
        // Demonstrates the eager-vs-lazy antipattern called out on getUser(): the tenantId is
        // resolved synchronously the moment getUser(id) is invoked, not deferred until the
        // returned Mono is subscribed. So changing the context *after* the call but *before*
        // subscribing makes no difference - the Mono was already built using the tenant that was
        // active at call time.
        User userForTenantOne = user(1L, 1L, "tenant-one-user", "one@example.com");
        when(userRepository.findByIdAndTenantId(1L, 1L)).thenReturn(userForTenantOne);

        TenantContextHolder.setContext(new TenantContext(1L));
        Mono<UserDto> result = userService.getUser(1L);
        TenantContextHolder.setContext(new TenantContext(2L));

        StepVerifier.create(result)
                .expectNext(UserDto.from(userForTenantOne))
                .verifyComplete();

        verify(userRepository).findByIdAndTenantId(1L, 1L);
        verify(userRepository, never()).findByIdAndTenantId(1L, 2L);
    }
}
