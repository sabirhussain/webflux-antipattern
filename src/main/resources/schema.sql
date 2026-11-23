CREATE TABLE IF NOT EXISTS tenants (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255) UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id  BIGINT NOT NULL,
    username   VARCHAR(255) UNIQUE,
    email      VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);
