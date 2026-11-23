package com.hcltech.sample.reactive.webflux.resource;

import com.hcltech.sample.reactive.webflux.dto.TenantDto;
import com.hcltech.sample.reactive.webflux.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
public final class TenantResource {
    private final TenantService tenantService;

    @GetMapping
    public Flux<TenantDto> getAllTenants() {
        return tenantService.getAllTenants();
    }
}
