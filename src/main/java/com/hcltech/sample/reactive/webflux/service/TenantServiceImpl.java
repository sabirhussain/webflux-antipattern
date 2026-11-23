package com.hcltech.sample.reactive.webflux.service;

import com.hcltech.sample.reactive.webflux.dto.TenantDto;
import com.hcltech.sample.reactive.webflux.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
final class TenantServiceImpl implements TenantService {
    private final TenantRepository tenantRepository;

    @Override
    public Flux<TenantDto> getAllTenants() {
        //This is for demo, prefer using reactive repository instead of blocking repository.
        // also prefer pageable queries instead of fetching all records at once.
        return Flux.fromStream(tenantRepository.findAll().stream().map(TenantDto::from));
    }
}
