package com.hcltech.sample.reactive.webflux.dto;

import com.hcltech.sample.reactive.webflux.model.User;

public record UserDto(long id, long tenantId, String username, String email, String createdAt, String updatedAt) {
    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getTenantId(), user.getUsername(), user.getEmail(),
                user.getCreatedAt().toString(), user.getUpdatedAt().toString());
    }
}
