ALTER TABLE users
    ADD COLUMN nickname VARCHAR(64) NULL AFTER username,
    ADD COLUMN real_name VARCHAR(64) NULL AFTER nickname,
    ADD COLUMN email VARCHAR(128) NULL AFTER real_name,
    ADD COLUMN avatar_url VARCHAR(500) NULL AFTER email;

CREATE TABLE user_addresses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    province VARCHAR(50) NOT NULL,
    city VARCHAR(50) NOT NULL,
    district VARCHAR(50) NOT NULL,
    detail VARCHAR(255) NOT NULL,
    is_default TINYINT(1) NOT NULL DEFAULT 0,
    default_user_id BIGINT GENERATED ALWAYS AS (CASE WHEN is_default = 1 THEN user_id ELSE NULL END) STORED,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_addresses_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_user_addresses_default UNIQUE (default_user_id),
    INDEX idx_user_addresses_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_favorites (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_favorites_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_favorites_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT uk_user_favorites_user_product UNIQUE (user_id, product_id),
    INDEX idx_user_favorites_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
