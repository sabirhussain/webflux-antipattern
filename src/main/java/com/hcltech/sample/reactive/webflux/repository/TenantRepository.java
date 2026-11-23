package com.hcltech.sample.reactive.webflux.repository;

import com.hcltech.sample.reactive.webflux.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

}
