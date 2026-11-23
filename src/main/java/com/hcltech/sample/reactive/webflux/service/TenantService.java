package com.hcltech.sample.reactive.webflux.service;

import com.hcltech.sample.reactive.webflux.dto.TenantDto;
import reactor.core.publisher.Flux;

public interface TenantService {
    Flux<TenantDto> getAllTenants();
}
