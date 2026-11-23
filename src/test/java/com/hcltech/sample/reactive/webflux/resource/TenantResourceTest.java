package com.hcltech.sample.reactive.webflux.resource;

import com.hcltech.sample.reactive.webflux.dto.TenantDto;
import com.hcltech.sample.reactive.webflux.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantResourceTest {

    @Mock
    private TenantService tenantService;

    private TenantResource tenantResource;

    @BeforeEach
    void setUp() {
        tenantResource = new TenantResource(tenantService);
    }

    @Test
    void shouldReturnAllTenantsFromService() {
        TenantDto firstTenant = new TenantDto(1L, "acme", "2024-01-01T00:00:00Z");
        TenantDto secondTenant = new TenantDto(2L, "globex", "2024-01-01T00:00:00Z");
        when(tenantService.getAllTenants()).thenReturn(Flux.fromIterable(List.of(firstTenant, secondTenant)));

        StepVerifier.create(tenantResource.getAllTenants())
                .expectNext(firstTenant)
                .expectNext(secondTenant)
                .verifyComplete();

        verify(tenantService).getAllTenants();
    }

    @Test
    void shouldReturnEmptyFluxWhenNoTenantsExist() {
        when(tenantService.getAllTenants()).thenReturn(Flux.empty());

        StepVerifier.create(tenantResource.getAllTenants())
                .verifyComplete();
    }
}
