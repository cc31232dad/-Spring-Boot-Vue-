CREATE TABLE farmer_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    real_name VARCHAR(64) NOT NULL,
    id_card VARCHAR(32) NOT NULL,
    province VARCHAR(64) NOT NULL,
    city VARCHAR(64) NOT NULL,
    district VARCHAR(64) NOT NULL,
    detail_address VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL,
    license_no VARCHAR(128) NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    reject_reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id), UNIQUE KEY uk_farmer_profiles_user (user_id), KEY idx_farmer_profiles_status (status),
    CONSTRAINT fk_farmer_profiles_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE farmer_audits (
    id BIGINT NOT NULL AUTO_INCREMENT,
    farmer_id BIGINT NOT NULL,
    admin_id BIGINT NULL,
    status VARCHAR(16) NOT NULL,
    remark VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id), KEY idx_farmer_audits_farmer (farmer_id), KEY idx_farmer_audits_status (status),
    CONSTRAINT fk_farmer_audits_farmer FOREIGN KEY (farmer_id) REFERENCES farmer_profiles (id),
    CONSTRAINT fk_farmer_audits_admin FOREIGN KEY (admin_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
