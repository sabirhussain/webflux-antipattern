package com.hcltech.sample.reactive.webflux.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

/**
 * <strong>Note: </strong> intention here is not to teach you best practices of JPA,
 * also no way we are suggesting to use multi-tenancy in this way,
 * but to demonstrate the patterns and antipatterns of reactive programming.
 */

@Entity
@Table(name = "users")
@Data
public final class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "tenant_id", nullable = false)
    private long tenantId;

    @Column(unique = true)
    private String username;

    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
