package com.hcltech.sample.reactive.webflux.service.recommended;

import com.hcltech.sample.reactive.webflux.dto.TenantDto;
import com.hcltech.sample.reactive.webflux.model.Tenant;
import com.hcltech.sample.reactive.webflux.repository.TenantRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceImplTest {
    @Mock
    private TenantRepository tenantRepository;

    private TenantServiceImpl tenantService;

    private static Tenant tenant(long id, String name, Instant createdAt) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setName(name);
        tenant.setCreatedAt(createdAt);
        return tenant;
    }

    @BeforeEach
    void setUp() {
        tenantService = new TenantServiceImpl(tenantRepository, Schedulers.newSingle("test-worker"));
    }

    @Test
    void shouldReturnAllTenantsMappedToDto() {
        Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
        Tenant firstTenant = tenant(1L, "acme", createdAt);
        Tenant secondTenant = tenant(2L, "globex", createdAt);
        when(tenantRepository.findAll()).thenReturn(List.of(firstTenant, secondTenant));

        StepVerifier.create(tenantService.getAllTenants())
                .expectNext(new TenantDto(1L, "acme", createdAt.toString()))
                .expectNext(new TenantDto(2L, "globex", createdAt.toString()))
                .verifyComplete();

        verify(tenantRepository).findAll();
    }

    @Test
    void shouldReturnEmptyFluxWhenNoTenantsExist() {
        when(tenantRepository.findAll()).thenReturn(List.of());

        StepVerifier.create(tenantService.getAllTenants())
                .verifyComplete();
    }

    @Test
    void shouldNotCallRepositoryIfNotSubscribed() {
        Flux<TenantDto> flux = tenantService.getAllTenants();

        Assertions.assertNotNull(flux);
        //No subscription has been made yet, repository should not have been called.
        verify(tenantRepository, never()).findAll();
    }

    @Test
    void shouldCallRepositoryWhenSubscribed() {
        when(tenantRepository.findAll()).thenReturn(List.of());

        Flux<TenantDto> flux = tenantService.getAllTenants();

        StepVerifier.create(flux)
                .verifyComplete();
        //Subscription has been made, repository should have been called.
        verify(tenantRepository).findAll();
    }

    @Test
    void shouldExecuteRepositoryOnConfiguredScheduler() {
        AtomicReference<String> threadName = new AtomicReference<>();
        when(tenantRepository.findAll()).thenAnswer(invocation -> {
            threadName.set(Thread.currentThread().getName());
            return List.of();
        });

        StepVerifier.create(tenantService.getAllTenants())
                .verifyComplete();

        verify(tenantRepository).findAll();
        Assertions.assertTrue(threadName.get().startsWith("test-worker"));
    }

    @Test
    void shouldNotFailSecondSubscriptionOnGetAllTenants() {
        when(tenantRepository.findAll()).thenReturn(List.of());

        Flux<TenantDto> flux = tenantService.getAllTenants();

        StepVerifier.create(flux).verifyComplete();

        StepVerifier.create(flux).verifyComplete();
    }
}
