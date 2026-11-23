package com.hcltech.sample.reactive.webflux.service.recommended;

import com.hcltech.sample.reactive.webflux.ctx.TenantContext;
import com.hcltech.sample.reactive.webflux.dto.UserDto;
import com.hcltech.sample.reactive.webflux.exception.TenantContextMissingException;
import com.hcltech.sample.reactive.webflux.model.User;
import com.hcltech.sample.reactive.webflux.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserServiceImpl userService;

    private static User user(long id, long tenantId, String username, String email, Instant createdAt) {
        User user = new User();
        user.setId(id);
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setEmail(email);
        user.setCreatedAt(createdAt);
        user.setUpdatedAt(createdAt);
        return user;
    }

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, Schedulers.boundedElastic());
    }

    @Test
    void shouldReturnErrorWhenTenantContextIsMissingFromReactorContext() {
        StepVerifier.create(userService.getAllUsers())
                .verifyError(TenantContextMissingException.class);
    }

    @Test
    void shouldReturnUsersForTenantIdInReactorContext() {
        Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
        User user = user(1L, 42L, "jdoe", "jdoe@example.com", createdAt);
        when(userRepository.findAllByTenantId(42L)).thenReturn(List.of(user));

        StepVerifier.create(userService.getAllUsers()
                        .contextWrite(Context.of(TenantContext.class, new TenantContext(42L))))
                .expectNext(UserDto.from(user))
                .verifyComplete();

        ArgumentCaptor<Long> tenantIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(userRepository).findAllByTenantId(tenantIdCaptor.capture());
        assertEquals(42L, tenantIdCaptor.getValue());
    }

    @Test
    void shouldReturnErrorWhenTenantContextIsMissingFromReactorContextForGetUser() {
        StepVerifier.create(userService.getUser(1L))
                .verifyError(TenantContextMissingException.class);

        verify(userRepository, never()).findByIdAndTenantId(anyLong(), anyLong());
    }

    @Test
    void shouldReturnUserDtoWhenUserExistsForTenant() {
        Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
        User user = user(1L, 42L, "jdoe", "jdoe@example.com", createdAt);
        when(userRepository.findByIdAndTenantId(1L, 42L)).thenReturn(user);

        StepVerifier.create(userService.getUser(1L)
                        .contextWrite(Context.of(TenantContext.class, new TenantContext(42L))))
                .expectNext(UserDto.from(user))
                .verifyComplete();

        verify(userRepository).findByIdAndTenantId(1L, 42L);
    }

    @Test
    void shouldResolveTenantIdLazilyAtSubscriptionTimeForGetUser() {
        // Contrast with the antipattern implementation: here the tenantId is resolved inside
        // Mono.deferContextual(...), so it is only read once something actually subscribes.
        // Building the Mono before the context even exists is fine - the context only needs to be
        // present by the time it is subscribed to.
        Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
        User user = user(1L, 42L, "jdoe", "jdoe@example.com", createdAt);
        when(userRepository.findByIdAndTenantId(1L, 42L)).thenReturn(user);

        Mono<UserDto> result = userService.getUser(1L);

        StepVerifier.create(result.contextWrite(Context.of(TenantContext.class, new TenantContext(42L))))
                .expectNext(UserDto.from(user))
                .verifyComplete();
    }
}
