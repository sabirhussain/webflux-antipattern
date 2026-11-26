package com.hcltech.sample.reactive.webflux.service.recommended;

import com.hcltech.sample.reactive.webflux.annotation.Recommended;
import com.hcltech.sample.reactive.webflux.dto.TenantDto;
import com.hcltech.sample.reactive.webflux.repository.TenantRepository;
import com.hcltech.sample.reactive.webflux.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@Service
@RequiredArgsConstructor
@Recommended
public class TenantServiceImpl implements TenantService {
    private final TenantRepository tenantRepository;
    private final Scheduler tenantServiceScheduler;

    @Override
    public Flux<TenantDto> getAllTenants() {
        //This is for demo, prefer using reactive repository instead of blocking repository.
        // also prefer pageable queries instead of fetching all records at once.
        return Mono.fromCallable(tenantRepository::findAll)
                .subscribeOn(tenantServiceScheduler)
                .flatMapMany(Flux::fromIterable)
                .map(TenantDto::from);
    }
}
