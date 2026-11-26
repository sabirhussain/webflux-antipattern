package com.hcltech.sample.reactive.webflux.resource;

import com.hcltech.sample.reactive.webflux.dto.UserDto;
import com.hcltech.sample.reactive.webflux.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserResourceTest {

    @Mock
    private UserService userService;

    private UserResource userResource;

    @BeforeEach
    void setUp() {
        userResource = new UserResource(userService);
    }

    @Test
    void shouldReturnAllUsersFromService() {
        UserDto firstUser = new UserDto(1L, 42L, "jdoe", "jdoe@example.com", "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
        UserDto secondUser = new UserDto(2L, 42L, "asmith", "asmith@example.com", "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
        when(userService.getAllUsers()).thenReturn(Flux.fromIterable(List.of(firstUser, secondUser)));

        StepVerifier.create(userResource.getUsers())
                .expectNext(firstUser)
                .expectNext(secondUser)
                .verifyComplete();

        verify(userService).getAllUsers();
    }

    @Test
    void shouldReturnEmptyFluxWhenNoUsersExist() {
        when(userService.getAllUsers()).thenReturn(Flux.empty());

        StepVerifier.create(userResource.getUsers())
                .verifyComplete();
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 42L, 100L})
    void shouldPassPathVariableThroughToServiceForGetUser(long userId) {
        UserDto user = new UserDto(userId, 42L, "jdoe", "jdoe@example.com", "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
        when(userService.getUser(userId)).thenReturn(Mono.just(user));

        StepVerifier.create(userResource.getUser(userId))
                .expectNext(user)
                .verifyComplete();

        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(userService).getUser(userIdCaptor.capture());
        assertEquals(userId, userIdCaptor.getValue());
    }

    @Test
    void shouldReturnErrorWhenUserNotFound() {
        when(userService.getUser(1L)).thenReturn(Mono.empty());

        StepVerifier.create(userResource.getUser(1L))
                .verifyErrorMatches(throwable -> throwable instanceof org.springframework.web.server.ResponseStatusException &&
                        ((org.springframework.web.server.ResponseStatusException) throwable).getStatusCode() == HttpStatus.NOT_FOUND &&
                        "User not found for ID: 1".equals(((org.springframework.web.server.ResponseStatusException) throwable).getReason()));
    }
}
