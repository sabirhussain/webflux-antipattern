package com.hcltech.sample.reactive.webflux.service.recommended;

import com.hcltech.sample.reactive.webflux.annotation.Recommended;
import com.hcltech.sample.reactive.webflux.ctx.TenantContext;
import com.hcltech.sample.reactive.webflux.dto.UserDto;
import com.hcltech.sample.reactive.webflux.exception.TenantContextMissingException;
import com.hcltech.sample.reactive.webflux.model.User;
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
@Recommended
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    //BoundedElasticScheduler in production, check src/main/java/com/hcltech/sample/reactive/webflux/config/SchedulerConfig.java for details
    //feel free to change as per your requirement, however, when blocking code takes significant time to complete prefer using a bounded elastic scheduler.
    private final Scheduler userServiceScheduler;

    @Override
    public Flux<UserDto> getAllUsers() {
        return getTenantIdFromContext()
                .doOnNext(tenantId -> log.debug("Tenant ID from context: {}", tenantId))
                .flatMapMany(tenantId -> Flux.fromIterable(userRepository.findAllByTenantId(tenantId))
                        .map(UserDto::from))
                .subscribeOn(userServiceScheduler);
    }

    @Override
    public Mono<UserDto> getUser(long id) {
        return getTenantIdFromContext()
                .doOnNext(tenantId -> log.debug("Tenant ID from context: {}", tenantId))
                .flatMap(tenantId -> Mono.justOrEmpty(findUser(id, tenantId)))
                .map(UserDto::from).subscribeOn(userServiceScheduler);
    }

    private Mono<Long> getTenantIdFromContext() {
        return Mono.deferContextual(ctx -> Mono.justOrEmpty(ctx.<TenantContext>getOrEmpty(TenantContext.class))
                .map(TenantContext::tenantId)
                .switchIfEmpty(Mono.error(new TenantContextMissingException("Tenant context is missing"))));
    }

    private Optional<User> findUser(long id, long tenantId) {
        return Optional.ofNullable(userRepository.findByIdAndTenantId(id, tenantId));
    }
}
