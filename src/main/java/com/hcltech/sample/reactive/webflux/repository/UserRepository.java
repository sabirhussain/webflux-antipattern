package com.hcltech.sample.reactive.webflux.repository;

import com.hcltech.sample.reactive.webflux.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * This non-reactive repository is used to demonstrate the antipattern of blocking calls in a reactive service.
 * In a real-world application, you would typically use a reactive repository (e.g., R2DBC) to avoid blocking operations
 * and maintain the reactive nature of the application.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findAllByTenantId(long tenantId);

    User findByIdAndTenantId(long id, long tenantId);
}
