package com.hcltech.sample.reactive.webflux.dto;

import com.hcltech.sample.reactive.webflux.model.Tenant;

public record TenantDto(long id, String name, String createdAt) {
    public static TenantDto from(Tenant tenant) {
        return new TenantDto(tenant.getId(), tenant.getName(), tenant.getCreatedAt().toString());
    }
}
