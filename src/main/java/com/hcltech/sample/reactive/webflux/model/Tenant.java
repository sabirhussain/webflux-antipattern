package com.hcltech.sample.reactive.webflux.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * <strong>Note: </strong> intention here is not to teach you best practices of JPA,
 * but to demonstrate the patterns and antipatterns of reactive programming.
 */

@Entity
@Table(name = "tenants")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @EqualsAndHashCode.Include
    @Column(unique = true)
    private String name;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
