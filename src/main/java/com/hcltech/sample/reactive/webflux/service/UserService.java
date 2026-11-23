package com.hcltech.sample.reactive.webflux.service;

import com.hcltech.sample.reactive.webflux.dto.UserDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserService {
    Flux<UserDto> getAllUsers();

    Mono<UserDto> getUser(long id);
}
