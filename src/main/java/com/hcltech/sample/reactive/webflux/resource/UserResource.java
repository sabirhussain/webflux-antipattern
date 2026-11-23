package com.hcltech.sample.reactive.webflux.resource;

import com.hcltech.sample.reactive.webflux.dto.UserDto;
import com.hcltech.sample.reactive.webflux.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public final class UserResource {
    private final UserService userService;

    @GetMapping
    public Flux<UserDto> getUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{userId}")
    public Mono<UserDto> getUser(@PathVariable long userId) {
        return userService.getUser(userId);
    }
}
