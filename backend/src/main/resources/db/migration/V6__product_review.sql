ALTER TABLE product
    ADD COLUMN reviewed_by BIGINT NULL,
    ADD COLUMN reviewed_at DATETIME NULL,
    ADD COLUMN review_reason VARCHAR(500) NULL,
    ADD INDEX idx_product_reviewed_by (reviewed_by),
    ADD CONSTRAINT fk_product_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id);
