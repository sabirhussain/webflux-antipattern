package com.hcltech.sample.reactive.webflux.service.antipattern;

import com.hcltech.sample.reactive.webflux.annotation.AntiPattern;
import com.hcltech.sample.reactive.webflux.ctx.TenantContext;
import com.hcltech.sample.reactive.webflux.ctx.TenantContextHolder;
import com.hcltech.sample.reactive.webflux.dto.UserDto;
import com.hcltech.sample.reactive.webflux.exception.TenantContextMissingException;
import com.hcltech.sample.reactive.webflux.repository.UserRepository;
import com.hcltech.sample.reactive.webflux.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@AntiPattern
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final Scheduler scheduler;

    @Override
    public Flux<UserDto> getAllUsers() {
        return Mono.defer(() -> Mono.justOrEmpty(TenantContextHolder.getContext()))
                .switchIfEmpty(Mono.error(new TenantContextMissingException("Tenant context is missing")))
                .map(TenantContext::tenantId).doOnNext(tenantId -> log.info("Tenant ID from context: {}", tenantId))
                .flatMapMany(tenantId -> Flux.fromIterable(userRepository.findAllByTenantId(tenantId)).map(UserDto::from))
                .subscribeOn(scheduler);

        /*
        Context retrieval happen when getAllUsers is called, not when the publisher is subscribed to.
        return Mono.justOrEmpty(TenantContextHolder.getContext())
                .switchIfEmpty(Mono.error(new IllegalStateException("Tenant context is not set")))
                .map(TenantContext::tenantId)
                .flatMapMany(tenantId -> Flux.fromStream(userRepository.findAllByTenantId(tenantId).map(UserDto::from)));

         */
    }

    @Override
    public Mono<UserDto> getUser(long id) {
        // Antipattern: eager vs lazy. TenantContextHolder.getContext() is read right here,
        // synchronously, the moment getUser(id) is called - before any Mono is even built, let
        // alone subscribed to. A Mono/Flux only actually does anything once something subscribes
        // to it; by resolving the tenant outside of that reactive chain, this method bakes in
        // whatever tenant happens to be active at call time. If the caller doesn't subscribe
        // immediately (composes this Mono further, retries it, delays it, caches it, or hands it
        // off across a thread/time boundary) and the context changes - or is cleared - in the
        // meantime, that no longer matters: the stale, already-captured tenantId is still what
        // gets used, instead of whatever tenant is actually in effect at subscription time.
        long tenantId = Optional.ofNullable(TenantContextHolder.getContext()).map(TenantContext::tenantId)
                .orElseThrow(() -> new TenantContextMissingException("Tenant context is missing"));
        return Optional.ofNullable(userRepository.findByIdAndTenantId(id, tenantId)).map(UserDto::from).map(Mono::just).orElseGet(Mono::empty);
    }
}
