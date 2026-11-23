package com.hcltech.sample.reactive.webflux.service.recommended;

import com.hcltech.sample.reactive.webflux.annotation.Recommended;
import com.hcltech.sample.reactive.webflux.ctx.TenantContext;
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

@Service
@RequiredArgsConstructor
@Slf4j
@Recommended
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    //BoundedElasticScheduler in production, check src/main/java/com/hcltech/sample/reactive/webflux/config/SchedulerConfig.java for details
    //feel free to change as per your requirement, however, when blocking code takes significant time to complete prefer using a bounded elastic scheduler.
    private final Scheduler scheduler;

    @Override
    public Flux<UserDto> getAllUsers() {
        return Mono.deferContextual(ctx -> Mono.justOrEmpty(ctx.<TenantContext>getOrEmpty(TenantContext.class))
                        .map(TenantContext::tenantId)
                        .switchIfEmpty(Mono.error(new TenantContextMissingException("Tenant context is missing"))))
                .doOnNext(tenantId -> log.info("Tenant ID from context: {}", tenantId))
                .flatMapMany(tenantId -> Flux.fromIterable(userRepository.findAllByTenantId(tenantId))
                        .map(UserDto::from))
                .subscribeOn(scheduler);
    }

    @Override
    public Mono<UserDto> getUser(long id) {
        //Assumption: findByIdAndTenantId(id, tenantId) is a single indexed primary-key + tenant-id
        //lookup that reliably completes in low single-digit milliseconds (not the "up to 100ms" this
        //comment used to claim - at that duration the overhead argument below no longer holds).
        //
        //There is deliberately no subscribeOn(scheduler) here, unlike getAllUsers() above. That means
        //this blocking call runs on whichever thread subscribes to this Mono - in a real WebFlux app,
        //that is one of the few Netty event-loop threads shared by every concurrent request. Hopping
        //to boundedElastic costs its own handoff/context-switch overhead (microseconds to low
        //milliseconds); for a call this fast, that handoff can cost as much as, or more than, the
        //query itself, so paying it on every request buys nothing but latency.
        //
        //This only stays safe as long as the assumption holds. If this query ever stops being fast -
        //missing/degraded index, lock contention, table growth, N+1 elsewhere in the same request -
        //it blocks an event-loop thread directly, which stalls every other request sharing that
        //thread, not just this one. Don't guess: back this with real load/latency testing, and prefer
        //subscribeOn(scheduler) (as getAllUsers() does for its unbounded-size, can't-assume-it's-fast
        //repository call) the moment that assumption is no longer solid.
        return Mono.deferContextual(ctx -> Mono.justOrEmpty(ctx.<TenantContext>getOrEmpty(TenantContext.class))
                        .map(TenantContext::tenantId)
                        .switchIfEmpty(Mono.error(new TenantContextMissingException("Tenant context is missing"))))
                .doOnNext(tenantId -> log.info("Tenant ID from context: {}", tenantId))
                .map(tenantId -> userRepository.findByIdAndTenantId(id, tenantId))
                .map(UserDto::from);
    }
}
