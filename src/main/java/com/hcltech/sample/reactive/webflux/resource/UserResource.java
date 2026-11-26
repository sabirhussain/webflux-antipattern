package com.hcltech.sample.reactive.webflux.resource;

import com.hcltech.sample.reactive.webflux.dto.UserDto;
import com.hcltech.sample.reactive.webflux.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public final class UserResource {
    private final UserService userService;

    @Operation(parameters = {@Parameter(ref = "#/components/parameters/TenantHeader")})
    @GetMapping
    public Flux<UserDto> getUsers() {
        return userService.getAllUsers();
    }

    @Operation(parameters = {@Parameter(ref = "#/components/parameters/TenantHeader")})
    @GetMapping("/{userId}")
    public Mono<UserDto> getUser(@PathVariable long userId) {
        return userService.getUser(userId).switchIfEmpty(Mono.error(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found for ID: " + userId)));

        /**
         * Here Mono.error(throwable) builds the exception on every call, even if the user is found.
         * This can be avoided by Mono.error(supplier) as shown above, which builds the exception only when the user is not found.
         */
        //return userService.getUser(userId).switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found for ID: " + userId)));
    }
}
