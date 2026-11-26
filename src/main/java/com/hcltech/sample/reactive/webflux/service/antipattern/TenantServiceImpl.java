package com.hcltech.sample.reactive.webflux.service.antipattern;

import com.hcltech.sample.reactive.webflux.annotation.AntiPattern;
import com.hcltech.sample.reactive.webflux.dto.TenantDto;
import com.hcltech.sample.reactive.webflux.repository.TenantRepository;
import com.hcltech.sample.reactive.webflux.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
@AntiPattern
public final class TenantServiceImpl implements TenantService {
    private final TenantRepository tenantRepository;

    @Override
    public Flux<TenantDto> getAllTenants() {
        /**
         * Why is this an antipattern?
         *
         * - It calls findAll() when the method is called, not when someone subscribes.
         * - A second subscription throws IllegalStateException: stream has already been operated upon or closed.
         * - It blocks the event loop.
         */
        return Flux.fromStream(tenantRepository.findAll().stream().map(TenantDto::from));
    }
}
